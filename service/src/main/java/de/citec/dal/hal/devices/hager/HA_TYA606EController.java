/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.devices.hager;

import de.citec.dal.data.Location;
import de.citec.dal.data.transform.PowerStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractHardwareController;
import de.citec.dal.hal.al.PowerConsumptionSensorController;
import de.citec.dal.hal.al.PowerPlugController;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.hager.HA_TYA606EType;

/**
 *
 * @author mpohling
 */
public class HA_TYA606EController extends AbstractHardwareController<HA_TYA606EType.HA_TYA606E, HA_TYA606EType.HA_TYA606E.Builder> {

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

    public HA_TYA606EController(final String id, final Location location) throws RSBBindingException {
        super(id, location, HA_TYA606EType.HA_TYA606E.newBuilder());

        builder.setId(id);
        this.powerPlug_0 = new PowerPlugController(COMPONENT_POWER_PLUG_0, this, builder.getPowerPlug0Builder());
        this.powerPlug_1 = new PowerPlugController(COMPONENT_POWER_PLUG_1, this, builder.getPowerPlug1Builder());
        this.powerPlug_2 = new PowerPlugController(COMPONENT_POWER_PLUG_2, this, builder.getPowerPlug2Builder());
        this.powerPlug_3 = new PowerPlugController(COMPONENT_POWER_PLUG_3, this, builder.getPowerPlug3Builder());
        this.powerPlug_4 = new PowerPlugController(COMPONENT_POWER_PLUG_4, this, builder.getPowerPlug4Builder());
        this.powerPlug_5 = new PowerPlugController(COMPONENT_POWER_PLUG_5, this, builder.getPowerPlug5Builder());
        this.powerConsumptionSensor_0 = new PowerConsumptionSensorController(COMPONENT_POWER_CONSUMPTION_SENSOR+"_0", this, builder.getPowerConsumptionSensor0Builder());
        this.powerConsumptionSensor_1 = new PowerConsumptionSensorController(COMPONENT_POWER_CONSUMPTION_SENSOR+"_1", this, builder.getPowerConsumptionSensor1Builder());
        this.powerConsumptionSensor_2 = new PowerConsumptionSensorController(COMPONENT_POWER_CONSUMPTION_SENSOR+"_2", this, builder.getPowerConsumptionSensor2Builder());
        this.powerConsumptionSensor_3 = new PowerConsumptionSensorController(COMPONENT_POWER_CONSUMPTION_SENSOR+"_3", this, builder.getPowerConsumptionSensor3Builder());
        this.powerConsumptionSensor_4 = new PowerConsumptionSensorController(COMPONENT_POWER_CONSUMPTION_SENSOR+"_4", this, builder.getPowerConsumptionSensor4Builder());
        this.powerConsumptionSensor_5 = new PowerConsumptionSensorController(COMPONENT_POWER_CONSUMPTION_SENSOR+"_5", this, builder.getPowerConsumptionSensor5Builder());
        this.register(powerPlug_0);
        this.register(powerPlug_1);
        this.register(powerPlug_2);
        this.register(powerPlug_3);
        this.register(powerPlug_4);
        this.register(powerPlug_5);
        this.register(powerConsumptionSensor_0);
        this.register(powerConsumptionSensor_1);
        this.register(powerConsumptionSensor_2);
        this.register(powerConsumptionSensor_3);
        this.register(powerConsumptionSensor_4);
        this.register(powerConsumptionSensor_5);
    }

    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(COMPONENT_POWER_PLUG_0, getClass().getMethod("updatePowerPlug_0", OnOffType.class));
        halFunctionMapping.put(COMPONENT_POWER_PLUG_1, getClass().getMethod("updatePowerPlug_1", OnOffType.class));
        halFunctionMapping.put(COMPONENT_POWER_PLUG_2, getClass().getMethod("updatePowerPlug_2", OnOffType.class));
        halFunctionMapping.put(COMPONENT_POWER_PLUG_3, getClass().getMethod("updatePowerPlug_3", OnOffType.class));
        halFunctionMapping.put(COMPONENT_POWER_PLUG_4, getClass().getMethod("updatePowerPlug_4", OnOffType.class));
        halFunctionMapping.put(COMPONENT_POWER_PLUG_5, getClass().getMethod("updatePowerPlug_5", OnOffType.class));
        halFunctionMapping.put(COMPONENT_POWER_CONSUMPTION_SENSOR+"_0", getClass().getMethod("updatePowerConsumption_0", DecimalType.class));
        halFunctionMapping.put(COMPONENT_POWER_CONSUMPTION_SENSOR+"_1", getClass().getMethod("updatePowerConsumption_1", DecimalType.class));
        halFunctionMapping.put(COMPONENT_POWER_CONSUMPTION_SENSOR+"_2", getClass().getMethod("updatePowerConsumption_2", DecimalType.class));
        halFunctionMapping.put(COMPONENT_POWER_CONSUMPTION_SENSOR+"_3", getClass().getMethod("updatePowerConsumption_3", DecimalType.class));
        halFunctionMapping.put(COMPONENT_POWER_CONSUMPTION_SENSOR+"_4", getClass().getMethod("updatePowerConsumption_4", DecimalType.class));
        halFunctionMapping.put(COMPONENT_POWER_CONSUMPTION_SENSOR+"_5", getClass().getMethod("updatePowerConsumption_5", DecimalType.class));
    }

    public void updatePowerPlug_0(OnOffType type) throws RSBBindingException {
        try {
            powerPlug_0.updatePowerState(PowerStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from OnOffType to PowerState!", ex);
        }
    }
    public void updatePowerPlug_1(OnOffType type) throws RSBBindingException {
        try {
            powerPlug_1.updatePowerState(PowerStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from OnOffType to PowerState!", ex);
        }
    }
    public void updatePowerPlug_2(OnOffType type) throws RSBBindingException {
        try {
            powerPlug_2.updatePowerState(PowerStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from OnOffType to PowerState!", ex);
        }
    }
    public void updatePowerPlug_3(OnOffType type) throws RSBBindingException {
        try {
            powerPlug_3.updatePowerState(PowerStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from OnOffType to PowerState!", ex);
        }
    }
    public void updatePowerPlug_4(OnOffType type) throws RSBBindingException {
        try {
            powerPlug_4.updatePowerState(PowerStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from OnOffType to PowerState!", ex);
        }
    }
    public void updatePowerPlug_5(OnOffType type) throws RSBBindingException {
        try {
            powerPlug_5.updatePowerState(PowerStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from OnOffType to PowerState!", ex);
        }
    }
    
    public void updatePowerConsumptionSensor_0(DecimalType value) {
        powerConsumptionSensor_0.updatePowerConsumption(value.floatValue());
    }
    
    public void updatePowerConsumptionSensor_1(DecimalType value) {
        powerConsumptionSensor_1.updatePowerConsumption(value.floatValue());
    }
    
    public void updatePowerConsumptionSensor_2(DecimalType value) {
        powerConsumptionSensor_2.updatePowerConsumption(value.floatValue());
    }
    
    public void updatePowerConsumptionSensor_3(DecimalType value) {
        powerConsumptionSensor_3.updatePowerConsumption(value.floatValue());
    }
    
    public void updatePowerConsumptionSensor_4(DecimalType value) {
        powerConsumptionSensor_4.updatePowerConsumption(value.floatValue());
    }
    
    public void updatePowerConsumptionSensor_5(DecimalType value) {
        powerConsumptionSensor_5.updatePowerConsumption(value.floatValue());
    }
}
