package org.openbase.bco.dal.test.layer.unit;

/*-
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.session.TokenGenerator;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription.Builder;
import org.openbase.type.domotic.action.ActionEmphasisType.ActionEmphasis.Category;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitAllocationTest extends AbstractBCODeviceManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitAllocationTest.class);

    private static ColorableLightRemote colorableLightRemote;

    private static final long VALID_EXECUTION_VARIATION = 100;

    private final SessionManager sessionManager;
    private AuthToken adminToken = null;

    public UnitAllocationTest() {
        this.sessionManager = new SessionManager();
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {

        // uncomment to enable debug mode
        // JPService.registerProperty(JPDebugMode.class, true);
        // JPService.registerProperty(JPLogLevel.class, LogLevel.DEBUG);

        // trigger super method
        AbstractBCODeviceManagerTest.setUpClass();

        // retrieve colorable light remote
        colorableLightRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.COLORABLE_LIGHT), true, ColorableLightRemote.class);

        // uncomment to visualize action inspector during tests
        // String[] args = {};
        // new Thread(() -> BCOActionInspector.main(args)).start();
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        AbstractBCODeviceManagerTest.tearDownClass();
    }


    @Before
    public void setUp() throws Exception {
        sessionManager.loginUser(Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_USER_ALIAS).getId(), UserCreationPlugin.ADMIN_PASSWORD, false);

        if (adminToken == null) {
            adminToken = TokenGenerator.generateAuthToken(sessionManager);
        }
    }

    @After
    public void tearDown() {
        sessionManager.logout();
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
        colorableLightRemote.addDataObserver(ServiceTempus.UNKNOWN, actionStateObserver);

        // set the power state of the colorable light
        final RemoteAction remoteAction = observe(colorableLightRemote.setPowerState(State.ON, ActionParameter.newBuilder().setExecutionTimePeriod(TimeUnit.SECONDS.toMicros(10)).build()));
        remoteAction.addActionDescriptionObserver((source, data) -> LOGGER.warn("Remote action received state {}", data.getActionState().getValue()));

        // wait for executing because it is likely to be initiating before
        remoteAction.waitForActionState(ActionState.State.EXECUTING);

        // validate that the action is available from unit data
        assertTrue("Unit data does not contain any action descriptions", colorableLightRemote.getData().getActionCount() > 0);
        // validate the initiator of the action
        assertEquals("Unexpected action initiator", SessionManager.getInstance().getUserClientPair().getClientId(), remoteAction.getActionDescription().getActionInitiator().getInitiatorId());
        // validate that the action is currently executing
        assertEquals("ActionState is not executing", ActionState.State.EXECUTING, remoteAction.getActionState());
        // validate that the power state is set
        assertEquals("PowerState has not been updated", State.ON, colorableLightRemote.getData().getPowerState().getValue());

        // cancel the action
        System.out.println("try to cancel");
        final Future<ActionDescription> cancelFuture = remoteAction.cancel();
        System.out.println("wait for cancel");
        cancelFuture.get();
        System.out.println("canceled");
        colorableLightRemote.requestData().get();

        // validate that the action is cancelled
        assertEquals("ActionState is not canceled", ActionState.State.CANCELED, remoteAction.getActionState());

        // validate state order
        actionStateObserver.validateActionStates(remoteAction.getActionId());

        // validate that all states were notified
        assertEquals("Not all action states have been notified", actionStates.length, actionStateObserver.getReceivedActionStateCounter(remoteAction.getActionId()));

        // remove the action state observer
        colorableLightRemote.removeDataObserver(actionStateObserver);
    }

    /**
     * Observer which makes sure that all of a set of action states were notified in their specified order.
     */
    private static class ActionStateObserver implements Observer<DataProvider<ColorableLightData>, ColorableLightData> {

        private final ActionState.State[] actionStates;


        private final SyncObject actionIdStateMapLock = new SyncObject("ActionIdStateMapLock");
        private final HashMap<String, ArrayList<ActionState.State>> actionIdStateMap;

        ActionStateObserver(final ActionState.State[] actionStates) {
            this.actionStates = actionStates;
            this.actionIdStateMap = new HashMap<>();
        }

        @Override
        public void update(final DataProvider<ColorableLightData> source, final ColorableLightData data) {

            System.out.println("ActionStateObserver: data update received");

            // do nothing if the there is no action
            if (data.getActionCount() == 0) {
                return;
            }

            synchronized (actionIdStateMapLock) {
                for (ActionDescription actionDescription : data.getActionList()) {

                    // filter non observed states
                    if (Arrays.stream(actionStates).noneMatch(actionDescription.getActionState().getValue()::equals)) {
                        continue;
                    }

                    // create missing units
                    if (!actionIdStateMap.containsKey(actionDescription.getActionId())) {
                        actionIdStateMap.put(actionDescription.getActionId(), new ArrayList<>());
                    }

                    final ArrayList<ActionState.State> stateList = actionIdStateMap.get(actionDescription.getActionId());

                    // do nothing if the action state has not been updated
                    if (!stateList.isEmpty() && stateList.get(stateList.size() - 1) == actionDescription.getActionState().getValue()) {
                        continue;
                    }

                    stateList.add(actionDescription.getActionState().getValue());
                }
            }
        }

        public int getReceivedActionStateCounter(final String actionId) {
            synchronized (actionIdStateMapLock) {
                return actionIdStateMap.get(actionId).size();
            }
        }

        void validateActionStates(final String actionId) {
            synchronized (actionIdStateMapLock) {
                assertEquals("Unexpected action state order.", StringProcessor.transformCollectionToString(Arrays.asList(actionStates), ", "), StringProcessor.transformCollectionToString(actionIdStateMap.get(actionId), ", "));
            }
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
        final RemoteAction firstAction = observe(colorableLightRemote.setPowerState(State.ON, ActionParameter.newBuilder().setExecutionTimePeriod(TimeUnit.SECONDS.toMicros(10)).build()));
        firstAction.waitForActionState(ActionState.State.EXECUTING);

        assertEquals(firstAction.getId(), colorableLightRemote.getActionList().get(0).getActionId());
        assertEquals(ActionState.State.EXECUTING, firstAction.getActionState());
        // validate that color value was set
        assertEquals(State.ON, colorableLightRemote.getPowerState().getValue());

        // set the color of the light
        final HSBColor hsb = HSBColor.newBuilder().setBrightness(1.0d).setHue(12).setSaturation(1.0d).build();
        final RemoteAction secondAction = observe(colorableLightRemote.setColor(hsb, ActionParameter.newBuilder().setExecutionTimePeriod(TimeUnit.SECONDS.toMicros(10)).build()));
        secondAction.waitForActionState(ActionState.State.EXECUTING);

        assertEquals(secondAction.getId(), colorableLightRemote.getActionList().get(0).getActionId());
        assertEquals(ActionState.State.EXECUTING, secondAction.getActionState());
        // validate that color value was set
        assertEquals(hsb, colorableLightRemote.getColorState().getColor().getHsbColor());
        // validate that previous action became rejected
        assertEquals(ActionState.State.REJECTED, firstAction.getActionState());

        // cancel running actions for next test
        secondAction.cancel().get();
        secondAction.waitForActionState(ActionState.State.CANCELED);
    }

    /**
     * Test rescheduling.
     *
     * @throws Exception if an error occurs.
     */
    @Test(timeout = 10000)
    public void testRescheduling() throws Exception {
        LOGGER.info("testRescheduling");

        // set the power state of the colorable light hue with the bco user
        final RemoteAction firstAction = observe(colorableLightRemote.setPowerState(State.ON, ActionParameter.newBuilder().setExecutionTimePeriod(TimeUnit.SECONDS.toMicros(10)).build()));
        firstAction.waitForActionState(ActionState.State.EXECUTING);

        assertEquals(firstAction.getId(), colorableLightRemote.getActionList().get(0).getActionId());
        assertEquals(ActionState.State.EXECUTING, firstAction.getActionState());
        // validate that color value was set
        assertEquals(State.ON, colorableLightRemote.getPowerState().getValue());

        // set the power state with the admin user
        Builder builder = ActionDescriptionProcessor.generateActionDescriptionBuilder(PowerState.newBuilder().setValue(State.OFF).build(), ServiceType.POWER_STATE_SERVICE, colorableLightRemote);
        builder.getActionInitiatorBuilder().setInitiatorId(sessionManager.getUserClientPair().getUserId());
        AuthenticatedValue authenticatedValue = sessionManager.initializeRequest(builder.build(), null);
        AuthenticatedValueFuture<ActionDescription> authenticatedValueFuture = new AuthenticatedValueFuture<>(colorableLightRemote.applyActionAuthenticated(authenticatedValue), ActionDescription.class, authenticatedValue.getTicketAuthenticatorWrapper(), sessionManager);
        final RemoteAction secondAction = new RemoteAction(authenticatedValueFuture);
        secondAction.waitForActionState(ActionState.State.EXECUTING);
        firstAction.waitForActionState(ActionState.State.SCHEDULED);

        assertEquals(secondAction.getId(), colorableLightRemote.getActionList().get(0).getActionId());
        assertEquals(ActionState.State.EXECUTING, secondAction.getActionState());
        // validate that light was turned off
        assertEquals(State.OFF, colorableLightRemote.getPowerState().getValue());
        // validate that previous action became scheduled
        assertEquals(ActionState.State.SCHEDULED, firstAction.getActionState());

        // cancel running action
        builder = secondAction.getActionDescription().toBuilder().setCancel(true);
        authenticatedValue = sessionManager.initializeRequest(builder.build(), null);
        new AuthenticatedValueFuture<>(colorableLightRemote.applyActionAuthenticated(authenticatedValue), ActionDescription.class, authenticatedValue.getTicketAuthenticatorWrapper(), sessionManager).get();
        // wait until old action is rescheduled to executing
        firstAction.waitForActionState(ActionState.State.EXECUTING);

        assertEquals(firstAction.getId(), colorableLightRemote.getActionList().get(0).getActionId());
        assertEquals(ActionState.State.EXECUTING, firstAction.getActionState());
        assertEquals(ActionState.State.CANCELED, secondAction.getActionState());
        // validate that color value was set
        assertEquals(State.ON, colorableLightRemote.getPowerState().getValue());

        // cancel remaining action for the next test
        firstAction.cancel().get();
        firstAction.waitForActionState(ActionState.State.CANCELED);
    }

    /**
     * Test prioritize action execution and low prioritized rescheduling afterwards.
     *
     * @throws Exception if an error occurs.
     */
    @Test(timeout = 15000)
    public void testPriorityHandling() throws Exception {
        LOGGER.info("testPriorityHandling");

        // uncomment to delay the test startup in case you need to prepare the action inspector
        // System.out.println("prepare...");
        // Thread.sleep(10000);
        // System.out.println("ready...");
        // Thread.sleep(10000);
        // System.out.println("go...");

        // set the brightness of the colorable light to 90
        final ActionParameter.Builder secondaryActionParameter = ActionParameter.newBuilder();
        secondaryActionParameter.setExecutionTimePeriod(TimeUnit.HOURS.toMicros(1));
        secondaryActionParameter.setPriority(Priority.LOW);
        secondaryActionParameter.addCategory(Category.ECONOMY);
        secondaryActionParameter.setSchedulable(true);
        secondaryActionParameter.setInterruptible(true);
        final RemoteAction secondaryAction = waitForExecution(colorableLightRemote.setBrightness(0.90d, secondaryActionParameter.build()), true);

        assertEquals(secondaryAction.getId(), colorableLightRemote.getActionList().get(0).getActionId());
        assertEquals(ActionState.State.EXECUTING, secondaryAction.getActionState());
        // validate that brightness value was set
        assertEquals(0.90d, colorableLightRemote.getBrightnessState().getBrightness(), 0.001);

        // set the brightness state with the admin user to 50 for 500 ms
        final ActionParameter.Builder primaryActionParameter = ActionDescriptionProcessor.generateDefaultActionParameter(BrightnessState.newBuilder().setBrightness(0.5d).build(), ServiceType.BRIGHTNESS_STATE_SERVICE, colorableLightRemote);
        primaryActionParameter.setExecutionTimePeriod(TimeUnit.MILLISECONDS.toMicros(6000));
        primaryActionParameter.setPriority(Priority.HIGH);

        primaryActionParameter.getActionInitiatorBuilder().setInitiatorId(sessionManager.getUserClientPair().getUserId());
        primaryActionParameter.getActionInitiatorBuilder().setInitiatorType(InitiatorType.SYSTEM);
        final ActionDescription actionDescription = ActionDescriptionProcessor.generateActionDescriptionBuilder(primaryActionParameter).build();
        assertTrue("initiator type not set.", actionDescription.getActionInitiator().hasInitiatorType());
        assertEquals("initiator type not correct.", InitiatorType.SYSTEM, actionDescription.getActionInitiator().getInitiatorType());

        AuthenticatedValue authenticatedValue = sessionManager.initializeRequest(actionDescription, null);

        AuthenticatedValueFuture<ActionDescription> authenticatedValueFuture = new AuthenticatedValueFuture<>(colorableLightRemote.applyActionAuthenticated(authenticatedValue), ActionDescription.class, authenticatedValue.getTicketAuthenticatorWrapper(), sessionManager);

        final RemoteAction primaryAction = observe(authenticatedValueFuture, adminToken, true);

        primaryAction.waitForActionState(ActionState.State.EXECUTING);
        secondaryAction.waitForActionState(ActionState.State.SCHEDULED);

        assertEquals(primaryAction.getId(), colorableLightRemote.getActionList().get(0).getActionId());
        assertEquals(ActionState.State.EXECUTING, primaryAction.getActionState());
        // validate that light brightness was adjusted to 50 percent
        assertEquals(0.5d, colorableLightRemote.getBrightnessState().getBrightness(), 0.001);
        // validate that previous action became scheduled
        assertEquals(ActionState.State.SCHEDULED, secondaryAction.getActionState());

        // make sure primary action is finalised.
        primaryAction.waitForActionState(ActionState.State.FINISHED);
        assertEquals(0, primaryAction.getExecutionTime());
        assertTrue(Math.abs(primaryAction.getLifetime() - 6000) < VALID_EXECUTION_VARIATION);
        assertEquals(false, primaryAction.isValid());
        assertEquals(true, primaryAction.isDone());
        assertEquals(false, primaryAction.isScheduled());

        // wait until old action is rescheduled to executing
        secondaryAction.waitForActionState(ActionState.State.EXECUTING);

        assertEquals(secondaryAction.getId(), colorableLightRemote.getActionList().get(0).getActionId());
        assertEquals(ActionState.State.EXECUTING, secondaryAction.getActionState());
        assertEquals(ActionState.State.FINISHED, primaryAction.getActionState());
        // validate that color value was set
        assertEquals(0.9d, colorableLightRemote.getBrightnessState().getBrightness(), 0.001);

        // cancel remaining action for the next test
        secondaryAction.cancel().get();
        secondaryAction.waitForActionState(ActionState.State.CANCELED);
    }

    /**
     * Test finalization after execution time period has passed.
     *
     * @throws Exception if an error occurs.
     */
    @Test(timeout = 5000)
    public void testFinalizationAfterExecutionTimePeriodPassed() throws Exception {
        LOGGER.info("testFinalizationAfterExecutionTimePeriodPassed");

        // set the brightness of the colorable light to 90
        final ActionParameter.Builder secondaryActionParameter = ActionParameter.newBuilder();
        secondaryActionParameter.setExecutionTimePeriod(TimeUnit.HOURS.toMicros(1));
        secondaryActionParameter.setPriority(Priority.LOW);
        secondaryActionParameter.setSchedulable(true);
        secondaryActionParameter.setInterruptible(true);
        final RemoteAction secondaryAction = observe(colorableLightRemote.setBrightness(.90d, secondaryActionParameter.build()));
        secondaryAction.waitForActionState(ActionState.State.EXECUTING);

        assertEquals(secondaryAction.getId(), colorableLightRemote.getActionList().get(0).getActionId());
        assertEquals(ActionState.State.EXECUTING, secondaryAction.getActionState());
        // validate that brightness value was set
        assertEquals(0.9d, colorableLightRemote.getBrightnessState().getBrightness(), 0.001);

        // set the brightness state with the admin user to 50 for 500 ms
        final ActionParameter.Builder primaryActionParameter = ActionDescriptionProcessor.generateDefaultActionParameter(BrightnessState.newBuilder().setBrightness(0.5d).build(), ServiceType.BRIGHTNESS_STATE_SERVICE, colorableLightRemote);
        primaryActionParameter.setExecutionTimePeriod(TimeUnit.MILLISECONDS.toMicros(500));
        primaryActionParameter.setPriority(Priority.HIGH);
        primaryActionParameter.setAutoContinueWithLowPriority(false);
        primaryActionParameter.getActionInitiatorBuilder().setInitiatorId(sessionManager.getUserClientPair().getUserId());
        AuthenticatedValue authenticatedValue = sessionManager.initializeRequest(ActionDescriptionProcessor.generateActionDescriptionBuilder(primaryActionParameter).build(), null);
        AuthenticatedValueFuture<ActionDescription> authenticatedValueFuture = new AuthenticatedValueFuture<>(colorableLightRemote.applyActionAuthenticated(authenticatedValue), ActionDescription.class, authenticatedValue.getTicketAuthenticatorWrapper(), sessionManager);

        final RemoteAction primaryAction = observe(authenticatedValueFuture, adminToken, true);
        primaryAction.waitForActionState(ActionState.State.EXECUTING);
        secondaryAction.waitForActionState(ActionState.State.SCHEDULED);

        assertEquals(primaryAction.getId(), colorableLightRemote.getActionList().get(0).getActionId());
        assertEquals(ActionState.State.EXECUTING, primaryAction.getActionState());

        // validate that light brightness was adjusted to 50 percent
        assertEquals(0.5d, colorableLightRemote.getBrightnessState().getBrightness(), 0.001);

        // validate that previous action became scheduled
        assertEquals(ActionState.State.SCHEDULED, secondaryAction.getActionState());

        // make sure primary action is finalised.
        primaryAction.waitForActionState(ActionState.State.FINISHED);
        assertEquals(0, primaryAction.getExecutionTime());
        assertTrue(Math.abs(primaryAction.getLifetime() - 500) < VALID_EXECUTION_VARIATION);
        assertEquals(false, primaryAction.isValid());
        assertEquals(true, primaryAction.isDone());
        assertEquals(false, primaryAction.isScheduled());

        // wait until old action is rescheduled to executing
        secondaryAction.waitForActionState(ActionState.State.EXECUTING);

        assertEquals(secondaryAction.getId(), colorableLightRemote.getActionList().get(0).getActionId());
        assertEquals(ActionState.State.EXECUTING, secondaryAction.getActionState());
        assertEquals(ActionState.State.FINISHED, primaryAction.getActionState());
        // validate that color value was set
        assertEquals(0.9d, colorableLightRemote.getBrightnessState().getBrightness(), 0.001);

        // cancel remaining action for the next test
        secondaryAction.cancel().get();
        secondaryAction.waitForActionState(ActionState.State.CANCELED);
    }

    /**
     * Test action extension..
     *
     * @throws Exception if an error occurs.
     */
    @Test(timeout = 5000)
    public void testActionExtension() throws Exception {
        LOGGER.info("testActionExtension");

        // set the brightness of the colorable light to 90
        final ActionParameter.Builder actionToExtendParameter = ActionParameter.newBuilder();
        actionToExtendParameter.setExecutionTimePeriod(TimeUnit.HOURS.toMicros(1));
        actionToExtendParameter.setPriority(Priority.LOW);
        actionToExtendParameter.setSchedulable(true);
        actionToExtendParameter.setInterruptible(true);
        final RemoteAction actionToExtend = observe(colorableLightRemote.setPowerState(State.ON, actionToExtendParameter.build()));
        actionToExtend.waitForActionState(ActionState.State.EXECUTING);

        assertEquals(actionToExtend.getId(), colorableLightRemote.getActionList().get(0).getActionId());
        assertEquals(ActionState.State.EXECUTING, actionToExtend.getActionState());

        // validate that power value was set
        assertEquals(State.ON, colorableLightRemote.getPowerState().getValue());

        assertEquals("last extension time was not initialized with the action creation time!", actionToExtend.getCreationTime(), actionToExtend.getLastExtensionTime());

        // make sure this works when using quantum computing
        Thread.sleep(1);
        actionToExtend.extend();
        actionToExtend.waitForExtension();
        assertNotEquals("last extension time was not updated!", actionToExtend.getCreationTime(), actionToExtend.getLastExtensionTime());

        // cancel remaining action for the next test
        actionToExtend.cancel().get();
        actionToExtend.waitForActionState(ActionState.State.CANCELED);
    }
}
