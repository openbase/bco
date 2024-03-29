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

import com.google.protobuf.Message;
import org.junit.jupiter.api.*;
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
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.session.TokenGenerator;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractUnitControllerTest extends AbstractBCODeviceManagerTest {

    private static final SessionManager sessionManager = new SessionManager();
    private static ColorableLightRemote colorableLightRemote;
    private static UnitController<?, ?> colorableLightController;
    private static AuthToken adminToken = null;

    public AbstractUnitControllerTest() {
    }

    @BeforeAll
    @Timeout(30)
    public static void loginUser() throws Throwable {
        colorableLightRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.COLORABLE_LIGHT), true, Units.COLORABLE_LIGHT);
        colorableLightController = deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(colorableLightRemote.getId());

        sessionManager.loginUser(Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_USER_ALIAS).getId(), UserCreationPlugin.ADMIN_PASSWORD, false);

        if (adminToken == null) {
            adminToken = TokenGenerator.generateAuthToken(sessionManager);
        }
    }

    @AfterAll
    @Timeout(30)
    public static void logoutUser() throws Throwable {
        sessionManager.logout();
    }

    @BeforeEach
    @Timeout(30)
    public void setupUnitController() throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException {
        for (ActionDescription actionDescription : colorableLightController.getActionList()) {

            // filter termination action
            if (actionDescription.getPriority() == Priority.TERMINATION) {
                continue;
            }

            final RemoteAction remoteAction = new RemoteAction(actionDescription);
            assertTrue(remoteAction.isDone(), "Found ongoing " + remoteAction + " on stack which could interfere with test execution!");
        }
    }

    @AfterEach
    @Timeout(30)
    public void tearDownUnitController() throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException {
        // cleanup leftover actions which were manually submitted to the controller.
        colorableLightController.cancelAllActions();
    }

    @Test
    @Timeout(30)
    public void applyDataStateUpdateTest() {
        try {
            colorableLightController.applyServiceState(States.Power.ON, ServiceType.POWER_STATE_SERVICE);
            assertEquals(State.ON, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue(), "Power state updated was not applied!");
            colorableLightRemote.requestData().get();
            assertEquals(State.ON, colorableLightRemote.getData().getPowerState().getValue(), "Power state updated was not applied to remote instance!");
            colorableLightController.applyServiceState(States.Power.OFF, ServiceType.POWER_STATE_SERVICE);
            assertEquals(State.OFF, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue(), "Power state updated was not applied because of: " + MultiLanguageTextProcessor.getBestMatch(((ColorableLightData) colorableLightController.getData()).getPowerState().getResponsibleAction().getDescription(), "?"));
            colorableLightRemote.requestData().get();
            assertEquals(State.OFF, colorableLightRemote.getData().getPowerState().getValue(), "Power state updated was not applied to remote instance!");

            colorableLightController.applyServiceState(States.Power.ON, ServiceType.POWER_STATE_SERVICE);
            assertEquals(State.ON, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue(), "Power state updated was not applied!");
            colorableLightRemote.requestData().get();
            assertEquals(State.ON, colorableLightRemote.getData().getPowerState().getValue(), "Power state updated was not applied to remote instance!");

            colorableLightController.applyServiceState(States.Power.OFF, ServiceType.POWER_STATE_SERVICE);
            assertEquals(State.OFF, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue(), "Power state updated was not applied!");
            colorableLightRemote.requestData().get();
            assertEquals(State.OFF, colorableLightRemote.getData().getPowerState().getValue(), "Power state updated was not applied to remote instance!");
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            ExceptionPrinter.printHistory(ex, System.err);
            assertTrue(false, "Error occurred during update!");
        }
    }

    @Test
    @Timeout(30)
    public void applyCustomDataStateUpdateTest() {
        try {
            for (int i = 0; i < 10; i++) {

                System.out.println("apply on " + i);
                colorableLightController.applyServiceState(States.Power.ON, ServiceType.POWER_STATE_SERVICE);
                assertEquals(State.ON, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue(), "Power state updated was not applied!");
                assertEquals(1.0, ((ColorableLightData) colorableLightController.getData()).getBrightnessState().getBrightness(), 0.0002, "Power state updated was not applied!");
                assertEquals(1.0, ((ColorableLightData) colorableLightController.getData()).getColorState().getColor().getHsbColor().getBrightness(), 0.0002, "Power state updated was not applied!");

                System.out.println("apply off " + i);
                colorableLightController.applyServiceState(States.Power.OFF, ServiceType.POWER_STATE_SERVICE);
                assertEquals(State.OFF, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
                assertEquals(0.0, ((ColorableLightData) colorableLightController.getData()).getBrightnessState().getBrightness(), 0.0002, "Power state updated was not applied!");
                assertEquals(0.0, ((ColorableLightData) colorableLightController.getData()).getColorState().getColor().getHsbColor().getBrightness(), 0.0002, "Power state updated was not applied!");

                System.out.println("apply green " + i);
                colorableLightController.applyServiceState(States.Color.GREEN, ServiceType.COLOR_STATE_SERVICE);
                assertEquals(State.ON, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
                assertEquals(1.0, ((ColorableLightData) colorableLightController.getData()).getBrightnessState().getBrightness(), 0.0002, "Power state updated was not applied!");
                assertEquals(Color.GREEN_VALUE, ((ColorableLightData) colorableLightController.getData()).getColorState().getColor());

                System.out.println("apply black " + i);
                colorableLightController.applyServiceState(Color.BLACK, ServiceType.COLOR_STATE_SERVICE);
                assertEquals(State.OFF, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
                assertEquals(0.0, ((ColorableLightData) colorableLightController.getData()).getBrightnessState().getBrightness(), 0.0002, "Power state updated was not applied!");
                assertEquals(Color.BLACK_VALUE, ((ColorableLightData) colorableLightController.getData()).getColorState().getColor());
            }

        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, System.err);
            assertTrue(false, "Error occurred during update!");
        }
    }

    @Test
    @Timeout(30)
    public void applyCustomDataStateFeedbackLoopTest() {
        try {
            colorableLightController.applyServiceState(Power.OFF, ServiceType.POWER_STATE_SERVICE);
            colorableLightController.applyServiceState(BrightnessState.newBuilder().setBrightness(0.0), ServiceType.BRIGHTNESS_STATE_SERVICE);

            RemoteAction action;

            colorableLightController.applyServiceState(Color.BLUE, ServiceType.COLOR_STATE_SERVICE);
            colorableLightController.applyServiceState(Power.OFF, ServiceType.POWER_STATE_SERVICE);

            action = waitForExecution(colorableLightRemote.setPowerState(Power.ON));
            assertEquals(ActionState.State.EXECUTING, action.getActionState(), "Action rejected by hardware feedback loop!");

            colorableLightController.applyServiceState(Color.BLUE, ServiceType.COLOR_STATE_SERVICE);
            colorableLightRemote.requestData().get();
            assertEquals(ActionState.State.EXECUTING, action.getActionState(), "Action rejected by power state feedback loop!");

            colorableLightController.applyServiceState(BrightnessState.newBuilder().setBrightness(1.0), ServiceType.BRIGHTNESS_STATE_SERVICE);
            colorableLightRemote.requestData().get();
            assertEquals(ActionState.State.EXECUTING, action.getActionState(), "Action rejected by brightness state feedback loop!");

            // perform inverse order

            colorableLightController.applyServiceState(Power.OFF, ServiceType.POWER_STATE_SERVICE);
            colorableLightController.applyServiceState(BrightnessState.newBuilder().setBrightness(0.0), ServiceType.BRIGHTNESS_STATE_SERVICE);

            action = waitForExecution(colorableLightRemote.setColorState(Color.GREEN));
            assertEquals(ActionState.State.EXECUTING, action.getActionState(), "Action rejected by hardware feedback loop!");

            colorableLightController.applyServiceState(Power.ON, ServiceType.POWER_STATE_SERVICE);
            colorableLightRemote.requestData().get();
            assertEquals(ActionState.State.EXECUTING, action.getActionState(), "Action rejected by power state feedback loop!");

            colorableLightController.applyServiceState(BrightnessState.newBuilder().setBrightness(1.0), ServiceType.BRIGHTNESS_STATE_SERVICE);
            colorableLightRemote.requestData().get();
            assertEquals(ActionState.State.EXECUTING, action.getActionState(), "Action rejected by brightness state feedback loop!");

        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            ExceptionPrinter.printHistory(ex, System.err);
            assertTrue(false, "Error occurred during update!");
        }
    }

    @Test
    @Timeout(30)
    public void rejectUpdateWhenStateIsCompatibleTest() {
        try {
            final RemoteAction mainAction = waitForExecution(colorableLightRemote.setColorState(Color.BLUE));
            assertTrue(colorableLightController.getActionList().get(0).getActionId().equals(mainAction.getId()), "Main action not on top!");
            assertEquals(ActionState.State.EXECUTING, mainAction.getActionState(), "Main action not executing!");

            // test compatible power state
            Message.Builder serviceStateBuilder = Power.ON.toBuilder();
            serviceStateBuilder = ActionDescriptionProcessor.generateAndSetResponsibleAction(serviceStateBuilder, ServiceType.POWER_STATE_SERVICE, colorableLightController, 30, TimeUnit.MINUTES, false, true, false, Priority.HIGH, ActionInitiator.newBuilder().setInitiatorType(InitiatorType.HUMAN).build());
            colorableLightController.applyDataUpdate(serviceStateBuilder, ServiceType.POWER_STATE_SERVICE);

            assertTrue(colorableLightController.getActionList().get(0).getActionId().equals(mainAction.getId()), "Too many actions on stack!");
            assertEquals(ActionState.State.EXECUTING, mainAction.getActionState(), "Main action not executing!");

            // test compatible color state
            serviceStateBuilder = Color.BLUE.toBuilder();
            serviceStateBuilder = ActionDescriptionProcessor.generateAndSetResponsibleAction(serviceStateBuilder, ServiceType.COLOR_STATE_SERVICE, colorableLightController, 30, TimeUnit.MINUTES, false, true, false, Priority.HIGH, ActionInitiator.newBuilder().setInitiatorType(InitiatorType.HUMAN).build());
            colorableLightController.applyDataUpdate(serviceStateBuilder, ServiceType.COLOR_STATE_SERVICE);

            assertTrue(colorableLightController.getActionList().get(0).getActionId().equals(mainAction.getId()), "Too many actions on stack!");
            assertEquals(ActionState.State.EXECUTING, mainAction.getActionState(), "Main action not executing!");

            // test compatible brightness state
            serviceStateBuilder = Brightness.MAX.toBuilder();
            serviceStateBuilder = ActionDescriptionProcessor.generateAndSetResponsibleAction(serviceStateBuilder, ServiceType.BRIGHTNESS_STATE_SERVICE, colorableLightController, 30, TimeUnit.MINUTES, false, true, false, Priority.HIGH, ActionInitiator.newBuilder().setInitiatorType(InitiatorType.HUMAN).build());
            colorableLightController.applyDataUpdate(serviceStateBuilder, ServiceType.BRIGHTNESS_STATE_SERVICE);

            assertTrue(colorableLightController.getActionList().get(0).getActionId().equals(mainAction.getId()), "Too many actions on stack!");
            assertEquals(ActionState.State.EXECUTING, mainAction.getActionState(), "Main action not executing!");

            // test nearly compatible color state
            ColorState.Builder colorServiceStateBuilder = Color.BLUE.toBuilder();
            colorServiceStateBuilder.getColorBuilder().getHsbColorBuilder().setHue(Color.BLUE.getColor().getHsbColor().getHue() + 0.0001);
            colorServiceStateBuilder.getColorBuilder().getHsbColorBuilder().setSaturation(Color.BLUE.getColor().getHsbColor().getSaturation() - 0.0001);
            colorServiceStateBuilder.getColorBuilder().getHsbColorBuilder().setBrightness(Color.BLUE.getColor().getHsbColor().getBrightness() - 0.0001);
            colorServiceStateBuilder = ActionDescriptionProcessor.generateAndSetResponsibleAction(colorServiceStateBuilder, ServiceType.COLOR_STATE_SERVICE, colorableLightController, 30, TimeUnit.MINUTES, false, true, false, Priority.HIGH, ActionInitiator.newBuilder().setInitiatorType(InitiatorType.HUMAN).build());
            colorableLightController.applyDataUpdate(colorServiceStateBuilder, ServiceType.COLOR_STATE_SERVICE);

            assertTrue(colorableLightController.getActionList().get(0).getActionId().equals(mainAction.getId()), "Too many actions on stack!");
            assertEquals(ActionState.State.EXECUTING, mainAction.getActionState(), "Main action not executing!");

        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, System.err);
            assertTrue(false, "Error occurred during update!");
        }
    }

    @Test
    @Timeout(30)
    public void futureSyncTest() throws InterruptedException, ExecutionException, TimeoutException, CouldNotPerformException {

        String anotherColorableLightId = null;
        for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.COLORABLE_LIGHT)) {
            if (!unitConfig.equals(colorableLightRemote.getId())) {
                anotherColorableLightId = unitConfig.getId();
                break;
            }
        }

        assertTrue(anotherColorableLightId != null, "No other colorable light found");

        CachedUnitRegistryRemote.shutdown();
        CachedUnitRegistryRemote.prepare();
        Units.getFutureUnit(anotherColorableLightId, true, Units.COLORABLE_LIGHT).get(10000, TimeUnit.MILLISECONDS).setColorState(Color.BLUE);
    }
}
