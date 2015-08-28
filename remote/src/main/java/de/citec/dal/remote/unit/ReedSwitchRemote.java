/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.ReedSwitchInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.ReedSwitchStateType.ReedSwitchState;
import rst.homeautomation.unit.ReedSwitchType.ReedSwitch;

/**
 *
 * @author thuxohl
 */
public class ReedSwitchRemote extends DALRemoteService<ReedSwitch> implements ReedSwitchInterface {
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ReedSwitch.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ReedSwitchState.getDefaultInstance()));
    }

    public ReedSwitchRemote() {
    }

    @Override
    public void notifyUpdated(ReedSwitch data) {
    }

    @Override
    public ReedSwitchState getReedSwitch() throws CouldNotPerformException {
        return getData().getReedSwitchState();
    }
     
}
