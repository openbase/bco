package org.openbase.bco.dal.test.layer.unit;

/*-
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import com.google.protobuf.Message;
import org.junit.*;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.state.States;
import org.openbase.bco.dal.lib.state.States.Brightness;
import org.openbase.bco.dal.lib.state.States.Color;
import org.openbase.bco.dal.lib.state.States.Power;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.session.TokenGenerator;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AbstractUnitControllerTest extends AbstractBCODeviceManagerTest {

    private static ColorableLightRemote colorableLightRemote;
    private static UnitController<?, ?> colorableLightController;
    private static final SessionManager sessionManager = new SessionManager();
    private static AuthToken adminToken = null;

    public AbstractUnitControllerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();
        colorableLightRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.COLORABLE_LIGHT), true, Units.COLORABLE_LIGHT);
        colorableLightController = deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(colorableLightRemote.getId());

        sessionManager.loginUser(Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_USER_ALIAS).getId(), UserCreationPlugin.ADMIN_PASSWORD, false);

        if (adminToken == null) {
            adminToken = TokenGenerator.generateAuthToken(sessionManager);
        }
    }
    @AfterClass
    public static void tearDownClass() throws Throwable {
        sessionManager.logout();
        AbstractBCODeviceManagerTest.tearDownClass();
    }

    @Before
    public void setUp() throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException {
        for (ActionDescription actionDescription : colorableLightController.getActionList()) {
            final RemoteAction remoteAction = new RemoteAction(actionDescription);
            Assert.assertTrue("Found ongoing " + remoteAction + " on stack which could interfere with test execution!", remoteAction.isDone());
        }
    }

    @After
    public void tearDown() throws NotAvailableException, InterruptedException, InstantiationException, TimeoutException, ExecutionException {
        // cleanup leftover actions which were manually submitted to the controller.
        for (ActionDescription actionDescription : colorableLightController.getActionList()) {
            new RemoteAction(actionDescription, adminToken).cancel().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void applyDataStateUpdateTest() {
        try {
            colorableLightController.applyServiceState(States.Power.ON, ServiceType.POWER_STATE_SERVICE);
            Assert.assertEquals("Power state updated was not applied!", State.ON, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
            colorableLightRemote.requestData().get();
            Assert.assertEquals("Power state updated was not applied to remote instance!", State.ON, colorableLightRemote.getData().getPowerState().getValue());

            colorableLightController.applyServiceState(States.Power.OFF, ServiceType.POWER_STATE_SERVICE);
            Assert.assertEquals("Power state updated was not applied!", State.OFF, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
            colorableLightRemote.requestData().get();
            Assert.assertEquals("Power state updated was not applied to remote instance!", State.OFF, colorableLightRemote.getData().getPowerState().getValue());

            colorableLightController.applyServiceState(States.Power.ON, ServiceType.POWER_STATE_SERVICE);
            Assert.assertEquals("Power state updated was not applied!", State.ON, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
            colorableLightRemote.requestData().get();
            Assert.assertEquals("Power state updated was not applied to remote instance!", State.ON, colorableLightRemote.getData().getPowerState().getValue());

            colorableLightController.applyServiceState(States.Power.OFF, ServiceType.POWER_STATE_SERVICE);
            Assert.assertEquals("Power state updated was not applied!", State.OFF, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
            colorableLightRemote.requestData().get();
            Assert.assertEquals("Power state updated was not applied to remote instance!", State.OFF, colorableLightRemote.getData().getPowerState().getValue());
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            ExceptionPrinter.printHistory(ex, System.err);
            Assert.assertTrue("Error occurred during update!", false);
        }
    }

    @Test
    public void applyCustomDataStateUpdateTest() {
        try {
            for (int i = 0; i < 10; i++) {

                System.out.println("apply on " + i);
                colorableLightController.applyServiceState(States.Power.ON, ServiceType.POWER_STATE_SERVICE);
                Assert.assertEquals("Power state updated was not applied!", State.ON, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
                Assert.assertEquals("Power state updated was not applied!", 1.0, ((ColorableLightData) colorableLightController.getData()).getBrightnessState().getBrightness(), 0.0001);
                Assert.assertEquals("Power state updated was not applied!", 1.0, ((ColorableLightData) colorableLightController.getData()).getColorState().getColor().getHsbColor().getBrightness(), 0.0001);

                System.out.println("apply off " + i);
                colorableLightController.applyServiceState(States.Power.OFF, ServiceType.POWER_STATE_SERVICE);
                Assert.assertEquals("Power state updated was not applied!", State.OFF, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
                Assert.assertEquals("Power state updated was not applied!", 0.0, ((ColorableLightData) colorableLightController.getData()).getBrightnessState().getBrightness(), 0.0001);
                Assert.assertEquals("Power state updated was not applied!", 0.0, ((ColorableLightData) colorableLightController.getData()).getColorState().getColor().getHsbColor().getBrightness(), 0.0001);

                System.out.println("apply green " + i);
                colorableLightController.applyServiceState(States.Color.GREEN, ServiceType.COLOR_STATE_SERVICE);
                Assert.assertEquals("Power state updated was not applied!", State.ON, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
                Assert.assertEquals("Power state updated was not applied!", 1.0, ((ColorableLightData) colorableLightController.getData()).getBrightnessState().getBrightness(), 0.0001);
                Assert.assertEquals("Power state updated was not applied!", Color.GREEN_VALUE, ((ColorableLightData) colorableLightController.getData()).getColorState().getColor());

                System.out.println("apply black " + i);
                colorableLightController.applyServiceState(Color.BLACK, ServiceType.COLOR_STATE_SERVICE);
                Assert.assertEquals("Power state updated was not applied!", State.OFF, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
                Assert.assertEquals("Power state updated was not applied!", 0.0, ((ColorableLightData) colorableLightController.getData()).getBrightnessState().getBrightness(), 0.0001);
                Assert.assertEquals("Power state updated was not applied!", Color.BLACK_VALUE, ((ColorableLightData) colorableLightController.getData()).getColorState().getColor());
            }

        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, System.err);
            Assert.assertTrue("Error occurred during update!", false);
        }
    }

    @Test
    public void applyCustomDataStateFeedbackLoopTest() {
        try {
            colorableLightController.applyServiceState(Power.OFF, ServiceType.POWER_STATE_SERVICE);
            colorableLightController.applyServiceState(BrightnessState.newBuilder().setBrightness(0.0), ServiceType.BRIGHTNESS_STATE_SERVICE);

            RemoteAction action;

            colorableLightController.applyServiceState(Color.BLUE, ServiceType.COLOR_STATE_SERVICE);
            colorableLightController.applyServiceState(Power.OFF, ServiceType.POWER_STATE_SERVICE);

            action = waitForExecution(colorableLightRemote.setPowerState(Power.ON));
            Assert.assertEquals("Action rejected by hardware feedback loop!", ActionState.State.EXECUTING, action.getActionState());

            colorableLightController.applyServiceState(Color.BLUE, ServiceType.COLOR_STATE_SERVICE);
            colorableLightRemote.requestData().get();
            Assert.assertEquals("Action rejected by power state feedback loop!", ActionState.State.EXECUTING, action.getActionState());

            colorableLightController.applyServiceState(BrightnessState.newBuilder().setBrightness(1.0), ServiceType.BRIGHTNESS_STATE_SERVICE);
            colorableLightRemote.requestData().get();
            Assert.assertEquals("Action rejected by brightness state feedback loop!", ActionState.State.EXECUTING, action.getActionState());

            // perform inverse order

            colorableLightController.applyServiceState(Power.OFF, ServiceType.POWER_STATE_SERVICE);
            colorableLightController.applyServiceState(BrightnessState.newBuilder().setBrightness(0.0), ServiceType.BRIGHTNESS_STATE_SERVICE);

            action = waitForExecution(colorableLightRemote.setColorState(Color.GREEN));
            Assert.assertEquals("Action rejected by hardware feedback loop!", ActionState.State.EXECUTING, action.getActionState());

            colorableLightController.applyServiceState(Power.ON, ServiceType.POWER_STATE_SERVICE);
            colorableLightRemote.requestData().get();
            Assert.assertEquals("Action rejected by power state feedback loop!", ActionState.State.EXECUTING, action.getActionState());

            colorableLightController.applyServiceState(BrightnessState.newBuilder().setBrightness(1.0), ServiceType.BRIGHTNESS_STATE_SERVICE);
            colorableLightRemote.requestData().get();
            Assert.assertEquals("Action rejected by brightness state feedback loop!", ActionState.State.EXECUTING, action.getActionState());

        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            ExceptionPrinter.printHistory(ex, System.err);
            Assert.assertTrue("Error occurred during update!", false);
        }
    }

    @Test
    public void rejectUpdateWhenStateIsCompatibleTest() {
        try {
            final RemoteAction mainAction = waitForExecution(colorableLightRemote.setColorState(Color.BLUE));
            Assert.assertTrue("Main action not on top!", colorableLightController.getActionList().get(0).getActionId().equals(mainAction.getId()));
            Assert.assertEquals("Main action not executing!", ActionState.State.EXECUTING, mainAction.getActionState());

            // test compatible power state
            Message.Builder serviceStateBuilder = Power.ON.toBuilder();
            serviceStateBuilder = ActionDescriptionProcessor.generateAndSetResponsibleAction(serviceStateBuilder, ServiceType.POWER_STATE_SERVICE, colorableLightController, 30, TimeUnit.MINUTES, false, true, false, Priority.HIGH, ActionInitiator.newBuilder().setInitiatorType(InitiatorType.HUMAN).build());
            colorableLightController.applyDataUpdate(serviceStateBuilder, ServiceType.POWER_STATE_SERVICE);

            Assert.assertTrue("Too many actions on stack!", colorableLightController.getActionList().get(0).getActionId().equals(mainAction.getId()));
            Assert.assertEquals("Main action not executing!", ActionState.State.EXECUTING, mainAction.getActionState());

            // test compatible color state
            serviceStateBuilder = Color.BLUE.toBuilder();
            serviceStateBuilder = ActionDescriptionProcessor.generateAndSetResponsibleAction(serviceStateBuilder, ServiceType.COLOR_STATE_SERVICE, colorableLightController, 30, TimeUnit.MINUTES, false, true, false, Priority.HIGH, ActionInitiator.newBuilder().setInitiatorType(InitiatorType.HUMAN).build());
            colorableLightController.applyDataUpdate(serviceStateBuilder, ServiceType.COLOR_STATE_SERVICE);

            Assert.assertTrue("Too many actions on stack!", colorableLightController.getActionList().get(0).getActionId().equals(mainAction.getId()));
            Assert.assertEquals("Main action not executing!", ActionState.State.EXECUTING, mainAction.getActionState());

            // test compatible brightness state
            serviceStateBuilder = Brightness.MAX.toBuilder();
            serviceStateBuilder = ActionDescriptionProcessor.generateAndSetResponsibleAction(serviceStateBuilder, ServiceType.BRIGHTNESS_STATE_SERVICE, colorableLightController, 30, TimeUnit.MINUTES, false, true, false, Priority.HIGH, ActionInitiator.newBuilder().setInitiatorType(InitiatorType.HUMAN).build());
            colorableLightController.applyDataUpdate(serviceStateBuilder, ServiceType.BRIGHTNESS_STATE_SERVICE);

            Assert.assertTrue("Too many actions on stack!", colorableLightController.getActionList().get(0).getActionId().equals(mainAction.getId()));
            Assert.assertEquals("Main action not executing!", ActionState.State.EXECUTING, mainAction.getActionState());

            // test nearly compatible color state
            ColorState.Builder colorServiceStateBuilder = Color.BLUE.toBuilder();
            colorServiceStateBuilder.getColorBuilder().getHsbColorBuilder().setHue(Color.BLUE.getColor().getHsbColor().getHue() + 0.0001);
            colorServiceStateBuilder.getColorBuilder().getHsbColorBuilder().setSaturation(Color.BLUE.getColor().getHsbColor().getSaturation() - 0.0001);
            colorServiceStateBuilder.getColorBuilder().getHsbColorBuilder().setBrightness(Color.BLUE.getColor().getHsbColor().getBrightness() - 0.0001);
            colorServiceStateBuilder = ActionDescriptionProcessor.generateAndSetResponsibleAction(colorServiceStateBuilder, ServiceType.COLOR_STATE_SERVICE, colorableLightController, 30, TimeUnit.MINUTES, false, true, false, Priority.HIGH, ActionInitiator.newBuilder().setInitiatorType(InitiatorType.HUMAN).build());
            colorableLightController.applyDataUpdate(colorServiceStateBuilder, ServiceType.COLOR_STATE_SERVICE);

            Assert.assertTrue("Too many actions on stack!", colorableLightController.getActionList().get(0).getActionId().equals(mainAction.getId()));
            Assert.assertEquals("Main action not executing!", ActionState.State.EXECUTING, mainAction.getActionState());

        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, System.err);
            Assert.assertTrue("Error occurred during update!", false);
        }
    }

    @Test
    public void futureSyncTest() throws InterruptedException, ExecutionException, TimeoutException, CouldNotPerformException {

        String anotherColorableLightId = null;
        for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.COLORABLE_LIGHT)) {
            if (!unitConfig.equals(colorableLightRemote.getId())) {
                anotherColorableLightId = unitConfig.getId();
                break;
            }
        }

        Assert.assertTrue("No other colorable light found", anotherColorableLightId != null);

        CachedUnitRegistryRemote.shutdown();
        CachedUnitRegistryRemote.prepare();
        Units.getFutureUnit(anotherColorableLightId, true, Units.COLORABLE_LIGHT).get(10000, TimeUnit.MILLISECONDS).setColorState(Color.BLUE);
    }
}
