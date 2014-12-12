/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.service.rsb.RSBRemoteService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.TamperSwitchType;

/**
 *
 * @author thuxohl
 */
public class TamperSwtichRemote extends RSBRemoteService<TamperSwitchType.TamperSwitch> {
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperSwitchType.TamperSwitch.getDefaultInstance()));
    }

    public TamperSwtichRemote() {
    }

    @Override
    public void notifyUpdated(TamperSwitchType.TamperSwitch data) {
    }
    
}
