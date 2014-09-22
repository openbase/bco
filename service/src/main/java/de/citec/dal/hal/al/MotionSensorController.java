/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractHALController;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.MotionSensorType;
import rst.homeautomation.MotionSensorType.MotionSensor;
import rst.homeautomation.states.MotionType;

/**
 *
 * @author mpohling
 */
public class MotionSensorController extends AbstractHALController<MotionSensor, MotionSensor.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(MotionSensorType.MotionSensor.getDefaultInstance()));
    }
    
	public MotionSensorController(String id, HardwareUnit hardwareUnit, MotionSensor.Builder builder) throws RSBBindingException {
		super(id, hardwareUnit, builder);
	}

	public void updateMotionState(final MotionType.Motion.MotionState state) {
		builder.getStateBuilder().setState(state);
		notifyChange();
	}
}
