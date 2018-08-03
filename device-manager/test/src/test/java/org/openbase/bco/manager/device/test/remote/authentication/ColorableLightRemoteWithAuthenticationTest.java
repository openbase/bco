package org.openbase.bco.manager.device.test.remote.authentication;

/*
 * #%L
 * BCO Manager Device Test
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authentication.lib.AuthenticatedServerManager;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.authentication.lib.jp.JPSessionTimeout;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.TimestampJavaTimeTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import rst.domotic.authentication.AuthorizationTokenType.AuthorizationToken;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketType.Ticket;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerStateType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PowerStateType.PowerState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.user.UserConfigType.UserConfig;
import rst.timing.IntervalType;
import rst.timing.IntervalType.Interval;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
 */
public class ColorableLightRemoteWithAuthenticationTest extends AbstractBCODeviceManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColorableLightRemoteWithAuthenticationTest.class);

    private static ColorableLightRemote colorableLightRemote;

    private final SessionManager sessionManager;

    public ColorableLightRemoteWithAuthenticationTest() {
        sessionManager = new SessionManager();
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        JPService.registerProperty(JPAuthentication.class, true);
        AbstractBCODeviceManagerTest.setUpClass();
    }

    @Before
    public void setUp() throws CouldNotPerformException, InterruptedException {
        sessionManager.login(Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_USER_ALIAS).getId(), UserCreationPlugin.ADMIN_PASSWORD);

        colorableLightRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.COLORABLE_LIGHT), true, ColorableLightRemote.class);
        colorableLightRemote.setSessionManager(sessionManager);
    }

    @After
    public void tearDown() throws CouldNotPerformException {
        sessionManager.logout();
        colorableLightRemote.setSessionManager(SessionManager.getInstance());
    }

    /**
     * Test if executing an action works with prior user login and authentication.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColorWithAuthentication() throws Exception {
        System.out.println("testSetColorWithAuthentication");

        colorableLightRemote.setPowerState(PowerStateType.PowerState.State.ON).get();
        colorableLightRemote.requestData().get();
        assertEquals("Power has not been set in time!", PowerStateType.PowerState.State.ON, colorableLightRemote.getData().getPowerState().getValue());
    }

    /**
     * Test if executing an action works with prior client login and authentication, but then with an expired session.
     * So first, the relog should be executed for a client and then the action should be executed.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColorWithClientWithoutAuthentication() throws Exception {
        System.out.println("testSetColorWithClientWithoutAuthentication");

        UnitConfig.Builder testClient = UnitConfig.newBuilder();
        testClient.setUnitType(UnitType.USER);
        UserConfig.Builder userConfig = testClient.getUserConfigBuilder();
        userConfig.setUserName("UnitTestClient");
        userConfig.setFirstName("First");
        userConfig.setLastName("Last");
        testClient.getPermissionConfigBuilder().getOtherPermissionBuilder().setWrite(true).setAccess(true).setRead(true);
        UnitConfig registered = Registries.getUnitRegistry().registerUnitConfig(testClient.build()).get();

        // register client
        System.out.println("register client");
        sessionManager.registerClient(registered.getId());

        // logout admin
        System.out.println("logout admin");
        sessionManager.logout();

        // login client
        System.out.println("login client");
        sessionManager.login(registered.getId());

        // make ticket invalid
        byte[] serviceServerSecretKey = AuthenticatedServerManager.getInstance().getServiceServerSecretKey();
        System.out.println("make ticket invalid");
        Ticket ticket = EncryptionHelper.decryptSymmetric(sessionManager.getTicketAuthenticatorWrapper().getTicket(), serviceServerSecretKey, Ticket.class);

        long currentTime = 0;
        Interval.Builder validityInterval = IntervalType.Interval.newBuilder();
        validityInterval.setBegin(TimestampJavaTimeTransform.transform(currentTime));
        validityInterval.setEnd(TimestampJavaTimeTransform.transform(currentTime + JPService.getProperty(JPSessionTimeout.class).getValue()));
        validityInterval.build();

        Ticket.Builder cstb = ticket.toBuilder();
        cstb.setValidityPeriod(validityInterval.build());

        TicketAuthenticatorWrapper.Builder wrapperBuilder = sessionManager.getTicketAuthenticatorWrapper().toBuilder();
        wrapperBuilder.setTicket(EncryptionHelper.encryptSymmetric(cstb.build(), serviceServerSecretKey));

        sessionManager.updateTicketAuthenticatorWrapper(wrapperBuilder.build());

        // execute action
        System.out.println("execute action");
        colorableLightRemote.setPowerState(PowerStateType.PowerState.State.ON).get();
        colorableLightRemote.requestData().get();
        assertEquals("Power has not been set in time!", PowerStateType.PowerState.State.ON, colorableLightRemote.getData().getPowerState().getValue());
    }

    /**
     * Test if executing an action works with prior user login and authentication, but then with an expired session.
     * So first, the relog should be executed for a user and then the action should be executed.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColorWithUserWithoutAuthentication() throws Exception {
        System.out.println("testSetColorWithClientWithoutAuthentication");

        byte[] serviceServerSecretKey = AuthenticatedServerManager.getInstance().getServiceServerSecretKey();
        // make ticket invalid
        System.out.println("make ticket invalid");
        Ticket ticket = EncryptionHelper.decryptSymmetric(sessionManager.getTicketAuthenticatorWrapper().getTicket(), serviceServerSecretKey, Ticket.class);

        long currentTime = 0;
        Interval.Builder validityInterval = IntervalType.Interval.newBuilder();
        validityInterval.setBegin(TimestampJavaTimeTransform.transform(currentTime));
        validityInterval.setEnd(TimestampJavaTimeTransform.transform(currentTime + JPService.getProperty(JPSessionTimeout.class).getValue()));
        validityInterval.build();

        Ticket.Builder cstb = ticket.toBuilder();
        cstb.setValidityPeriod(validityInterval.build());

        TicketAuthenticatorWrapper.Builder wrapperBuilder = sessionManager.getTicketAuthenticatorWrapper().toBuilder();
        wrapperBuilder.setTicket(EncryptionHelper.encryptSymmetric(cstb.build(), serviceServerSecretKey));

        sessionManager.updateTicketAuthenticatorWrapper(wrapperBuilder.build());

        // execute action
        System.out.println("execute action");
        colorableLightRemote.setPowerState(PowerStateType.PowerState.State.ON).get();
        colorableLightRemote.requestData().get();
        assertEquals("Power has not been set in time!", PowerStateType.PowerState.State.ON, colorableLightRemote.getData().getPowerState().getValue());
    }

    @Test(timeout = 15000)
    public void testApplyActionWithToken() throws Exception {
        System.out.println("testApplyActionWithToken");

        // Remove other access permission from root location so that new users should not be able to control it
        final UnitConfig.Builder rootLocation = Registries.getUnitRegistry().getRootLocationConfig().toBuilder();
        rootLocation.getPermissionConfigBuilder().getOtherPermissionBuilder().setAccess(false);
        Registries.getUnitRegistry().updateUnitConfig(rootLocation.build()).get();

        // Register a new user that does not have access permissions
        final String username = "Murray";
        final String password = "skull";
        UnitConfig.Builder userUnitConfig = UnitConfig.newBuilder().setUnitType(UnitType.USER);
        userUnitConfig.getUserConfigBuilder().setFirstName("Murray").setLastName("the skull").setUserName(username);
        userUnitConfig = Registries.getUnitRegistry().registerUnitConfig(userUnitConfig.build()).get().toBuilder();
        sessionManager.registerUser(userUnitConfig.getId(), password, false);

        // request authentication and authorization tokens for admin user
        AuthenticatedValue authenticatedValue = sessionManager.initializeRequest(AuthenticationToken.newBuilder().setUserId(sessionManager.getUserId()).build(), null, null);
        final String authenticationToken = new AuthenticatedValueFuture<>(
                Registries.getUnitRegistry().requestAuthenticationTokenAuthenticated(authenticatedValue),
                String.class,
                authenticatedValue.getTicketAuthenticatorWrapper(),
                sessionManager).get();
        AuthorizationToken.Builder authorizationToken = AuthorizationToken.newBuilder().setUserId(sessionManager.getUserId());
        AuthorizationToken.PermissionRule.Builder permissionRuleBuilder = authorizationToken.addPermissionRuleBuilder();
        permissionRuleBuilder.setUnitId(colorableLightRemote.getId());
        permissionRuleBuilder.getPermissionBuilder().setAccess(true).setRead(true).setWrite(false);
        authenticatedValue = sessionManager.initializeRequest(authorizationToken.build(), null, null);
        final String token = new AuthenticatedValueFuture<>(
                Registries.getUnitRegistry().requestAuthorizationTokenAuthenticated(authenticatedValue),
                String.class,
                authenticatedValue.getTicketAuthenticatorWrapper(),
                sessionManager).get();

        // login previously registered user
        sessionManager.login(userUnitConfig.getId(), password);

        // try to set the power state which should fail
        try {
            ExceptionPrinter.setBeQuit(true);
            colorableLightRemote.setPowerState(State.ON).get();
            assertTrue("Could set power state without access permissions", false);
        } catch (ExecutionException ex) {
            // this should happen
//            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.INFO);
        } finally {
            ExceptionPrinter.setBeQuit(false);
        }

        PowerState.Builder powerState = PowerState.newBuilder().setValue(State.ON);
        ActionDescription actionDescription = ActionDescriptionProcessor.generateActionDescriptionBuilderAndUpdate(powerState.build(), ServiceType.POWER_STATE_SERVICE, colorableLightRemote).build();

        authenticatedValue = sessionManager.initializeRequest(actionDescription, null, token);
        colorableLightRemote.applyActionAuthenticated(authenticatedValue).get();
        assertEquals(State.ON, colorableLightRemote.getPowerState().getValue());

        powerState = PowerState.newBuilder().setValue(State.OFF);
        actionDescription = ActionDescriptionProcessor.generateActionDescriptionBuilderAndUpdate(powerState.build(), ServiceType.POWER_STATE_SERVICE, colorableLightRemote).build();

        authenticatedValue = sessionManager.initializeRequest(actionDescription, authenticationToken, token);
        colorableLightRemote.applyActionAuthenticated(authenticatedValue).get();
        assertEquals(State.OFF, colorableLightRemote.getPowerState().getValue());

        // reset root location permissions to not interfere with other tests
        rootLocation.getPermissionConfigBuilder().getOtherPermissionBuilder().setAccess(true);
        Registries.getUnitRegistry().updateUnitConfig(rootLocation.build()).get();
    }
}
