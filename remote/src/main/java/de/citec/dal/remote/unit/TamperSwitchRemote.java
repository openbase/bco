/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.TamperSwitchInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.TamperType;
import rst.homeautomation.unit.TamperSwitchType;

/**
 *
 * @author thuxohl
 */
public class TamperSwitchRemote extends DALRemoteService<TamperSwitchType.TamperSwitch> implements TamperSwitchInterface {
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperSwitchType.TamperSwitch.getDefaultInstance()));
    }

    public TamperSwitchRemote() {
    }

    @Override
    public void notifyUpdated(TamperSwitchType.TamperSwitch data) {
    }

    @Override
    public TamperType.Tamper getTamper() throws CouldNotPerformException {
        return this.getData().getTamperState();
    }
    
}
