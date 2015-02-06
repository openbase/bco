/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.hal.unit.MotionSensorInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.RSBRemoteService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.MotionSensorType;
import rst.homeautomation.states.MotionType;

/**
 *
 * @author thuxohl
 */
public class MotionSenorRemote extends RSBRemoteService<MotionSensorType.MotionSensor> implements MotionSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionSensorType.MotionSensor.getDefaultInstance()));
    }

    public MotionSenorRemote() {
    }

    @Override
    public void notifyUpdated(MotionSensorType.MotionSensor data) {
    }

    @Override
    public MotionType.Motion.MotionState getMotionState() throws CouldNotPerformException {
        return this.getData().getMotionState().getState();
    }

}
