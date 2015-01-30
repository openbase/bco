/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.jul.rsb.RSBRemoteService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.ReedSwitchType;

/**
 *
 * @author thuxohl
 */
public class ReedSwtichRemote extends RSBRemoteService<ReedSwitchType.ReedSwitch>{
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ReedSwitchType.ReedSwitch.getDefaultInstance()));
    }

    public ReedSwtichRemote() {
    }

    @Override
    public void notifyUpdated(ReedSwitchType.ReedSwitch data) {
    }
     
}
