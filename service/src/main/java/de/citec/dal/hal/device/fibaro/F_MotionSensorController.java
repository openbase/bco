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

    public F_MotionSensorController(final String id, String label, final Location location) throws InstantiationException {
        super(id, label, location, F_MotionSensor.newBuilder());
        registerUnit(new MotionSensorController(label, this, data.getMotionSensorBuilder()));
        registerUnit(new TemperatureSensorController(label, this, data.getTemperatureSensorBuilder()));
        registerUnit(new BrightnessSensorController(label, this, data.getBrightnessSensorBuilder()));
        registerUnit(new TamperSwitchController(label, this, data.getTamperSwitchBuilder()));
        registerUnit(new BatteryController(label, this, data.getBatteryBuilder()));
    }
}
