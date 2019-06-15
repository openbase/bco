package org.openbase.bco.dal.test.action;

/*-
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.dal.remote.action.Actions;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.dal.test.layer.unit.location.AbstractBCOLocationManagerTest;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.type.domotic.action.ActionDescriptionType;
import org.openbase.type.domotic.action.ActionParameterType;
import org.openbase.type.domotic.action.ActionReferenceType;
import org.openbase.type.domotic.authentication.AuthTokenType;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import org.openbase.type.domotic.authentication.UserClientPairType;
import org.openbase.type.domotic.state.ActionStateType;
import org.openbase.type.domotic.state.PowerStateType;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.fail;

public class RemoteActionTest extends AbstractBCOLocationManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteActionTest.class);

    public RemoteActionTest() {
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {

    }

    @Test(timeout = 10000)
    public void testExecutionAndCancellationWithToken() throws Exception {
        System.out.println("testExecutionAndCancellationWithToken");

        // login as admin
        final SessionManager sessionManager = new SessionManager();
        sessionManager.loginUser(Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_USER_ALIAS).getId(), UserCreationPlugin.ADMIN_PASSWORD, false);

        // register a new user which is not an admin
        final String username = "Undercover";
        final String password = "Agent";
        UnitConfigType.UnitConfig.Builder userUnitConfig = UnitConfigType.UnitConfig.newBuilder().setUnitType(UnitType.USER);
        userUnitConfig.getUserConfigBuilder().setFirstName("4").setLastName("7").setUserName(username);
        final String userId = Registries.getUnitRegistry().registerUnitConfig(userUnitConfig.build()).get().getId();
        sessionManager.registerUser(userId, password, false).get();

        // login new user
        sessionManager.logout();
        sessionManager.loginUser(userId, password, false);

        // request authentication token for new user
        AuthenticatedValue authenticatedValue = sessionManager.initializeRequest(AuthenticationToken.newBuilder().setUserId(sessionManager.getUserClientPair().getUserId()).build(), null);
        final String authenticationToken = new AuthenticatedValueFuture<>(
                Registries.getUnitRegistry().requestAuthenticationTokenAuthenticated(authenticatedValue),
                String.class,
                authenticatedValue.getTicketAuthenticatorWrapper(),
                sessionManager).get();
        final AuthTokenType.AuthToken authToken = AuthTokenType.AuthToken.newBuilder().setAuthenticationToken(authenticationToken).build();

        // logout user
        sessionManager.logout();

        final LocationRemote locationRemote = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);
        List<? extends ColorableLightRemote> units = locationRemote.getUnits(UnitType.COLORABLE_LIGHT, true, Units.COLORABLE_LIGHT);

        final ActionParameterType.ActionParameter actionParameter = ActionParameterType.ActionParameter.newBuilder().setAuthToken(authToken).build();

        for (int i = 0; i < 10; i++) {
            PowerStateType.PowerState.State powerState = (i % 2 == 0) ? PowerStateType.PowerState.State.ON : PowerStateType.PowerState.State.OFF;

            final RemoteAction remoteAction = new RemoteAction(locationRemote.setPowerState(powerState, UnitType.COLORABLE_LIGHT, actionParameter), authToken);
            remoteAction.waitForExecution();

            for (ColorableLightRemote unit : units) {
                boolean actionIsExecuting = false;
                for (ActionReferenceType.ActionReference actionReference : unit.getActionList().get(0).getActionCauseList()) {
                    if (actionReference.getActionId().equals(remoteAction.getId())) {
                        actionIsExecuting = true;
                        break;
                    }
                }

                assertTrue("Action on unit[" + unit + "] is not executing", actionIsExecuting);
                assertEquals("Action was not authenticated by the correct user", userId, unit.getActionList().get(0).getActionInitiator().getAuthenticatedBy());
            }

            remoteAction.cancel().get();

            // validate that action is cancelled on all units
            for (final ColorableLightRemote colorableLightRemote : units) {
                ActionDescriptionType.ActionDescription causedAction = null;
                for (final ActionDescriptionType.ActionDescription description : colorableLightRemote.getActionList()) {
                    for (ActionReferenceType.ActionReference actionReference : description.getActionCauseList()) {
                        if (actionReference.getActionId().equals(remoteAction.getId())) {
                            causedAction = description;
                            break;
                        }
                    }
                }

                if (causedAction == null) {
                    fail("Caused action on unit[" + colorableLightRemote + "]could not be found!");
                }

                Assert.assertEquals("Action on unit[" + colorableLightRemote + "] was not cancelled!", ActionStateType.ActionState.State.CANCELED, causedAction.getActionState().getValue());
            }

        }
    }
}
