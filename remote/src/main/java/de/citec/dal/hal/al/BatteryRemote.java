/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.service.rsb.RSBRemoteService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.BatteryType;

/**
 *
 * @author thuxohl
 */
public class BatteryRemote extends RSBRemoteService<BatteryType.Battery>{

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BatteryType.Battery.getDefaultInstance()));
    }
    
    public BatteryRemote() {
        
    }

    @Override
    public void notifyUpdated(BatteryType.Battery data) {

    }
}
