/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.hager;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.data.transform.PowerStateTransformer;
import de.citec.dal.hal.unit.PowerConsumptionSensorController;
import de.citec.dal.hal.unit.PowerPlugController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.VerificationFailedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.hager.HA_TYA606EType;
import rst.homeautomation.openhab.OnOffHolderType.OnOffHolder.OnOff;

/**
 *
 * @author mpohling
 */
public class HA_TYA606EController extends AbstractOpenHABDeviceController<HA_TYA606EType.HA_TYA606E, HA_TYA606EType.HA_TYA606E.Builder> {

    private final static String COMPONENT_POWER_PLUG_0 = "PowerPlug_0";
    private final static String COMPONENT_POWER_PLUG_1 = "PowerPlug_1";
    private final static String COMPONENT_POWER_PLUG_2 = "PowerPlug_2";
    private final static String COMPONENT_POWER_PLUG_3 = "PowerPlug_3";
    private final static String COMPONENT_POWER_PLUG_4 = "PowerPlug_4";
    private final static String COMPONENT_POWER_PLUG_5 = "PowerPlug_5";
    private final static String COMPONENT_POWER_CONSUMPTION_SENSOR = "PowerConsumptionSensor";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(HA_TYA606EType.HA_TYA606E.getDefaultInstance()));
    }

    private final PowerPlugController powerPlug_0;
    private final PowerPlugController powerPlug_1;
    private final PowerPlugController powerPlug_2;
    private final PowerPlugController powerPlug_3;
    private final PowerPlugController powerPlug_4;
    private final PowerPlugController powerPlug_5;
    private final PowerConsumptionSensorController powerConsumptionSensor_0;
    private final PowerConsumptionSensorController powerConsumptionSensor_1;
    private final PowerConsumptionSensorController powerConsumptionSensor_2;
    private final PowerConsumptionSensorController powerConsumptionSensor_3;
    private final PowerConsumptionSensorController powerConsumptionSensor_4;
    private final PowerConsumptionSensorController powerConsumptionSensor_5;

    public HA_TYA606EController(final String id, final String label, final Location location) throws VerificationFailedException, InstantiationException {
        super(id, label, location, HA_TYA606EType.HA_TYA606E.newBuilder());

        data.setId(id);
        this.powerPlug_0 = new PowerPlugController(COMPONENT_POWER_PLUG_0, label, this, data.getPowerPlug0Builder());
        this.powerPlug_1 = new PowerPlugController(COMPONENT_POWER_PLUG_1, label, this, data.getPowerPlug1Builder());
        this.powerPlug_2 = new PowerPlugController(COMPONENT_POWER_PLUG_2, label, this, data.getPowerPlug2Builder());
        this.powerPlug_3 = new PowerPlugController(COMPONENT_POWER_PLUG_3, label, this, data.getPowerPlug3Builder());
        this.powerPlug_4 = new PowerPlugController(COMPONENT_POWER_PLUG_4, label, this, data.getPowerPlug4Builder());
        this.powerPlug_5 = new PowerPlugController(COMPONENT_POWER_PLUG_5, label, this, data.getPowerPlug5Builder());
        this.powerConsumptionSensor_0 = new PowerConsumptionSensorController(COMPONENT_POWER_CONSUMPTION_SENSOR + "_0", label, this, data.getPowerConsumptionSensor0Builder());
        this.powerConsumptionSensor_1 = new PowerConsumptionSensorController(COMPONENT_POWER_CONSUMPTION_SENSOR + "_1", label, this, data.getPowerConsumptionSensor1Builder());
        this.powerConsumptionSensor_2 = new PowerConsumptionSensorController(COMPONENT_POWER_CONSUMPTION_SENSOR + "_2", label, this, data.getPowerConsumptionSensor2Builder());
        this.powerConsumptionSensor_3 = new PowerConsumptionSensorController(COMPONENT_POWER_CONSUMPTION_SENSOR + "_3", label, this, data.getPowerConsumptionSensor3Builder());
        this.powerConsumptionSensor_4 = new PowerConsumptionSensorController(COMPONENT_POWER_CONSUMPTION_SENSOR + "_4", label, this, data.getPowerConsumptionSensor4Builder());
        this.powerConsumptionSensor_5 = new PowerConsumptionSensorController(COMPONENT_POWER_CONSUMPTION_SENSOR + "_5", label, this, data.getPowerConsumptionSensor5Builder());
        this.registerUnit(powerPlug_0);
        this.registerUnit(powerPlug_1);
        this.registerUnit(powerPlug_2);
        this.registerUnit(powerPlug_3);
        this.registerUnit(powerPlug_4);
        this.registerUnit(powerPlug_5);
        this.registerUnit(powerConsumptionSensor_0);
        this.registerUnit(powerConsumptionSensor_1);
        this.registerUnit(powerConsumptionSensor_2);
        this.registerUnit(powerConsumptionSensor_3);
        this.registerUnit(powerConsumptionSensor_4);
        this.registerUnit(powerConsumptionSensor_5);
    }

    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(COMPONENT_POWER_PLUG_0, getClass().getMethod("updatePowerPlug_0", OnOff.class));
        halFunctionMapping.put(COMPONENT_POWER_PLUG_1, getClass().getMethod("updatePowerPlug_1", OnOff.class));
        halFunctionMapping.put(COMPONENT_POWER_PLUG_2, getClass().getMethod("updatePowerPlug_2", OnOff.class));
        halFunctionMapping.put(COMPONENT_POWER_PLUG_3, getClass().getMethod("updatePowerPlug_3", OnOff.class));
        halFunctionMapping.put(COMPONENT_POWER_PLUG_4, getClass().getMethod("updatePowerPlug_4", OnOff.class));
        halFunctionMapping.put(COMPONENT_POWER_PLUG_5, getClass().getMethod("updatePowerPlug_5", OnOff.class));
        halFunctionMapping.put(COMPONENT_POWER_CONSUMPTION_SENSOR + "_0", getClass().getMethod("updatePowerConsumption_0", double.class));
        halFunctionMapping.put(COMPONENT_POWER_CONSUMPTION_SENSOR + "_1", getClass().getMethod("updatePowerConsumption_1", double.class));
        halFunctionMapping.put(COMPONENT_POWER_CONSUMPTION_SENSOR + "_2", getClass().getMethod("updatePowerConsumption_2", double.class));
        halFunctionMapping.put(COMPONENT_POWER_CONSUMPTION_SENSOR + "_3", getClass().getMethod("updatePowerConsumption_3", double.class));
        halFunctionMapping.put(COMPONENT_POWER_CONSUMPTION_SENSOR + "_4", getClass().getMethod("updatePowerConsumption_4", double.class));
        halFunctionMapping.put(COMPONENT_POWER_CONSUMPTION_SENSOR + "_5", getClass().getMethod("updatePowerConsumption_5", double.class));
    }

    public void updatePowerPlug_0(OnOff type) throws CouldNotPerformException {
        try {
            powerPlug_0.updatePowerState(PowerStateTransformer.transform(type));
        } catch (CouldNotTransformException ex) {
            throw new CouldNotPerformException("Could not updatePowerPlug_0!", ex);
        }
    }

    public void updatePowerPlug_1(OnOff type) throws CouldNotPerformException {
        try {
            powerPlug_1.updatePowerState(PowerStateTransformer.transform(type));
        } catch (CouldNotTransformException ex) {
            throw new CouldNotPerformException("Could not updatePowerPlug_1!", ex);
        }
    }

    public void updatePowerPlug_2(OnOff type) throws CouldNotPerformException {
        try {
            powerPlug_2.updatePowerState(PowerStateTransformer.transform(type));
        } catch (CouldNotTransformException ex) {
            throw new CouldNotPerformException("Could not updatePowerPlug_2!", ex);
        }
    }

    public void updatePowerPlug_3(OnOff type) throws CouldNotPerformException {
        try {
            powerPlug_3.updatePowerState(PowerStateTransformer.transform(type));
        } catch (CouldNotTransformException ex) {
            throw new CouldNotPerformException("Could not updatePowerPlug_3!", ex);
        }
    }

    public void updatePowerPlug_4(OnOff type) throws CouldNotPerformException {
        try {
            powerPlug_4.updatePowerState(PowerStateTransformer.transform(type));
        } catch (CouldNotTransformException ex) {
            throw new CouldNotPerformException("Could not updatePowerPlug_4!", ex);
        }
    }

    public void updatePowerPlug_5(OnOff type) throws CouldNotPerformException {
        try {
            powerPlug_5.updatePowerState(PowerStateTransformer.transform(type));
        } catch (CouldNotTransformException ex) {
            throw new CouldNotPerformException("Could not updatePowerPlug_5!", ex);
        }
    }

    public void updatePowerConsumption_0(double value) {
        powerConsumptionSensor_0.updatePowerConsumption((float) value);
    }

    public void updatePowerConsumption_1(double value) {
        powerConsumptionSensor_1.updatePowerConsumption((float) value);
    }

    public void updatePowerConsumption_2(double value) {
        powerConsumptionSensor_2.updatePowerConsumption((float) value);
    }

    public void updatePowerConsumption_3(double value) {
        powerConsumptionSensor_3.updatePowerConsumption((float) value);
    }

    public void updatePowerConsumption_4(double value) {
        powerConsumptionSensor_4.updatePowerConsumption((float) value);
    }

    public void updatePowerConsumption_5(double value) {
        powerConsumptionSensor_5.updatePowerConsumption((float) value);
    }
}
