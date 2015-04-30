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
import rst.homeautomation.state.MotionType;
import rst.homeautomation.unit.MotionSensorType;

/**
 *
 * @author thuxohl
 */
public class MotionSensorRemote extends DALRemoteService<MotionSensorType.MotionSensor> implements MotionSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionSensorType.MotionSensor.getDefaultInstance()));
    }

    public MotionSensorRemote() {
    }

    @Override
    public void notifyUpdated(MotionSensorType.MotionSensor data) {
    }

    @Override
    public MotionType.Motion getMotion() throws CouldNotPerformException {
        return this.getData().getMotionState();
    }

}
