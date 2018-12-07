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
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.jp.JPUnitAllocation;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionParameterType.ActionParameter.Builder;
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
        // expected order of action states
        final ActionState.State[] actionStates = {
//                ActionState.State.INITIALIZED,
//                ActionState.State.INITIATING,
                ActionState.State.EXECUTING,
                ActionState.State.CANCELED
        };

        // observer testing if all action state updates are notified as expected
        final ActionStateObserver actionStateObserver = new ActionStateObserver(actionStates);
        // add actions state observer
        colorableLightRemote.addDataObserver(actionStateObserver);

        // set the power state of the colorable light
//        final ActionDescription actionDescription = colorableLightRemote.setPowerState(State.ON);
        final RemoteAction remoteAction = new RemoteAction(colorableLightRemote.setPowerState(State.ON));
        remoteAction.addActionDescriptionObserver(new Observer<RemoteAction, ActionDescription>() {
            @Override
            public void update(RemoteAction source, ActionDescription data) throws Exception {
                LOGGER.warn("Remote action received state {}", data.getActionState().getValue());
            }
        });
        remoteAction.waitForSubmission();
        Thread.sleep(2000);

        // validate that the action is available from unit data
        assertEquals("Unit data does not contain the action description", 1, colorableLightRemote.getData().getActionCount());
        // validate the initiator of the action
        assertEquals("Unexpected action initiator", SessionManager.getInstance().getClientId(), remoteAction.getActionDescription().getActionInitiator().getInitiatorId());
        // validate that the action is currently executing
        assertEquals("ActionState is not executing", ActionState.State.EXECUTING, remoteAction.getActionState());
        // validate that executing action description is the same
        assertEquals("ActionDescriptions differ", remoteAction.getActionDescription(), colorableLightRemote.getData().getAction(0));
        // validate that the power state is set
        assertEquals("PowerState has not been updated", State.ON, colorableLightRemote.getData().getPowerState().getValue());

        // cancel the action
        remoteAction.cancel().get();

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
            LOGGER.info("Received data with transaction id [" + data.getTransactionId() + "]");
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
        // set the power state of the colorable light
        ActionDescription actionDescription = colorableLightRemote.setPowerState(State.ON).get();

        // validate that the action is available from unit data
        assertEquals("Unit data does not contain the action description", 1, colorableLightRemote.getData().getActionCount());
        // validate action description
        assertEquals("ActionDescriptions differ", actionDescription, colorableLightRemote.getData().getAction(0));

        // set color and test if old action is removed
        actionDescription = colorableLightRemote.setColor(HSBColor.newBuilder().setBrightness(100).setHue(0).setSaturation(100).build()).get();

        // validate that only one action is available
        assertEquals("Unit data does not contain the action description", 1, colorableLightRemote.getData().getActionCount());
        // validate that it is the expected action description
        assertEquals("ActionDescriptions differ", actionDescription, colorableLightRemote.getData().getAction(0));

        Builder actionParameter = ActionDescriptionProcessor.generateDefaultActionParameter(PowerState.newBuilder().setValue(State.OFF).build(), ServiceType.POWER_STATE_SERVICE);
        ActionDescription.Builder olderActionDescription = ActionDescriptionProcessor.generateActionDescriptionBuilder(actionParameter);
        olderActionDescription.getTimestampBuilder().setTime(actionDescription.getTimestamp().getTime() - 1000);
        ActionDescription test = colorableLightRemote.applyAction(olderActionDescription.build()).get();

        LOGGER.info("test {}", test);

        // validate that only one action is available
        assertEquals("Unit data does not contain the action description", 1, colorableLightRemote.getData().getActionCount());
        // validate that it is still the old action executing
        assertEquals("ActionDescriptions differ", actionDescription, colorableLightRemote.getData().getAction(0));
    }
}
