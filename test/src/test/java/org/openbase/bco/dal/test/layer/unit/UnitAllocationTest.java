package org.openbase.bco.dal.test.layer.unit;

/*-
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.junit.*;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.jp.JPUnitAllocation;
import org.openbase.bco.dal.lib.layer.unit.user.UserController;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionDescriptionType.ActionDescription.Builder;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActionStateType.ActionState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PowerStateType.PowerState.State;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import rst.vision.HSBColorType.HSBColor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitAllocationTest extends AbstractBCODeviceManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitAllocationTest.class);

    private static ColorableLightRemote colorableLightRemote;

    public UnitAllocationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        // activate unit allocation
        JPService.registerProperty(JPUnitAllocation.class, true);
        // trigger super method
        AbstractBCODeviceManagerTest.setUpClass();
        // retrieve colorable light remote
        colorableLightRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.COLORABLE_LIGHT), true, ColorableLightRemote.class);
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        AbstractBCODeviceManagerTest.tearDownClass();
    }


    @Before
    public void setUp() throws Exception {
        //TODO how to reset light
//        colorableLightRemote.setColor(RGBColor.newBuilder().setBlue(0).setGreen(0).setRed(0).build()).get();
//        final ActionDescription actionDescription = colorableLightRemote.setPowerState(State.OFF).get();
//        colorableLightRemote.cancelAction(actionDescription).get();
//
//        assertEquals("Colorable light still contains action after reset", 0, colorableLightRemote.getData().getActionCount());
//        assertEquals("ColorableLight is not turned off", State.OFF, colorableLightRemote.getPowerState().getValue());
        //TODO add test
//        assertEquals("ColorableLight color is not black", State.OFF, colorableLightRemote.getPowerState().getValue());
    }

    @After
    public void tearDown() throws CouldNotPerformException {

    }

    /**
     * Test if an action which is executed notifies all action states correctly and if it is cancelled correctly.
     *
     * @throws Exception if an error occurs.
     */
    @Test(timeout = 5000)
    public void testActionStateNotifications() throws Exception {
        LOGGER.info("testActionStateNotifications");
        // expected order of action states
        final ActionState.State[] actionStates = {
                ActionState.State.EXECUTING,
                ActionState.State.CANCELED
        };

        // observer testing if all action state updates are notified as expected
        final ActionStateObserver actionStateObserver = new ActionStateObserver(actionStates);
        // add actions state observer
        colorableLightRemote.addDataObserver(actionStateObserver);

        // set the power state of the colorable light
        final RemoteAction remoteAction = new RemoteAction(colorableLightRemote.setPowerState(State.ON));
        remoteAction.addActionDescriptionObserver((source, data) -> LOGGER.warn("Remote action received state {}", data.getActionState().getValue()));

        // wait for executing because it is likely to be initiating before
        remoteAction.waitForActionState(ActionState.State.EXECUTING);

        // validate that the action is available from unit data
        assertTrue("Unit data does not contain any action descriptions", colorableLightRemote.getData().getActionCount() > 0);
        // validate the initiator of the action
        assertEquals("Unexpected action initiator", SessionManager.getInstance().getClientId(), remoteAction.getActionDescription().getActionInitiator().getInitiatorId());
        // validate that the action is currently executing
        assertEquals("ActionState is not executing", ActionState.State.EXECUTING, remoteAction.getActionState());
        // validate that the power state is set
        assertEquals("PowerState has not been updated", State.ON, colorableLightRemote.getData().getPowerState().getValue());

        // cancel the action
        remoteAction.cancel().get();

        // validate that the action is cancelled
        assertEquals("ActionState is not canceled", ActionState.State.CANCELED, remoteAction.getActionState());
        // validate that all states were notified
        assertEquals("Not all action states have been notified", actionStates.length, actionStateObserver.getStateIndex());

        // remove the action state observer
        colorableLightRemote.removeDataObserver(actionStateObserver);
    }

    /**
     * Observer which makes sure that all of a set of action states were notified in their specified order.
     */
    private class ActionStateObserver implements Observer<DataProvider<ColorableLightData>, ColorableLightData> {

        private final ActionState.State[] actionStates;
        // current index for the action states array
        private int stateIndex;

        ActionStateObserver(final ActionState.State[] actionStates) {
            this.actionStates = actionStates;
            stateIndex = 0;
        }

        @Override
        public void update(DataProvider<ColorableLightData> source, ColorableLightData data) {
            // validate that no notification takes place while action state index is completed
            assertTrue("Unexpected number of action state updates", stateIndex < actionStates.length);

            // do nothing if the there is no action
            if (data.getActionCount() == 0) {
                return;
            }

            // do nothing if the action state has not been updated
            if (stateIndex > 0 && data.getAction(0).getActionState().getValue() == actionStates[stateIndex - 1]) {
                return;
            }

            // test if the new action state is the expected one and increase the index
            assertEquals("Unexpected new action state", actionStates[stateIndex], data.getAction(0).getActionState().getValue());
            stateIndex++;
        }

        int getStateIndex() {
            return stateIndex;
        }
    }

    /**
     * Test if another action from the same initiator is scheduled, the newer one is executed.
     *
     * @throws Exception if an error occurs.
     */
    @Test(timeout = 5000)
    public void testMultiActionsBySameInitiator() throws Exception {
        LOGGER.info("testMultiActionsBySameInitiator");
        // set the power state of the colorable light
        final RemoteAction firstAction = new RemoteAction(colorableLightRemote.setPowerState(State.ON));
        firstAction.waitForActionState(ActionState.State.EXECUTING);

        assertEquals(firstAction.getId(), colorableLightRemote.getActionList().get(0).getId());
        assertEquals(ActionState.State.EXECUTING, firstAction.getActionState());
        // validate that color value was set
        assertEquals(State.ON, colorableLightRemote.getPowerState().getValue());

        // set the color of the light
        final HSBColor hsb = HSBColor.newBuilder().setBrightness(100).setHue(12).setSaturation(100).build();
        final RemoteAction secondAction = new RemoteAction(colorableLightRemote.setColor(hsb));
        secondAction.waitForActionState(ActionState.State.EXECUTING);

        assertEquals(secondAction.getId(), colorableLightRemote.getActionList().get(0).getId());
        assertEquals(ActionState.State.EXECUTING, secondAction.getActionState());
        // validate that color value was set
        assertEquals(hsb, colorableLightRemote.getColorState().getColor().getHsbColor());
        // validate that previous action became rejected
        assertEquals(ActionState.State.REJECTED, firstAction.getActionState());

        // cancel running actions for next test
        secondAction.cancel().get();
    }

    /**
     * Test rescheduling.
     *
     * @throws Exception if an error occurs.
     */
    @Test(timeout = 5000)
    public void testRescheduling() throws Exception {
        LOGGER.info("testRescheduling");
        final SessionManager sessionManager = new SessionManager();
        sessionManager.login(Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_USER_ALIAS).getId(), UserCreationPlugin.ADMIN_PASSWORD);

        // set the power state of the colorable light with the bco user
        final RemoteAction firstAction = new RemoteAction(colorableLightRemote.setPowerState(State.ON));
        firstAction.waitForActionState(ActionState.State.EXECUTING);

        assertEquals(firstAction.getId(), colorableLightRemote.getActionList().get(0).getId());
        assertEquals(ActionState.State.EXECUTING, firstAction.getActionState());
        // validate that color value was set
        assertEquals(State.ON, colorableLightRemote.getPowerState().getValue());

        // set the power state with the admin user
        Builder builder = ActionDescriptionProcessor.generateActionDescriptionBuilder(PowerState.newBuilder().setValue(State.OFF).build(), ServiceType.POWER_STATE_SERVICE, colorableLightRemote);
        builder.getActionInitiatorBuilder().setInitiatorId(sessionManager.getUserId());
        AuthenticatedValue authenticatedValue = sessionManager.initializeRequest(builder.build(), null, null);
        AuthenticatedValueFuture<ActionDescription> authenticatedValueFuture = new AuthenticatedValueFuture<>(colorableLightRemote.applyActionAuthenticated(authenticatedValue), ActionDescription.class, authenticatedValue.getTicketAuthenticatorWrapper(), sessionManager);
        final RemoteAction secondAction = new RemoteAction(authenticatedValueFuture);
        secondAction.waitForActionState(ActionState.State.EXECUTING);
        firstAction.waitForActionState(ActionState.State.SCHEDULED);

        assertEquals(secondAction.getId(), colorableLightRemote.getActionList().get(0).getId());
        assertEquals(ActionState.State.EXECUTING, secondAction.getActionState());
        // validate that light was turned off
        assertEquals(State.OFF, colorableLightRemote.getPowerState().getValue());
        // validate that previous action became scheduled
        assertEquals(ActionState.State.SCHEDULED, firstAction.getActionState());

        // cancel running action
        secondAction.cancel().get();
        // wait until old action is rescheduled to executing
        firstAction.waitForActionState(ActionState.State.EXECUTING);

        assertEquals(firstAction.getId(), colorableLightRemote.getActionList().get(0).getId());
        assertEquals(ActionState.State.EXECUTING, firstAction.getActionState());
        assertEquals(ActionState.State.CANCELED, secondAction.getActionState());
        // validate that color value was set
        assertEquals(State.ON, colorableLightRemote.getPowerState().getValue());

        // cancel remaining action for the next test
        firstAction.cancel().get();
    }
}
