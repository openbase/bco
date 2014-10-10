/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.devices.fibaro;

import de.citec.dal.data.Location;
import de.citec.dal.data.transform.MotionStateTransformer;
import de.citec.dal.data.transform.TamperStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractHardwareController;
import de.citec.dal.hal.al.BatteryStateController;
import de.citec.dal.hal.al.BrightnessSensorController;
import de.citec.dal.hal.al.MotionSensorController;
import de.citec.dal.hal.al.TamperSwitchController;
import de.citec.dal.hal.al.TemperatureSensorController;
import org.openhab.core.library.types.DecimalType;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.fibaro.F_MotionSensorType;
import rst.devices.fibaro.F_MotionSensorType.F_MotionSensor;

/**
 *
 * @author mpohling
 */
public class F_MotionSensorController extends AbstractHardwareController<F_MotionSensor, F_MotionSensor.Builder> {   
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(F_MotionSensorType.F_MotionSensor.getDefaultInstance()));
    }
    
    private final MotionSensorController motionSensor;
    private final TemperatureSensorController temperatureSensor;
    private final BrightnessSensorController brightnessSensor;
    private final TamperSwitchController tamperSwitch;
    private final BatteryStateController batteryState;
    
    public F_MotionSensorController(final String id, final Location location) throws RSBBindingException {
        super(id, location, F_MotionSensor.newBuilder());
        this.motionSensor = new MotionSensorController("MotionSensor", this, builder.getMotionSensorBuilder());
        this.temperatureSensor = new TemperatureSensorController("TemperatureSensor", this, builder.getTemperatureSensorBuilder());
        this.brightnessSensor = new BrightnessSensorController("BrightnessSensor", this, builder.getBrightnessSensorBuilder());
        this.tamperSwitch = new TamperSwitchController("TamperSwitch", this, builder.getTamperSwitchBuilder());
        this.batteryState = new BatteryStateController("BatteryState", this, builder.getBatteryStateBuilder());
        this.register(motionSensor);
        this.register(temperatureSensor);
        this.register(brightnessSensor);
        this.register(tamperSwitch);
        this.register(batteryState);
    }
    
    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put("MotionSensor", getClass().getMethod("updateMotionSensor", DecimalType.class));
        halFunctionMapping.put("TemperatureSensor", getClass().getMethod("updateTemperature", DecimalType.class));
        halFunctionMapping.put("BrightnessSensor", getClass().getMethod("updateBrightness", DecimalType.class));
        halFunctionMapping.put("TamperSwitch", getClass().getMethod("updateTamperSwitch", DecimalType.class));
        halFunctionMapping.put("BatteryState", getClass().getMethod("updateBatteryState", DecimalType.class));
    }
    
    public void updateMotionSensor(DecimalType type) {
        try {
            motionSensor.updateMotionState(MotionStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from DecimalType to MotionState!", ex);
        }
    }
    
    public void updateTemperature(DecimalType type) {
        temperatureSensor.updateTemperature(type.floatValue());
    }
    
    public void updateBrightness(DecimalType type) {
        brightnessSensor.updateBrightness(type.floatValue());
    }
    
    public void updateTamperSwitch(DecimalType type) {
        try {
            tamperSwitch.updateTamperState(TamperStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from DecimalType to TamperState!", ex);
        }
    }
    
    public void updateBatteryState(DecimalType value) {
        batteryState.updateBatteryLevel(value.floatValue());
    }
}
