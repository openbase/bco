package org.openbase.bco.dal.test.layer.unit.location;

/*
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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.dal.control.layer.unit.*;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.remote.action.Actions;
import org.openbase.bco.dal.remote.detector.PresenceDetector;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionReferenceType;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActionStateType;
import org.openbase.type.domotic.state.BlindStateType.BlindState;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState;
import org.openbase.type.domotic.state.MotionStateType.MotionState;
import org.openbase.type.domotic.state.PowerConsumptionStateType.PowerConsumptionState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;
import org.openbase.type.vision.ColorType.Color;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationRemoteTest extends AbstractBCOLocationManagerTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LocationRemoteTest.class);

    private static LocationRemote locationRemote;

    public LocationRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            AbstractBCOLocationManagerTest.setUpClass();
            locationRemote = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    /**
     * Test if changes in locations are published to underlying units.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testLocationToUnitPipeline() throws Exception {
        System.out.println("testLocationToUnitPipeline");

        try {
            List<PowerStateOperationService> powerServiceList = new ArrayList<>();
            for (UnitConfig dalUnitConfig : Registries.getUnitRegistry().getDalUnitConfigs()) {
                if (dalUnitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    continue;
                }

                if (unitHasService(dalUnitConfig, ServiceType.POWER_STATE_SERVICE, ServicePattern.OPERATION)) {
                    UnitController unitController = deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dalUnitConfig.getId());
                    powerServiceList.add((PowerStateOperationService) unitController);
                }
            }

            PowerState powerOn = PowerState.newBuilder().setValue(PowerState.State.ON).build();
            PowerState powerOff = PowerState.newBuilder().setValue(PowerState.State.OFF).build();

            Actions.waitForExecution(locationRemote.setPowerState(powerOn));
            for (PowerStateOperationService powerStateService : powerServiceList) {
                assertEquals("PowerState of unit [" + ((UnitController) powerStateService).getLabel() + "] has not been updated by the locationRemote!", powerOn.getValue(), powerStateService.getPowerState().getValue());
            }

            Actions.waitForExecution(locationRemote.setPowerState(powerOff));
            for (PowerStateOperationService powerStateService : powerServiceList) {
                assertEquals("PowerState of unit [" + ((UnitController) powerStateService).getLabel() + "] has not been updated by the locationRemote!", powerOff.getValue(), powerStateService.getPowerState().getValue());
            }
        } catch (CouldNotPerformException | InterruptedException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    private boolean unitHasService(UnitConfig unitConfig, ServiceType serviceType, ServicePattern servicePattern) throws CouldNotPerformException {
        for (ServiceDescription serviceDescription : Registries.getTemplateRegistry().getUnitTemplateByType(unitConfig.getUnitType()).getServiceDescriptionList()) {
            if (serviceDescription.getServiceType() == serviceType && serviceDescription.getPattern() == servicePattern) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test if changes in unitControllers are published to a location remote.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testUnitToLocationPipeline() throws Exception {
        System.out.println("testUnitToLocationPipeline");

        List<TemperatureSensorController> temperatureSensorList = new ArrayList<>();
        List<TemperatureControllerController> temperatureControllerList = new ArrayList<>();
        List<PowerConsumptionSensorController> powerConsumptionSensorList = new ArrayList<>();
        for (UnitConfig dalUnitConfig : Registries.getUnitRegistry().getDalUnitConfigs()) {
            if (dalUnitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                continue;
            }

            UnitController unitController = deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dalUnitConfig.getId());
            if (unitController instanceof TemperatureSensorController) {
                temperatureSensorList.add((TemperatureSensorController) unitController);
            } else if (unitController instanceof TemperatureControllerController) {
                temperatureControllerList.add((TemperatureControllerController) unitController);
            } else if (unitController instanceof PowerConsumptionSensorController) {
                powerConsumptionSensorList.add((PowerConsumptionSensorController) unitController);
            }
        }

        double temperature = 18;
        final TemperatureState temperatureState = TemperatureState.newBuilder().setTemperature(temperature).setTimestamp(TimestampProcessor.getCurrentTimestamp()).build();
        for (TemperatureSensorController temperatureSensor : temperatureSensorList) {
            temperatureSensor.applyDataUpdate(temperatureState, ServiceType.TEMPERATURE_STATE_SERVICE);
        }
        double targetTemperature = 21;
        final TemperatureState.Builder targetTemperatureStateBuilder = TemperatureState.newBuilder().setTemperature(targetTemperature).setTimestamp(TimestampProcessor.getCurrentTimestamp());
        for (TemperatureControllerController temperatureController : temperatureControllerList) {
            temperatureController.applyDataUpdate(temperatureState, ServiceType.TEMPERATURE_STATE_SERVICE);

            final ActionParameter.Builder actionParameter = ActionDescriptionProcessor.generateDefaultActionParameter(targetTemperatureStateBuilder.build(), ServiceType.TARGET_TEMPERATURE_STATE_SERVICE, temperatureController);
            actionParameter.setInterruptible(false);
            actionParameter.setSchedulable(false);
            actionParameter.setExecutionTimePeriod(TimeUnit.MINUTES.toMicros(15));
            targetTemperatureStateBuilder.setResponsibleAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(actionParameter));
            final TemperatureState targetTemperatureState = targetTemperatureStateBuilder.build();
            temperatureController.applyDataUpdate(targetTemperatureState, ServiceType.TARGET_TEMPERATURE_STATE_SERVICE);
        }
        locationRemote.ping().get();
        while (locationRemote.getTemperatureState().getTemperature() != temperature) {
            System.out.println("current temp: " + locationRemote.getTemperatureState().getTemperature() + " waiting for: " + temperature);
            Thread.sleep(10);
        }
        assertEquals("Temperature of the location has not been updated!", temperature, locationRemote.getTemperatureState().getTemperature(), 0.01);
        while (locationRemote.getTargetTemperatureState().getTemperature() != targetTemperature) {
            System.out.println("current target temp: " + locationRemote.getTargetTemperatureState().getTemperature() + " waiting for: " + targetTemperature);
            Thread.sleep(10);
        }
        assertEquals("TargetTemperature of the location has not been updated!", targetTemperature, locationRemote.getTargetTemperatureState().getTemperature(), 0.01);

        System.out.println("PowerConsumptionSensors: " + powerConsumptionSensorList.size());
        PowerConsumptionState powerConsumptionState = PowerConsumptionState.newBuilder().setVoltage(240).setConsumption(10).setCurrent(1).setTimestamp(TimestampProcessor.getCurrentTimestamp()).build();
        for (PowerConsumptionSensorController powerConsumptionSensor : powerConsumptionSensorList) {
            powerConsumptionSensor.applyDataUpdate(powerConsumptionState, ServiceType.POWER_CONSUMPTION_STATE_SERVICE);
            System.out.println("Updated powerConsumptionState of [" + powerConsumptionSensor.toString() + "] to [" + powerConsumptionSensor.getPowerConsumptionState() + "]");
        }

        while (locationRemote.getPowerConsumptionState().getCurrent() != powerConsumptionState.getCurrent()) {
            System.out.println("Waiting for locationRemote powerConsumptionState update!");
            Thread.sleep(10);
        }
        assertEquals("Voltage of location has not been updated!", powerConsumptionState.getVoltage(), locationRemote.getPowerConsumptionState().getVoltage(), 0.01);
        assertEquals("Current of location has not been updated!", powerConsumptionState.getCurrent(), locationRemote.getPowerConsumptionState().getCurrent(), 0.01);
        assertEquals("Consumption of location has not been updated!", powerConsumptionState.getConsumption() * powerConsumptionSensorList.size(), locationRemote.getPowerConsumptionState().getConsumption(), 0.01);
    }

    @Test(timeout = 15000)
    public void testRecordAndRestoreSnapshots() throws Exception {
        BlindState snapshotBlindState = BlindState.newBuilder().setValue(BlindState.State.DOWN).setOpeningRatio(0).build();
        BlindState newBlindState = BlindState.newBuilder().setValue(BlindState.State.UP).setOpeningRatio(0.5).build();
        ColorState snapshotColorState = ColorState.newBuilder().setColor(Color.newBuilder().setType(Color.Type.HSB).setHsbColor(HSBColor.newBuilder().setBrightness(1).setHue(0).setSaturation(1))).build();
        ColorState newColorState = ColorState.newBuilder().setColor(Color.newBuilder().setType(Color.Type.HSB).setHsbColor(HSBColor.newBuilder().setBrightness(0.2).setHue(100).setSaturation(0.5))).build();
        PowerState snapshotPowerState = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        PowerState newPowerState = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        TemperatureState snapshotTemperatureState = TemperatureState.newBuilder().setTemperature(20).setTemperatureDataUnit(TemperatureState.DataUnit.CELSIUS).build();
        TemperatureState newTemperatureState = TemperatureState.newBuilder().setTemperature(22).setTemperatureDataUnit(TemperatureState.DataUnit.CELSIUS).build();

        logger.info("Set initial states");

        Actions.waitForExecution(locationRemote.setBlindState(snapshotBlindState));
        Actions.waitForExecution(locationRemote.setColorState(snapshotColorState));
        Actions.waitForExecution(locationRemote.setPowerState(snapshotPowerState));
        Actions.waitForExecution(locationRemote.setTargetTemperatureState(snapshotTemperatureState));

        logger.info("Record snapshot");

        Snapshot snapshot = locationRemote.recordSnapshot().get();

        // save location state
        snapshotBlindState = locationRemote.getBlindState(UnitType.UNKNOWN);
        snapshotColorState = locationRemote.getColorState(UnitType.UNKNOWN);
        snapshotPowerState = locationRemote.getPowerState(UnitType.UNKNOWN);
        snapshotTemperatureState = locationRemote.getTargetTemperatureState(UnitType.UNKNOWN);

        logger.info("Change states");

        Actions.waitForExecution(locationRemote.setBlindState(newBlindState));
        Actions.waitForExecution(locationRemote.setColorState(newColorState));
        Actions.waitForExecution(locationRemote.setPowerState(newPowerState));
        Actions.waitForExecution(locationRemote.setTargetTemperatureState(newTemperatureState));

        assertTrue("BlindState of location has not changed!", locationRemote.getBlindState(UnitType.UNKNOWN).getValue() != snapshotBlindState.getValue());
        assertTrue("ColorState of location has not changed!", !locationRemote.getColorState(UnitType.UNKNOWN).getColor().getHsbColor().equals(snapshotColorState.getColor().getHsbColor()));
        assertTrue("PowerState of location has not changed!", locationRemote.getPowerState(UnitType.UNKNOWN).getValue() != snapshotPowerState.getValue());
        assertTrue("TargetTemperatureState of location has not changed!", locationRemote.getTargetTemperatureState(UnitType.UNKNOWN).getTemperature() != snapshotTemperatureState.getTemperature());

        logger.info("Restore snapshot");

        locationRemote.restoreSnapshot(snapshot).get(10, TimeUnit.SECONDS);
        locationRemote.requestData().get(10, TimeUnit.SECONDS);

        final BlindState.State blindStateValue = snapshotBlindState.getValue();
        final HSBColor hsbColor = snapshotColorState.getColor().getHsbColor();
        final PowerState.State powerStateValue = snapshotPowerState.getValue();
        final double targetTemperature = snapshotTemperatureState.getTemperature();
        final UnitStateAwaiter<LocationData, LocationRemote> stateAwaiter = new UnitStateAwaiter<>(locationRemote);
        logger.warn("Wait for blindState");
        stateAwaiter.waitForState(data -> blindStateValue == data.getBlindState().getValue());
        logger.warn("Wait for Color");
        stateAwaiter.waitForState(data -> hsbColor.equals(data.getColorState().getColor().getHsbColor()));
        logger.warn("Wait for power");
        stateAwaiter.waitForState(data -> powerStateValue == data.getPowerState().getValue());
        logger.warn("Wait for target temperature");
        stateAwaiter.waitForState(data -> {
            logger.warn("Expected {} but is {}", targetTemperature, data.getTargetTemperatureState().getTemperature());
            return targetTemperature == data.getTargetTemperatureState().getTemperature();
        });

        assertEquals("BlindState of location has not been restored through snapshot!", snapshotBlindState.getValue(), locationRemote.getBlindState(UnitType.UNKNOWN).getValue());
        assertEquals("ColorState of location has not been restored through snapshot!", snapshotColorState.getColor().getHsbColor(), locationRemote.getColorState(UnitType.UNKNOWN).getColor().getHsbColor());
        assertEquals("PowerState of location has not been restored through snapshot!", snapshotPowerState.getValue(), locationRemote.getPowerState(UnitType.UNKNOWN).getValue());
        assertEquals("TargetTemperatureState of location has not been restored through snapshot!", snapshotTemperatureState.getTemperature(), locationRemote.getTargetTemperatureState(UnitType.UNKNOWN).getTemperature(), 0.5);
    }

    @Test(timeout = 5000)
    public void testManipulatingByUnitType() throws Exception {
        System.out.println("testManipulatingByUnitType");

        try {
            List<UnitType> lightTypes = Registries.getTemplateRegistry().getSubUnitTypes(UnitType.LIGHT);
            lightTypes.add(UnitType.LIGHT);
            assertTrue("UnitType not found as subType of LIGHT!", lightTypes.contains(UnitType.LIGHT));
            assertTrue("UnitType not found as subType of LIGHT!", lightTypes.contains(UnitType.DIMMABLE_LIGHT));
            assertTrue("UnitType not found as subType of LIGHT!", lightTypes.contains(UnitType.COLORABLE_LIGHT));

            List<PowerStateOperationService> powerServiceList = new ArrayList<>();
            for (UnitConfig dalUnitConfig : Registries.getUnitRegistry().getDalUnitConfigs()) {
                if (dalUnitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    continue;
                }

                if (lightTypes.contains(dalUnitConfig.getUnitType())) {
                    UnitController unitController = deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dalUnitConfig.getId());
                    powerServiceList.add((PowerStateOperationService) unitController);
                }
            }

            PowerState powerOn = PowerState.newBuilder().setValue(PowerState.State.ON).build();
            PowerState powerOff = PowerState.newBuilder().setValue(PowerState.State.OFF).build();

            Actions.waitForExecution(locationRemote.setPowerState(powerOn, UnitType.LIGHT));
            for (PowerStateOperationService powerStateService : powerServiceList) {
                assertEquals("PowerState of lightUnit [" + ((UnitController) powerStateService).getLabel() + "] has not been updated by the loationRemote!", powerOn.getValue(), powerStateService.getPowerState().getValue());
            }

            Actions.waitForExecution(locationRemote.setPowerState(powerOff));
            for (PowerStateOperationService powerStateService : powerServiceList) {
                assertEquals("PowerState of lightUnit [" + ((UnitController) powerStateService).getLabel() + "] has not been updated by the loationRemote!", powerOff.getValue(), powerStateService.getPowerState().getValue());
            }

            for (PowerStateOperationService powerStateOperationService : powerServiceList) {
                powerStateOperationService.setPowerState(powerOn).get();
            }
            while (locationRemote.getPowerState(UnitType.LIGHT).getValue() != powerOn.getValue()) {
                System.out.println("Waiting for locationRemote update!");
                Thread.sleep(10);
            }
            assertEquals("PowerState of location has not been updated!", powerOn.getValue(), locationRemote.getPowerState(UnitType.LIGHT).getValue());

            for (PowerStateOperationService powerStateOperationService : powerServiceList) {
                powerStateOperationService.setPowerState(powerOff).get();
            }

            while (locationRemote.getPowerState(UnitType.LIGHT).getValue() != powerOff.getValue()) {
                System.out.println("Waiting for locationRemote update!");
                Thread.sleep(10);
            }
            assertEquals("PowerState of location has not been updated!", powerOff.getValue(), locationRemote.getPowerState(UnitType.LIGHT).getValue());
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    @Test(timeout = 5000)
    public void testPresenceState() throws Exception {
        System.out.println("testPresenceState");

        try {
            MotionDetectorController motionDetectorController = null;
            for (UnitConfig dalUnitConfig : Registries.getUnitRegistry().getDalUnitConfigs()) {
                if (dalUnitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    continue;
                }

                UnitController unitController = deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dalUnitConfig.getId());
                if (unitController instanceof MotionDetectorController) {
                    motionDetectorController = (MotionDetectorController) unitController;
                }
            }

            if (motionDetectorController == null) {
                Assert.fail("Mock registry does not contain a motionDetector!");
                return;
            }

            motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(MotionState.newBuilder().setValue(MotionState.State.MOTION).build()), ServiceType.MOTION_STATE_SERVICE);

            while (locationRemote.getPresenceState().getValue() != PresenceState.State.PRESENT) {
                System.out.println("Waiting for locationRemote presenceState update!");
                Thread.sleep(10);
            }
            assertEquals("PresenceState of location has not been updated!", PresenceState.State.PRESENT, locationRemote.getPresenceState().getValue());

            motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(MotionState.newBuilder().setValue(MotionState.State.NO_MOTION).build()), ServiceType.MOTION_STATE_SERVICE);

            Thread.sleep(PresenceDetector.PRESENCE_TIMEOUT);
            while (locationRemote.getPresenceState().getValue() != PresenceState.State.ABSENT) {
                System.out.println("Waiting for locationRemote presenceState update!");
                Thread.sleep(10);
            }
            assertEquals("PresenceState of location has not been updated!", PresenceState.State.ABSENT, locationRemote.getPresenceState().getValue());
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    @Test(timeout = 5000)
    public void testIlluminanceState() throws Exception {
        System.out.println("testIlluminanceState");

        try {
            List<LightSensorController> lightSensorControllerList = new ArrayList<>();
            for (UnitConfig dalUnitConfig : Registries.getUnitRegistry().getDalUnitConfigs()) {
                if (dalUnitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    continue;
                }

                UnitController unitController = deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dalUnitConfig.getId());
                if (unitController instanceof LightSensorController) {
                    lightSensorControllerList.add((LightSensorController) unitController);
                }
            }

            if (lightSensorControllerList.isEmpty()) {
                Assert.fail("Mock registry does not contain a lightSensor!");
                return;
            }

            double illuminance = 50000.0;
            for (LightSensorController lightSensorController : lightSensorControllerList) {
                lightSensorController.applyDataUpdate(IlluminanceState.newBuilder().setIlluminance(illuminance).build(), ServiceType.ILLUMINANCE_STATE_SERVICE);
            }

            while (locationRemote.getIlluminanceState().getIlluminance() != illuminance) {
                System.out.println("Waiting for locationRemote illuminance update!");
                Thread.sleep(10);
            }
            assertEquals("IlluminationState of location has not been updated!", illuminance, locationRemote.getIlluminanceState().getIlluminance(), 1.0);

            illuminance = 10000.0;
            for (LightSensorController lightSensorController : lightSensorControllerList) {
                lightSensorController.applyDataUpdate(IlluminanceState.newBuilder().setIlluminance(illuminance).build(), ServiceType.ILLUMINANCE_STATE_SERVICE);
            }

            while (locationRemote.getIlluminanceState().getIlluminance() != illuminance) {
                System.out.println("Waiting for locationRemote illuminance update!");
                Thread.sleep(10);
            }
            assertEquals("IlluminationState of location has not been updated!", illuminance, locationRemote.getIlluminanceState().getIlluminance(), 1.0);

        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    /**
     * Test the applyActionAuthenticated method of the location remote.
     *
     * @throws Exception if something fails.
     */
    @Test(timeout = 5000)
    public void testApplyActionAuthenticated() throws Exception {
        System.out.println("testApplyActionAuthenticated");

        // wait for data
        locationRemote.waitForData();
        Actions.waitForExecution(locationRemote.setPowerState(State.OFF));

        // init authenticated value
        final PowerState serviceState = PowerState.newBuilder().setValue(State.ON).build();
        final ActionDescription actionDescription = ActionDescriptionProcessor.generateActionDescriptionBuilder(serviceState, ServiceType.POWER_STATE_SERVICE, locationRemote).build();
        final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(actionDescription, null);

        // perform request
        final AuthenticatedValueFuture<ActionDescription> future = new AuthenticatedValueFuture<>(locationRemote.applyActionAuthenticated(authenticatedValue), ActionDescription.class, authenticatedValue.getTicketAuthenticatorWrapper(), SessionManager.getInstance());
        // wait for request
        Actions.waitForExecution(future);

        while (locationRemote.getPowerState().getValue() != serviceState.getValue()) {
            // sleep until state is published back to location
            Thread.sleep(10);
        }

        // test if new value has been set
        assertEquals(serviceState.getValue(), locationRemote.getPowerState().getValue());
    }

    @Test(timeout = 20000)
    public void testActionCancellation() throws Exception {
        System.out.println("testActionCancellation");

        final List<? extends ColorableLightRemote> colorableLightRemotes = locationRemote.getUnits(UnitType.COLORABLE_LIGHT, false, Units.COLORABLE_LIGHT);
        // validate that location has at least one unit of the type used in this test
        assertTrue("Cannot execute test if location does not have a colorable light!",
                colorableLightRemotes.size() > 0);

        for (int i = 0; i < 10; i++) {
            final PowerState.State powerState = (i % 2 == 0) ? State.ON : State.OFF;

            // execute an action
            final Future<ActionDescription> actionFuture = locationRemote.setPowerState(powerState, UnitType.COLORABLE_LIGHT);
            // wait for its execution
            Actions.waitForExecution(actionFuture);
            // save the action description
            final ActionDescription actionDescription = actionFuture.get();

            // validate that actions are running on the triggered units
            for (final ColorableLightRemote colorableLightRemote : colorableLightRemotes) {
                boolean actionRunning = false;
                for (final ActionReferenceType.ActionReference actionReference : colorableLightRemote.getActionList().get(0).getActionCauseList()) {
                    if (actionReference.getActionId().equals(actionDescription.getId())) {
                        actionRunning = true;
                        break;
                    }
                }
                assertTrue("Location action is not running on unit[" + colorableLightRemote + "]", actionRunning);
            }

            // cancel the action
            locationRemote.cancelAction(actionDescription).get();
            // validate that action is cancelled on all units
            for (final ColorableLightRemote colorableLightRemote : colorableLightRemotes) {
                ActionDescription causedAction = null;
                for (final ActionDescription description : colorableLightRemote.getActionList()) {
                    for (ActionReferenceType.ActionReference actionReference : description.getActionCauseList()) {
                        if (actionReference.getActionId().equals(actionDescription.getId())) {
                            causedAction = description;
                            break;
                        }
                    }
                }

                if (causedAction == null) {
                    fail("Caused action on unit[" + colorableLightRemote + "]could not be found!");
                }

                assertEquals("Action on unit[" + colorableLightRemote + "] was not cancelled!", ActionStateType.ActionState.State.CANCELED, causedAction.getActionState().getValue());
            }
        }
    }
}
