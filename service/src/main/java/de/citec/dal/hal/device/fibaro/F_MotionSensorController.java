/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.fibaro;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.BatteryController;
import de.citec.dal.hal.unit.BrightnessSensorController;
import de.citec.dal.hal.unit.MotionSensorController;
import de.citec.dal.hal.unit.TamperSwitchController;
import de.citec.dal.hal.unit.TemperatureSensorController;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.fibaro.F_MotionSensorType;
import rst.devices.fibaro.F_MotionSensorType.F_MotionSensor;
import de.citec.jul.exception.InstantiationException;

/**
 *
 * @author mpohling
 */
public class F_MotionSensorController extends AbstractOpenHABDeviceController<F_MotionSensor, F_MotionSensor.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(F_MotionSensorType.F_MotionSensor.getDefaultInstance()));
    }

    private final MotionSensorController motionSensor;
    private final TemperatureSensorController temperatureSensor;
    private final BrightnessSensorController brightnessSensor;
    private final TamperSwitchController tamperSwitch;
    private final BatteryController battery;

    public F_MotionSensorController(final String id, String label, final Location location) throws InstantiationException {
        super(id, label, location, F_MotionSensor.newBuilder());

        data.setId(id);
        this.motionSensor = new MotionSensorController(label, this, data.getMotionSensorBuilder());
        this.temperatureSensor = new TemperatureSensorController(label, this, data.getTemperatureSensorBuilder());
        this.brightnessSensor = new BrightnessSensorController(label, this, data.getBrightnessSensorBuilder());
        this.tamperSwitch = new TamperSwitchController(label, this, data.getTamperSwitchBuilder());
        this.battery = new BatteryController(label, this, data.getBatteryBuilder());
        this.registerUnit(motionSensor);
        this.registerUnit(temperatureSensor);
        this.registerUnit(brightnessSensor);
        this.registerUnit(tamperSwitch);
        this.registerUnit(battery);
    }

//    public void updateMotionSensor(double type) throws CouldNotPerformException {
//        try {
//            motionSensor.updateMotionState(MotionStateTransformer.transform(type));
//        } catch (CouldNotTransformException ex) {
//            throw new CouldNotPerformException("Could not updateMotionSensor!", ex);
//        }
//    }
//
//    public void updateTemperature(double type) {
//        temperatureSensor.updateTemperature((float) type);
//    }
//
//    public void updateBrightness(double type) {
//        brightnessSensor.updateBrightness((float) type);
//    }
//
//    public void updateTamperSwitch(double type) throws CouldNotPerformException {
//        try {
//            tamperSwitch.updateTamperState(TamperStateTransformer.transform(type));
//        } catch (CouldNotTransformException ex) {
//            throw new CouldNotPerformException("Could not updateTamperSwitch!", ex);
//        }
//    }
//
//    public void updateBatteryLevel(double value) {
//        battery.updateBatteryLevel(value);
//    }
}
