/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.jul.rsb.RSBRemoteService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.TemperatureSensorType;

/**
 *
 * @author thuxohl
 */
public class TemperatureSensorRemote extends RSBRemoteService<TemperatureSensorType.TemperatureSensor>{
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureSensorType.TemperatureSensor.getDefaultInstance()));
    }

    public TemperatureSensorRemote() {
    }

    @Override
    public void notifyUpdated(TemperatureSensorType.TemperatureSensor data) {
    }

}
