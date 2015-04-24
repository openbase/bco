/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.fibaro;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import de.citec.jul.exception.InstantiationException;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.fibaro.F_MotionSensorType;
import rst.homeautomation.device.fibaro.F_MotionSensorType.F_MotionSensor;

/**
 *
 * @author mpohling
 */
public class F_MotionSensorController extends AbstractOpenHABDeviceController<F_MotionSensor, F_MotionSensor.Builder> {

	static {
		DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(F_MotionSensorType.F_MotionSensor.getDefaultInstance()));
	}

	public F_MotionSensorController(final DeviceConfigType.DeviceConfig config) throws InstantiationException, CouldNotTransformException {
		super(config, F_MotionSensor.newBuilder());
		try {
			registerUnits(config.getUnitConfigList());
		} catch (CouldNotPerformException ex) {
			throw new InstantiationException(this, ex);
		}
	}
}
