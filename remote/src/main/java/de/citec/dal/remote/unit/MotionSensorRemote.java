/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.MotionSensorInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.MotionStateType.MotionState;
import rst.homeautomation.unit.MotionSensorType.MotionSensor;

/**
 *
 * @author thuxohl
 */
public class MotionSensorRemote extends DALRemoteService<MotionSensor> implements MotionSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionSensor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionState.getDefaultInstance()));
    }

    public MotionSensorRemote() {
    }

    @Override
    public void notifyUpdated(MotionSensor data) {
    }

    @Override
    public MotionState getMotion() throws CouldNotPerformException {
        return this.getData().getMotionState();
    }

}
