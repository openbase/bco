/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.hal.unit.TamperSwitchInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.RSBRemoteService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.TamperSwitchType;
import rst.homeautomation.states.TamperType;

/**
 *
 * @author thuxohl
 */
public class TamperSwtichRemote extends RSBRemoteService<TamperSwitchType.TamperSwitch> implements TamperSwitchInterface {
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperSwitchType.TamperSwitch.getDefaultInstance()));
    }

    public TamperSwtichRemote() {
    }

    @Override
    public void notifyUpdated(TamperSwitchType.TamperSwitch data) {
    }

    @Override
    public TamperType.Tamper.TamperState getTamperState() throws CouldNotPerformException {
        return this.getData().getTamperState().getState();
    }
    
}
