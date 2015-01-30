/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.jul.rsb.RSBRemoteService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.MotionSensorType;

/**
 *
 * @author thuxohl
 */
public class MotionSenorRemote extends RSBRemoteService<MotionSensorType.MotionSensor> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionSensorType.MotionSensor.getDefaultInstance()));
    }

    public MotionSenorRemote() {
    }

    @Override
    public void notifyUpdated(MotionSensorType.MotionSensor data) {
    }

}
