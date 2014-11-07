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
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.dal.hal.al.BatteryController;
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
public class F_MotionSensorController extends AbstractDeviceController<F_MotionSensor, F_MotionSensor.Builder> {

    private final static String UNIT_MOTION_SENSOR = "MotionSensor";
    private final static String UNIT_TEMPERATURE_SENSOR = "TemperatureSensor";
    private final static String UNIT_BRIGHTNESS_SENSOR = "BrightnessSensor";
    private final static String UNIT_TAMPER_SWITCH = "TamperSwitch";
    private final static String UNIT_BATTERY = "Battery";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(F_MotionSensorType.F_MotionSensor.getDefaultInstance()));
    }

    private final MotionSensorController motionSensor;
    private final TemperatureSensorController temperatureSensor;
    private final BrightnessSensorController brightnessSensor;
    private final TamperSwitchController tamperSwitch;
    private final BatteryController battery;

    public F_MotionSensorController(final String id, String label, final Location location) throws RSBBindingException {
        super(id, label, location, F_MotionSensor.newBuilder());

        builder.setId(id);
        this.motionSensor = new MotionSensorController(UNIT_MOTION_SENSOR, label, this, builder.getMotionSensorBuilder());
        this.temperatureSensor = new TemperatureSensorController(UNIT_TEMPERATURE_SENSOR, label, this, builder.getTemperatureSensorBuilder());
        this.brightnessSensor = new BrightnessSensorController(UNIT_BRIGHTNESS_SENSOR, label, this, builder.getBrightnessSensorBuilder());
        this.tamperSwitch = new TamperSwitchController(UNIT_TAMPER_SWITCH, label, this, builder.getTamperSwitchBuilder());
        this.battery = new BatteryController(UNIT_BATTERY, label, this, builder.getBatteryBuilder());
        this.register(motionSensor);
        this.register(temperatureSensor);
        this.register(brightnessSensor);
        this.register(tamperSwitch);
        this.register(battery);
    }

    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(UNIT_MOTION_SENSOR, getClass().getMethod("updateMotionSensor", DecimalType.class));
        halFunctionMapping.put(UNIT_TEMPERATURE_SENSOR, getClass().getMethod("updateTemperature", DecimalType.class));
        halFunctionMapping.put(UNIT_BRIGHTNESS_SENSOR, getClass().getMethod("updateBrightness", DecimalType.class));
        halFunctionMapping.put(UNIT_TAMPER_SWITCH, getClass().getMethod("updateTamperSwitch", DecimalType.class));
        halFunctionMapping.put(UNIT_BATTERY, getClass().getMethod("updateBatteryLevel", DecimalType.class));
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

    public void updateBatteryLevel(DecimalType value) {
        battery.updateBatteryLevel(value.doubleValue());
    }
}
