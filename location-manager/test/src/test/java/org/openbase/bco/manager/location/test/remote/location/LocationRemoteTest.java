package org.openbase.bco.manager.location.test.remote.location;

/*
 * #%L
 * BCO Manager Location Test
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.LightSensorController;
import org.openbase.bco.dal.lib.layer.unit.MotionDetectorController;
import org.openbase.bco.dal.lib.layer.unit.PowerConsumptionSensorController;
import org.openbase.bco.dal.lib.layer.unit.TemperatureControllerController;
import org.openbase.bco.dal.lib.layer.unit.TemperatureSensorController;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.remote.detector.PresenceDetector;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.slf4j.LoggerFactory;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.IlluminanceStateType.IlluminanceState;
import rst.domotic.state.MotionStateType.MotionState;
import rst.domotic.state.PowerConsumptionStateType.PowerConsumptionState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.vision.ColorType.Color;
import rst.vision.HSBColorType.HSBColor;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationRemoteTest extends AbstractBCOLocationManagerTest{

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LocationRemoteTest.class);

    private static LocationRemote locationRemote;

    public LocationRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            AbstractBCOLocationManagerTest.setUpClass();
            locationRemote = Units.getUnit(Registries.getLocationRegistry().getRootLocationConfig(), true, Units.LOCATION);
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    /**
     * Test if changes in locations are published to underlying units.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
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

            locationRemote.setPowerState(powerOn).get();
            for (PowerStateOperationService powerStateService : powerServiceList) {
                Assert.assertEquals("PowerState of unit [" + ((UnitController) powerStateService).getLabel() + "] has not been updated by the loationRemote!", powerOn.getValue(), powerStateService.getPowerState().getValue());
            }

            locationRemote.setPowerState(powerOff).get();
            for (PowerStateOperationService powerStateService : powerServiceList) {
                Assert.assertEquals("PowerState of unit [" + ((UnitController) powerStateService).getLabel() + "] has not been updated by the loationRemote!", powerOff.getValue(), powerStateService.getPowerState().getValue());
            }
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    private boolean unitHasService(UnitConfig unitConfig, ServiceType serviceType, ServicePattern servicePattern) throws CouldNotPerformException, NotAvailableException, InterruptedException {
        for (ServiceDescription serviceDescription : Registries.getUnitRegistry().getUnitTemplateByType(unitConfig.getType()).getServiceDescriptionList()) {
            if (serviceDescription.getType() == serviceType && serviceDescription.getPattern() == servicePattern) {
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

        double temperature = 21;
        TemperatureState temperatureState = TemperatureState.newBuilder().setTemperature(temperature).build();
        for (TemperatureSensorController temperatureSensor : temperatureSensorList) {
            temperatureSensor.applyDataUpdate(temperatureState);
        }
        for (TemperatureControllerController temperatureController : temperatureControllerList) {
            temperatureController.applyDataUpdate(temperatureState);
        }
        System.out.println("ping");
        locationRemote.ping().get();
        System.out.println("ping done");
        System.out.println("request data of " + ScopeGenerator.generateStringRep(locationRemote.getScope()));
        System.out.println("got data: " + locationRemote.requestData().get().getTemperatureState().getTemperature());
        while (locationRemote.getTemperatureState().getTemperature() != temperature) {
            System.out.println("current temp: " + locationRemote.getTemperatureState().getTemperature() + " waiting for: " + temperature);
            Thread.sleep(10);
        }
        Assert.assertEquals("Temperature of the location has not been updated!", temperature, locationRemote.getTemperatureState().getTemperature(), 0.01);

        System.out.println("PowerConsumptionSensors: " + powerConsumptionSensorList.size());
        PowerConsumptionState powerConsumptionState = PowerConsumptionState.newBuilder().setVoltage(240).setConsumption(10).setCurrent(1).build();
        for (PowerConsumptionSensorController powerConsumptionSensor : powerConsumptionSensorList) {
            powerConsumptionSensor.applyDataUpdate(powerConsumptionState);
            System.out.println("Updated powerConsumptionState of [" + powerConsumptionSensor.toString() + "] to [" + powerConsumptionSensor.getPowerConsumptionState() + "]");
        }

        while (locationRemote.getPowerConsumptionState().getCurrent() != powerConsumptionState.getCurrent()) {
            System.out.println("Waiting for locationRemote powerConsumptionState update!");
            Thread.sleep(10);
        }
        Assert.assertEquals("Voltage of location has not been updated!", powerConsumptionState.getVoltage(), locationRemote.getPowerConsumptionState().getVoltage(), 0.01);
        Assert.assertEquals("Current of location has not been updated!", powerConsumptionState.getCurrent(), locationRemote.getPowerConsumptionState().getCurrent(), 0.01);
        Assert.assertEquals("Consumption of location has not been updated!", powerConsumptionState.getConsumption() * powerConsumptionSensorList.size(), locationRemote.getPowerConsumptionState().getConsumption(), 0.01);
    }

    @Test(timeout = 5000)
    public void testRecordAndRestoreSnapshots() throws Exception {
        BlindState snapshotBlindState = BlindState.newBuilder().setMovementState(BlindState.MovementState.DOWN).setOpeningRatio(0).build();
        BlindState newBlindState = BlindState.newBuilder().setMovementState(BlindState.MovementState.UP).setOpeningRatio(50).build();
        ColorState snapshotColorState = ColorState.newBuilder().setColor(Color.newBuilder().setType(Color.Type.HSB).setHsbColor(HSBColor.newBuilder().setBrightness(100).setHue(0).setSaturation(100))).build();
        ColorState newColorState = ColorState.newBuilder().setColor(Color.newBuilder().setType(Color.Type.HSB).setHsbColor(HSBColor.newBuilder().setBrightness(20).setHue(100).setSaturation(50))).build();
        PowerState snapshotPowerState = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        PowerState newPowerState = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        TemperatureState snapshotTemperatureState = TemperatureState.newBuilder().setTemperature(20).setTemperatureDataUnit(TemperatureState.DataUnit.CELSIUS).build();
        TemperatureState newTemperatureState = TemperatureState.newBuilder().setTemperature(22).setTemperatureDataUnit(TemperatureState.DataUnit.CELSIUS).build();

        locationRemote.setBlindState(snapshotBlindState).get();
        locationRemote.setColorState(snapshotColorState).get();
        locationRemote.setPowerState(snapshotPowerState).get();
        locationRemote.setTargetTemperatureState(snapshotTemperatureState).get();

        Snapshot snapshot = locationRemote.recordSnapshot().get();

        // save location state
        snapshotBlindState = locationRemote.getBlindState(UnitType.UNKNOWN);
        snapshotColorState = locationRemote.getColorState(UnitType.UNKNOWN);
        snapshotPowerState = locationRemote.getPowerState(UnitType.UNKNOWN);
        snapshotTemperatureState = locationRemote.getTargetTemperatureState(UnitType.UNKNOWN);

        locationRemote.setBlindState(newBlindState).get();
        locationRemote.setColorState(newColorState).get();
        locationRemote.setPowerState(newPowerState).get();
        locationRemote.setTargetTemperatureState(newTemperatureState).get();
        locationRemote.requestData().get();

        assertTrue("BlindState of location has not changed!", locationRemote.getBlindState(UnitType.UNKNOWN).getMovementState() != snapshotBlindState.getMovementState());
        assertTrue("ColorState of location has not changed!", !locationRemote.getColorState(UnitType.UNKNOWN).getColor().getHsbColor().equals(snapshotColorState.getColor().getHsbColor()));
        assertTrue("PowerState of location has not changed!", locationRemote.getPowerState(UnitType.UNKNOWN).getValue() != snapshotPowerState.getValue());
        assertTrue("TargetTemperatureState of location has not changed!", locationRemote.getTargetTemperatureState(UnitType.UNKNOWN).getTemperature() != snapshotTemperatureState.getTemperature());

        locationRemote.restoreSnapshot(snapshot).get();
        locationRemote.requestData().get();

        assertTrue("BlindState of location has not been restored through snapshot!", locationRemote.getBlindState(UnitType.UNKNOWN).getMovementState() == snapshotBlindState.getMovementState());
        assertTrue("ColorState of location has not been restored through snapshot!", locationRemote.getColorState(UnitType.UNKNOWN).getColor().getHsbColor().equals(snapshotColorState.getColor().getHsbColor()));
        assertTrue("PowerState of location has not been restored through snapshot!", locationRemote.getPowerState(UnitType.UNKNOWN).getValue() == snapshotPowerState.getValue());
        assertTrue("TargetTemperatureState of location has not been restored through snapshot!", locationRemote.getTargetTemperatureState(UnitType.UNKNOWN).getTemperature() == snapshotTemperatureState.getTemperature());
    }

    @Test(timeout = 5000)
    public void testManipulatingByUnitType() throws Exception {
        System.out.println("testManipulatingByUnitType");

        try {
            List<UnitType> lightTypes = Registries.getUnitRegistry().getSubUnitTypes(UnitType.LIGHT);
            lightTypes.add(UnitType.LIGHT);
            assertTrue("UnitType not found as subType of LIGHT!", lightTypes.contains(UnitType.LIGHT));
            assertTrue("UnitType not found as subType of LIGHT!", lightTypes.contains(UnitType.DIMMABLE_LIGHT));
            assertTrue("UnitType not found as subType of LIGHT!", lightTypes.contains(UnitType.COLORABLE_LIGHT));

            List<PowerStateOperationService> powerServiceList = new ArrayList<>();
            for (UnitConfig dalUnitConfig : Registries.getUnitRegistry().getDalUnitConfigs()) {
                if (dalUnitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    continue;
                }

                if (lightTypes.contains(dalUnitConfig.getType())) {
                    UnitController unitController = deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dalUnitConfig.getId());
                    powerServiceList.add((PowerStateOperationService) unitController);
                }
            }

            PowerState powerOn = PowerState.newBuilder().setValue(PowerState.State.ON).build();
            PowerState powerOff = PowerState.newBuilder().setValue(PowerState.State.OFF).build();

            locationRemote.setPowerState(powerOn, UnitType.LIGHT).get();
            for (PowerStateOperationService powerStateService : powerServiceList) {
                Assert.assertEquals("PowerState of lightUnit [" + ((UnitController) powerStateService).getLabel() + "] has not been updated by the loationRemote!", powerOn.getValue(), powerStateService.getPowerState().getValue());
            }

            locationRemote.setPowerState(powerOff).get();
            for (PowerStateOperationService powerStateService : powerServiceList) {
                Assert.assertEquals("PowerState of lightUnit [" + ((UnitController) powerStateService).getLabel() + "] has not been updated by the loationRemote!", powerOff.getValue(), powerStateService.getPowerState().getValue());
            }

            for (PowerStateOperationService powerStateOperationService : powerServiceList) {
                powerStateOperationService.setPowerState(powerOn).get();
            }
            while (locationRemote.getPowerState(UnitType.LIGHT).getValue() != powerOn.getValue()) {
                System.out.println("Waiting for locationRemote update!");
                Thread.sleep(10);
            }
            Assert.assertEquals("PowerState of location has not been updated!", powerOn.getValue(), locationRemote.getPowerState(UnitType.LIGHT).getValue());

            for (PowerStateOperationService powerStateOperationService : powerServiceList) {
                powerStateOperationService.setPowerState(powerOff).get();
            }

            while (locationRemote.getPowerState(UnitType.LIGHT).getValue() != powerOff.getValue()) {
                System.out.println("Waiting for locationRemote update!");
                Thread.sleep(10);
            }
            Assert.assertEquals("PowerState of location has not been updated!", powerOff.getValue(), locationRemote.getPowerState(UnitType.LIGHT).getValue());
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

            motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(MotionState.newBuilder().setValue(MotionState.State.MOTION).build()));

            while (locationRemote.getPresenceState().getValue() != PresenceState.State.PRESENT) {
                System.out.println("Waiting for locationRemote presenceState update!");
                Thread.sleep(10);
            }
            Assert.assertEquals("PresenceState of location has not been updated!", PresenceState.State.PRESENT, locationRemote.getPresenceState().getValue());

            motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(MotionState.newBuilder().setValue(MotionState.State.NO_MOTION).build()));

            Thread.sleep(PresenceDetector.PRESENCE_TIMEOUT);
            while (locationRemote.getPresenceState().getValue() != PresenceState.State.ABSENT) {
                System.out.println("Waiting for locationRemote presenceState update!");
                Thread.sleep(10);
            }
            Assert.assertEquals("PresenceState of location has not been updated!", PresenceState.State.ABSENT, locationRemote.getPresenceState().getValue());
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
                lightSensorController.applyDataUpdate(IlluminanceState.newBuilder().setIlluminance(illuminance).build());
            }

            while (locationRemote.getIlluminanceState().getIlluminance() != illuminance) {
                System.out.println("Waiting for locationRemote illuminance update!");
                Thread.sleep(10);
            }
            Assert.assertEquals("IlluminationState of location has not been updated!", illuminance, locationRemote.getIlluminanceState().getIlluminance(), 1.0);

            illuminance = 10000.0;
            for (LightSensorController lightSensorController : lightSensorControllerList) {
                lightSensorController.applyDataUpdate(IlluminanceState.newBuilder().setIlluminance(illuminance).build());
            }

            while (locationRemote.getIlluminanceState().getIlluminance() != illuminance) {
                System.out.println("Waiting for locationRemote illuminance update!");
                Thread.sleep(10);
            }
            Assert.assertEquals("IlluminationState of location has not been updated!", illuminance, locationRemote.getIlluminanceState().getIlluminance(), 1.0);

        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }
}
