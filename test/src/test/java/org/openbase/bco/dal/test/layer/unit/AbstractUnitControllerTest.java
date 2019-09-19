package org.openbase.bco.dal.test.layer.unit;

import org.junit.*;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.state.States;
import org.openbase.bco.dal.lib.state.States.Color;
import org.openbase.bco.dal.lib.state.States.Power;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import org.openbase.type.vision.ColorType;

import java.util.concurrent.ExecutionException;

public class AbstractUnitControllerTest extends AbstractBCODeviceManagerTest {

    private static ColorableLightRemote colorableLightRemote;
    private static UnitController<?, ?> colorableLightController;

    public AbstractUnitControllerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();
        colorableLightRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.COLORABLE_LIGHT), true, Units.COLORABLE_LIGHT);
        colorableLightController = deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(colorableLightRemote.getId());
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void applyDataStateUpdateTest() {
        final PowerState powerStateOn = ColorableLightData.newBuilder().getPowerStateBuilder().setValue(State.ON).build();
        final PowerState powerStateOff = ColorableLightData.newBuilder().getPowerStateBuilder().setValue(State.OFF).build();
        try {

            colorableLightController.applyDataUpdate(States.Power.ON, ServiceType.POWER_STATE_SERVICE);
            Assert.assertEquals("Power state updated was not applied!", State.ON, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
            colorableLightRemote.requestData().get();
            Assert.assertEquals("Power state updated was not applied to remote instance!", State.ON, colorableLightRemote.getData().getPowerState().getValue());

            colorableLightController.applyDataUpdate(States.Power.OFF, ServiceType.POWER_STATE_SERVICE);
            Assert.assertEquals("Power state updated was not applied!", State.OFF, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());

            colorableLightController.applyDataUpdate(powerStateOff, ServiceType.POWER_STATE_SERVICE);
            Assert.assertEquals("Power state updated was not applied!", powerStateOff.getValue(), ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
            colorableLightRemote.requestData().get();
            Assert.assertEquals("Power state updated was not applied to remote instance!", State.ON, colorableLightRemote.getData().getPowerState().getValue());

        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            ExceptionPrinter.printHistory(ex, System.err);
            Assert.assertTrue("Error occured during update!", false);
        }
    }

    @Test
    public void applyCustomDataStateUpdateTest() {
        try {
            colorableLightController.applyDataUpdate(States.Power.ON, ServiceType.POWER_STATE_SERVICE);
            Assert.assertEquals("Power state updated was not applied!", State.ON, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
            Assert.assertEquals("Power state updated was not applied!", 1.0, ((ColorableLightData) colorableLightController.getData()).getBrightnessState().getBrightness(), 0.0001);
            Assert.assertEquals("Power state updated was not applied!", 1.0, ((ColorableLightData) colorableLightController.getData()).getColorState().getColor().getHsbColor().getBrightness(), 0.0001);

            colorableLightController.applyDataUpdate(States.Power.OFF, ServiceType.POWER_STATE_SERVICE);
            Assert.assertEquals("Power state updated was not applied!", State.OFF, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
            Assert.assertEquals("Power state updated was not applied!", 0.0, ((ColorableLightData) colorableLightController.getData()).getBrightnessState().getBrightness(), 0.0001);
            Assert.assertEquals("Power state updated was not applied!", 0.0, ((ColorableLightData) colorableLightController.getData()).getColorState().getColor().getHsbColor().getBrightness(), 0.0001);

            colorableLightController.applyDataUpdate(States.Color.GREEN, ServiceType.COLOR_STATE_SERVICE);
            Assert.assertEquals("Power state updated was not applied!", State.ON, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
            Assert.assertEquals("Power state updated was not applied!", 1.0, ((ColorableLightData) colorableLightController.getData()).getBrightnessState().getBrightness(), 0.0001);
            Assert.assertEquals("Power state updated was not applied!", Color.GREEN_VALUE, ((ColorableLightData) colorableLightController.getData()).getColorState().getColor());

            colorableLightController.applyDataUpdate(Color.BLACK, ServiceType.COLOR_STATE_SERVICE);
            Assert.assertEquals("Power state updated was not applied!", State.OFF, ((ColorableLightData) colorableLightController.getData()).getPowerState().getValue());
            Assert.assertEquals("Power state updated was not applied!", 0.0, ((ColorableLightData) colorableLightController.getData()).getBrightnessState().getBrightness(), 0.0001);
            Assert.assertEquals("Power state updated was not applied!", Color.BLACK_VALUE, ((ColorableLightData) colorableLightController.getData()).getColorState().getColor());

        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, System.err);
            Assert.assertTrue("Error occurred during update!", false);
        }
    }

    @Test
    public void applyCustomDataStateFeedbackLoopTest() {
        try {
            colorableLightController.applyDataUpdate(Power.OFF, ServiceType.POWER_STATE_SERVICE);
            colorableLightController.applyDataUpdate(BrightnessState.newBuilder().setBrightness(0.0), ServiceType.BRIGHTNESS_STATE_SERVICE);

            RemoteAction action;

            action = new RemoteAction(colorableLightRemote.setPowerState(Power.ON));
            action.waitForSubmission();
            Assert.assertEquals("Action rejected by hardware feedback loop!", ActionState.State.EXECUTING, action.getActionState());

            colorableLightController.applyDataUpdate(Color.BLUE, ServiceType.COLOR_STATE_SERVICE);
            colorableLightRemote.requestData().get();
            Assert.assertEquals("Action rejected by power state feedback loop!", ActionState.State.EXECUTING, action.getActionState());

            colorableLightController.applyDataUpdate(BrightnessState.newBuilder().setBrightness(1.0), ServiceType.BRIGHTNESS_STATE_SERVICE);
            colorableLightRemote.requestData().get();
            Assert.assertEquals("Action rejected by brightness state feedback loop!", ActionState.State.EXECUTING, action.getActionState());

            // perform inverse order

            colorableLightController.applyDataUpdate(Power.OFF, ServiceType.POWER_STATE_SERVICE);
            colorableLightController.applyDataUpdate(BrightnessState.newBuilder().setBrightness(0.0), ServiceType.BRIGHTNESS_STATE_SERVICE);

            action = new RemoteAction(colorableLightRemote.setColorState(Color.GREEN));
            action.waitForSubmission();
            Assert.assertEquals("Action rejected by hardware feedback loop!", ActionState.State.EXECUTING, action.getActionState());

            colorableLightController.applyDataUpdate(Power.ON, ServiceType.POWER_STATE_SERVICE);
            colorableLightRemote.requestData().get();
            Assert.assertEquals("Action rejected by power state feedback loop!", ActionState.State.EXECUTING, action.getActionState());

            colorableLightController.applyDataUpdate(BrightnessState.newBuilder().setBrightness(1.0), ServiceType.BRIGHTNESS_STATE_SERVICE);
            colorableLightRemote.requestData().get();
            Assert.assertEquals("Action rejected by brightness state feedback loop!", ActionState.State.EXECUTING, action.getActionState());

        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            ExceptionPrinter.printHistory(ex, System.err);
            Assert.assertTrue("Error occurred during update!", false);
        }
    }
}
