package org.openbase.bco.dal.test.layer.unit.location;

/*
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
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.dal.control.layer.unit.*;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.lib.state.States;
import org.openbase.bco.dal.lib.state.States.Motion;
import org.openbase.bco.dal.lib.state.States.Power;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.detector.PresenceDetector;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.LightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.session.BCOSessionImpl;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionReferenceType;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
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
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData.Builder;
import org.openbase.type.vision.ColorType.Color;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationRemoteTest extends AbstractBCOLocationManagerTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LocationRemoteTest.class);

    /**
     * Test if changes in locations are published to underlying units.
     *
     * @throws Exception
     */
    @Test
    @Timeout(10)
    public void testLocationToUnitPipeline() throws Exception {
        System.out.println("testLocationToUnitPipeline");

        final LocationRemote rootLocationRemote =
                Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);

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

            waitForExecution(rootLocationRemote.setPowerState(powerOn));
            for (PowerStateOperationService powerStateService : powerServiceList) {
                assertEquals(powerOn.getValue(), powerStateService.getPowerState().getValue(), "PowerState of unit [" + ((UnitController) powerStateService).getLabel() + "] has not been updated by the locationRemote!");
            }

            waitForExecution(rootLocationRemote.setPowerState(powerOff));
            for (PowerStateOperationService powerStateService : powerServiceList) {
                assertEquals(powerOff.getValue(), powerStateService.getPowerState().getValue(), "PowerState of unit [" + ((UnitController) powerStateService).getLabel() + "] has not been updated by the locationRemote!");
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
    @Test
    @Timeout(15)
    public void testUnitToLocationPipeline() throws Exception {
        System.out.println("testUnitToLocationPipeline");

        final LocationRemote rootLocationRemote =
                Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);

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
        final TemperatureState temperatureState = TemperatureState.newBuilder().setTemperature(temperature).build();
        for (TemperatureSensorController temperatureSensor : temperatureSensorList) {
            temperatureSensor.applyServiceState(temperatureState, ServiceType.TEMPERATURE_STATE_SERVICE);
        }
        double targetTemperature = 21;
        final TemperatureState targetTemperatureState = TemperatureState.newBuilder().setTemperature(targetTemperature).build();
        for (TemperatureControllerController temperatureController : temperatureControllerList) {
            temperatureController.applyServiceState(temperatureState, ServiceType.TEMPERATURE_STATE_SERVICE);
            temperatureController.applyServiceState(targetTemperatureState, ServiceType.TARGET_TEMPERATURE_STATE_SERVICE);
        }
        rootLocationRemote.ping().get();
        while (rootLocationRemote.getTemperatureState().getTemperature() != temperature) {
            System.out.println("current temp: " + rootLocationRemote.getTemperatureState().getTemperature() + " waiting for: " + temperature);
            Thread.sleep(10);
        }
        assertEquals(temperature, rootLocationRemote.getTemperatureState().getTemperature(), 0.01, "Temperature of the location has not been updated!");
        while (rootLocationRemote.getTargetTemperatureState().getTemperature() != targetTemperature) {
            System.out.println("current target temp: " + rootLocationRemote.getTargetTemperatureState().getTemperature() + " waiting for: " + targetTemperature);
            Thread.sleep(10);
        }
        assertEquals(targetTemperature, rootLocationRemote.getTargetTemperatureState().getTemperature(), 0.01, "TargetTemperature of the location has not been updated!");

        System.out.println("PowerConsumptionSensors: " + powerConsumptionSensorList.size());
        PowerConsumptionState powerConsumptionState = PowerConsumptionState.newBuilder().setVoltage(240).setConsumption(10).setCurrent(1).build();
        for (PowerConsumptionSensorController powerConsumptionSensor : powerConsumptionSensorList) {
            powerConsumptionSensor.applyServiceState(powerConsumptionState, ServiceType.POWER_CONSUMPTION_STATE_SERVICE);
            System.out.println("Updated powerConsumptionState of [" + powerConsumptionSensor.toString() + "] to [" + powerConsumptionSensor.getPowerConsumptionState() + "]");
        }

        while (rootLocationRemote.getPowerConsumptionState().getCurrent() != powerConsumptionState.getCurrent()) {
            System.out.println("Waiting for locationRemote powerConsumptionState update!");
            Thread.sleep(10);
        }
        assertEquals(powerConsumptionState.getVoltage(), rootLocationRemote.getPowerConsumptionState().getVoltage(), 0.01, "Voltage of location has not been updated!");
        assertEquals(powerConsumptionState.getCurrent(), rootLocationRemote.getPowerConsumptionState().getCurrent(), 0.01, "Current of location has not been updated!");
        assertEquals(powerConsumptionState.getConsumption() * powerConsumptionSensorList.size(), rootLocationRemote.getPowerConsumptionState().getConsumption(), 0.01, "Consumption of location has not been updated!");
    }

    @Test
    @Timeout(15)
    public void testRecordAndRestoreSnapshots() throws Exception {

        final LocationRemote rootLocationRemote =
                Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);

        BlindState snapshotBlindState = BlindState.newBuilder().setValue(BlindState.State.DOWN).setOpeningRatio(0).build();
        BlindState newBlindState = BlindState.newBuilder().setValue(BlindState.State.UP).setOpeningRatio(0.5).build();
        ColorState snapshotColorState = ColorState.newBuilder().setColor(Color.newBuilder().setType(Color.Type.HSB).setHsbColor(HSBColor.newBuilder().setBrightness(1).setHue(0).setSaturation(1))).build();
        ColorState newColorState = ColorState.newBuilder().setColor(Color.newBuilder().setType(Color.Type.HSB).setHsbColor(HSBColor.newBuilder().setBrightness(0.2).setHue(100).setSaturation(0.5))).build();
        PowerState snapshotPowerState = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        PowerState newPowerState = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        TemperatureState snapshotTemperatureState = TemperatureState.newBuilder().setTemperature(20).setTemperatureDataUnit(TemperatureState.DataUnit.CELSIUS).build();
        TemperatureState newTemperatureState = TemperatureState.newBuilder().setTemperature(22).setTemperatureDataUnit(TemperatureState.DataUnit.CELSIUS).build();

        logger.info("Set initial states");

        waitForExecution(rootLocationRemote.setBlindState(snapshotBlindState));
        waitForExecution(rootLocationRemote.setColorState(snapshotColorState));
        waitForExecution(rootLocationRemote.setPowerState(snapshotPowerState));
        waitForExecution(rootLocationRemote.setTargetTemperatureState(snapshotTemperatureState));

        logger.info("Record snapshot");

        Snapshot snapshot = rootLocationRemote.recordSnapshot().get();

        // save location state
        snapshotBlindState = rootLocationRemote.getBlindState(UnitType.UNKNOWN);
        snapshotColorState = rootLocationRemote.getColorState(UnitType.UNKNOWN);
        snapshotPowerState = rootLocationRemote.getPowerState(UnitType.UNKNOWN);
        snapshotTemperatureState = rootLocationRemote.getTargetTemperatureState(UnitType.UNKNOWN);

        logger.info("Change states");

        waitForExecution(rootLocationRemote.setBlindState(newBlindState));
        waitForExecution(rootLocationRemote.setColorState(newColorState));
        waitForExecution(rootLocationRemote.setPowerState(newPowerState));
        waitForExecution(rootLocationRemote.setTargetTemperatureState(newTemperatureState));

        assertTrue(rootLocationRemote.getBlindState(UnitType.UNKNOWN).getValue() != snapshotBlindState.getValue(), "BlindState of location has not changed!");
        assertTrue(!rootLocationRemote.getColorState(UnitType.UNKNOWN).getColor().getHsbColor().equals(snapshotColorState.getColor().getHsbColor()), "ColorState of location has not changed!");
        assertTrue(rootLocationRemote.getPowerState(UnitType.UNKNOWN).getValue() != snapshotPowerState.getValue(), "PowerState of location has not changed!");
        assertTrue(rootLocationRemote.getTargetTemperatureState(UnitType.UNKNOWN).getTemperature() != snapshotTemperatureState.getTemperature(), "TargetTemperatureState of location has not changed!");

        logger.info("Restore snapshot");

        rootLocationRemote.restoreSnapshot(snapshot).get(10, TimeUnit.SECONDS);
        rootLocationRemote.requestData().get(10, TimeUnit.SECONDS);

        final BlindState.State blindStateValue = snapshotBlindState.getValue();
        final HSBColor hsbColor = snapshotColorState.getColor().getHsbColor();
        final PowerState.State powerStateValue = snapshotPowerState.getValue();
        final double targetTemperature = snapshotTemperatureState.getTemperature();
        final UnitStateAwaiter<LocationData, LocationRemote> stateAwaiter = new UnitStateAwaiter<>(rootLocationRemote);
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

        assertEquals(snapshotBlindState.getValue(), rootLocationRemote.getBlindState(UnitType.UNKNOWN).getValue(), "BlindState of location has not been restored through snapshot!");
        assertEquals(snapshotColorState.getColor().getHsbColor(), rootLocationRemote.getColorState(UnitType.UNKNOWN).getColor().getHsbColor(), "ColorState of location has not been restored through snapshot!");
        assertEquals(snapshotPowerState.getValue(), rootLocationRemote.getPowerState(UnitType.UNKNOWN).getValue(), "PowerState of location has not been restored through snapshot!");
        assertEquals(snapshotTemperatureState.getTemperature(), rootLocationRemote.getTargetTemperatureState(UnitType.UNKNOWN).getTemperature(), 0.5, "TargetTemperatureState of location has not been restored through snapshot!");
    }

    @Test
    @Timeout(5)
    public void testManipulatingByUnitType() throws Exception {
        System.out.println("testManipulatingByUnitType");

        final LocationRemote rootLocationRemote =
                Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);

        try {
            List<UnitType> lightTypes = Registries.getTemplateRegistry().getSubUnitTypes(UnitType.LIGHT);
            lightTypes.add(UnitType.LIGHT);
            assertTrue(lightTypes.contains(UnitType.LIGHT), "UnitType not found as subType of LIGHT!");
            assertTrue(lightTypes.contains(UnitType.DIMMABLE_LIGHT), "UnitType not found as subType of LIGHT!");
            assertTrue(lightTypes.contains(UnitType.COLORABLE_LIGHT), "UnitType not found as subType of LIGHT!");

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

            waitForExecution(rootLocationRemote.setPowerState(powerOn, UnitType.LIGHT));
            for (PowerStateOperationService powerStateService : powerServiceList) {
                assertEquals(powerOn.getValue(), powerStateService.getPowerState().getValue(), "PowerState of lightUnit [" + ((UnitController) powerStateService).getLabel() + "] has not been updated by the loationRemote!");
            }

            waitForExecution(rootLocationRemote.setPowerState(powerOff));
            for (PowerStateOperationService powerStateService : powerServiceList) {
                assertEquals(powerOff.getValue(), powerStateService.getPowerState().getValue(), "PowerState of lightUnit [" + ((UnitController) powerStateService).getLabel() + "] has not been updated by the loationRemote!");
            }

            for (PowerStateOperationService powerStateOperationService : powerServiceList) {
                powerStateOperationService.setPowerState(powerOn).get();
            }
            while (rootLocationRemote.getPowerState(UnitType.LIGHT).getValue() != powerOn.getValue()) {
                System.out.println("Waiting for locationRemote update!");
                Thread.sleep(10);
            }
            assertEquals(powerOn.getValue(), rootLocationRemote.getPowerState(UnitType.LIGHT).getValue(), "PowerState of location has not been updated!");

            for (PowerStateOperationService powerStateOperationService : powerServiceList) {
                powerStateOperationService.setPowerState(powerOff).get();
            }

            while (rootLocationRemote.getPowerState(UnitType.LIGHT).getValue() != powerOff.getValue()) {
                System.out.println("Waiting for locationRemote update!");
                Thread.sleep(10);
            }
            assertEquals(powerOff.getValue(), rootLocationRemote.getPowerState(UnitType.LIGHT).getValue(), "PowerState of location has not been updated!");
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    @Test
    @Timeout(15)
    public void testPresenceState() throws Exception {
        System.out.println("testPresenceState");

        final LocationRemote rootLocationRemote =
                Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);

        try {
            MotionDetectorController motionDetectorController = (MotionDetectorController)
                    deviceManagerLauncher
                            .getLaunchable()
                            .getUnitControllerRegistry()
                            .getUnitByAlias(MockRegistry.ALIAS_MOTION_SENSOR_HELL);

            motionDetectorController.applyServiceState(Motion.MOTION, ServiceType.MOTION_STATE_SERVICE);

            while (rootLocationRemote.getPresenceState().getValue() != PresenceState.State.PRESENT) {
                System.out.println("Waiting for locationRemote presenceState update!");
                Thread.sleep(10);
            }
            assertEquals(PresenceState.State.PRESENT, rootLocationRemote.getPresenceState().getValue(), "PresenceState of location has not been updated!");

            motionDetectorController.applyServiceState(Motion.NO_MOTION, ServiceType.MOTION_STATE_SERVICE);

            Thread.sleep(PresenceDetector.PRESENCE_TIMEOUT);
            while (rootLocationRemote.getPresenceState().getValue() != PresenceState.State.ABSENT) {
                System.out.println("Waiting for locationRemote presenceState update!");
                Thread.sleep(10);
            }
            assertEquals(PresenceState.State.ABSENT, rootLocationRemote.getPresenceState().getValue(), "PresenceState of location has not been updated!");
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    /**
     * Test of setColor and setPowerstate method, of class LocationRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testColorableLightControlViaLocation() throws Exception {
        System.out.println("testColorableLightControlViaLocation");

        final LocationRemote rootLocationRemote =
                Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);
        final List<? extends ColorableLightRemote> colorableLightRemotes =
                rootLocationRemote.getUnits(UnitType.COLORABLE_LIGHT, true, Units.COLORABLE_LIGHT, true);
        final List<? extends LightRemote> lightRemotes =
                rootLocationRemote.getUnits(UnitType.LIGHT, true, Units.LIGHT, true);

        // ==== TEST SUBTYPES

        // validate light remotes are sublist of colorable lights
        // todo: this can only be realized if interfaces like the "LightRemote" interface are introduced. Then ColorableLightRemoteImpl and LightRemoteImpl can both implement the "LightRemote" interface and locationRemote.getUnits(UnitType.LIGHT, true, Units.LIGHT); could list all of them.
        // Currently, only pur lightRemotes are returned to guarantee the remote class cast.
//        for (ColorableLightRemote colorableLightRemote : colorableLightRemotes) {
//            boolean found = false;
//            for (LightRemote lightRemote : lightRemotes) {
//                if(colorableLightRemote.getId().equals(lightRemote.getId())) {
//                    found = true;
//                }
//            }
//            assertTrue(colorableLightRemote + " is not included in light list of same loction.", found);
//        }

        // ==== TEST POWER ON SYNC

        // switch lights on
        waitForExecution(rootLocationRemote.setPowerState(State.ON));

        // validate colorable light remote states
        for (ColorableLightRemote colorableLightRemote : colorableLightRemotes) {
            colorableLightRemote.requestData().get();
            assertEquals(State.ON, colorableLightRemote.getData().getPowerState().getValue(), "Power has not been set in time!");
        }

        // validate light remote states
        for (LightRemote lightRemote : lightRemotes) {
            lightRemote.requestData().get();
            assertEquals(State.ON, lightRemote.getData().getPowerState().getValue(), "Power has not been set in time!");
        }

        // ==== TEST COLOR SYNC

        // set color via location
        waitForExecution(rootLocationRemote.setColorState(States.Color.RED));

        // validate at colorable light remote
        for (ColorableLightRemote colorableLightRemote : colorableLightRemotes) {
            colorableLightRemote.requestData().get();
            assertEquals(States.Color.RED.getColor().getHsbColor(), colorableLightRemote.getData().getColorState().getColor().getHsbColor(), "Color has not been set in time!");
        }

        // validate light remote states
        for (LightRemote lightRemote : lightRemotes) {
            lightRemote.requestData().get();
            assertEquals(State.ON, lightRemote.getData().getPowerState().getValue(), "Power has not been set in time!");
        }

        // ==== TEST POWER OFF SYNC

        // switch lights on
        waitForExecution(rootLocationRemote.setPowerState(State.OFF, UnitType.LIGHT));

        // validate colorable light remote states
        for (ColorableLightRemote colorableLightRemote : colorableLightRemotes) {
            colorableLightRemote.requestData().get();
            assertEquals(State.OFF, colorableLightRemote.getData().getPowerState().getValue(), "Power has not been set in time!");
        }

        // validate light remote states
        for (LightRemote lightRemote : lightRemotes) {
            lightRemote.requestData().get();
            assertEquals(State.OFF, lightRemote.getData().getPowerState().getValue(), "Power has not been set in time!");
        }
    }

    @Test
    @Timeout(15)
    public void testIlluminanceState() throws Exception {
        System.out.println("testIlluminanceState");

        final LocationRemote rootLocationRemote =
                Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);
        
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
                fail("Mock registry does not contain a lightSensor!");
                return;
            }

            double illuminance = 50000.0;
            for (LightSensorController lightSensorController : lightSensorControllerList) {
                lightSensorController.applyServiceState(IlluminanceState.newBuilder().setIlluminance(illuminance).build(), ServiceType.ILLUMINANCE_STATE_SERVICE);
            }

            while (rootLocationRemote.getIlluminanceState().getIlluminance() != illuminance) {
                System.out.println("Waiting for locationRemote illuminance update!");
                Thread.sleep(10);
            }
            assertEquals(illuminance, rootLocationRemote.getIlluminanceState().getIlluminance(), 1.0, "IlluminationState of location has not been updated!");

            illuminance = 10000.0;
            for (LightSensorController lightSensorController : lightSensorControllerList) {
                lightSensorController.applyServiceState(IlluminanceState.newBuilder().setIlluminance(illuminance).build(), ServiceType.ILLUMINANCE_STATE_SERVICE);
            }

            while (rootLocationRemote.getIlluminanceState().getIlluminance() != illuminance) {
                System.out.println("Waiting for locationRemote illuminance update!");
                Thread.sleep(10);
            }
            assertEquals(illuminance, rootLocationRemote.getIlluminanceState().getIlluminance(), 1.0, "IlluminationState of location has not been updated!");

        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    /**
     * Test the applyActionAuthenticated method of the location remote.
     *
     * @throws Exception if something fails.
     */
    @Test
    @Timeout(15)
    public void testApplyActionAuthenticated() throws Exception {
        System.out.println("testApplyActionAuthenticated");

        final LocationRemote rootLocationRemote =
                Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);
        
        // wait for data
        rootLocationRemote.waitForData();
        waitForExecution(rootLocationRemote.setPowerState(State.OFF));

        // init authenticated value
        final PowerState serviceState = PowerState.newBuilder().setValue(State.ON).build();
        final ActionDescription actionDescription = ActionDescriptionProcessor.generateActionDescriptionBuilder(serviceState, ServiceType.POWER_STATE_SERVICE, rootLocationRemote).build();
        final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(actionDescription, null);

        // perform request
        final AuthenticatedValueFuture<ActionDescription> future = new AuthenticatedValueFuture<>(rootLocationRemote.applyActionAuthenticated(authenticatedValue), ActionDescription.class, authenticatedValue.getTicketAuthenticatorWrapper(), SessionManager.getInstance());
        // wait for request
        waitForExecution(future);

        while (rootLocationRemote.getPowerState().getValue() != serviceState.getValue()) {
            // sleep until state is published back to location
            Thread.sleep(10);
        }

        // test if new value has been set
        assertEquals(serviceState.getValue(), rootLocationRemote.getPowerState().getValue());
    }

    @Test
    @Timeout(20)
    public void testActionCancellation() throws Exception {
        System.out.println("testActionCancellation");

        final LocationRemote rootLocationRemote =
                Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);
        
        final List<? extends ColorableLightRemote> colorableLightRemotes = rootLocationRemote.getUnits(UnitType.COLORABLE_LIGHT, false, Units.COLORABLE_LIGHT);
        // validate that location has at least one unit of the type used in this test
        assertTrue(colorableLightRemotes.size() > 0, "Cannot execute test if location does not have a colorable light!");

        for (int i = 0; i < 10; i++) {
            final PowerState.State powerState = (i % 2 == 0) ? State.ON : State.OFF;

            // execute an action
            final RemoteAction remoteAction = waitForExecution(rootLocationRemote.setPowerState(powerState, UnitType.COLORABLE_LIGHT));
            // save the action description

            // validate that actions are running on the triggered units
            for (final ColorableLightRemote colorableLightRemote : colorableLightRemotes) {
                boolean actionRunning = false;
                for (final ActionReferenceType.ActionReference actionReference : colorableLightRemote.getActionList().get(0).getActionCauseList()) {
                    if (actionReference.getActionId().equals(remoteAction.getActionId())) {
                        actionRunning = true;
                        break;
                    }
                }
                assertTrue(actionRunning, "Location action is not running on unit[" + colorableLightRemote + "]");
            }

            // cancel the action
            remoteAction.waitForRegistration();
            remoteAction.cancel().get();
            //locationRemote.cancelAction(actionDescription).get();
            // validate that action is cancelled on all units
            for (final ColorableLightRemote colorableLightRemote : colorableLightRemotes) {
                ActionDescription causedAction = null;
                for (final ActionDescription description : colorableLightRemote.getActionList()) {
                    for (ActionReferenceType.ActionReference actionReference : description.getActionCauseList()) {
                        if (actionReference.getActionId().equals(remoteAction.getActionId())) {
                            causedAction = description;
                            break;
                        }
                    }
                }

                if (causedAction == null) {
                    fail("Caused action on unit[" + colorableLightRemote + "]could not be found!");
                }

                assertEquals(ActionStateType.ActionState.State.CANCELED, causedAction.getActionState().getValue(), "Action on unit[" + colorableLightRemote + "] was not cancelled!");
            }
        }
    }

    @Test
    @Timeout(5)
    public void testLocationModificationViaApplyAction() throws Exception {

        final UnitRemote<?> unit = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig().getId(), true);

        final List<RemoteAction> remoteActions = new ArrayList<>();

        final BCOSessionImpl bcoSession = new BCOSessionImpl(new SessionManager());
        bcoSession.loginUserViaUsername("admin", "admin", false);

        final String token = bcoSession.generateAuthToken().getAuthenticationToken();

        LocationData locationData = LocationData.newBuilder().setPowerState(Power.ON).build();

        for (final ServiceType serviceType : unit.getSupportedServiceTypes()) {

            if (!Services.hasServiceState(serviceType, ServiceTempus.CURRENT, locationData)) {
                continue;
            }

            final Message serviceState = Services.invokeProviderServiceMethod(serviceType, locationData);
            final ActionParameter.Builder builder = ActionDescriptionProcessor.generateDefaultActionParameter(serviceState, serviceType, unit);
            final AuthToken authToken = AuthToken.newBuilder().setAuthenticationToken(token).build();
            builder.setAuthToken(authToken);

            builder.getServiceStateDescriptionBuilder().setUnitId(unit.getId());

            logger.debug("action parameter:" + builder);
            try {
                logger.debug("action registered:" + unit.applyAction(builder).get());
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            remoteActions.add(
                observe(unit.applyAction(builder), authToken, true)
            );
        }

        if (!remoteActions.isEmpty()) {
            for (final RemoteAction remoteAction : remoteActions) {
                remoteAction.waitForRegistration(5, TimeUnit.SECONDS);
            }
        }
    }
}
