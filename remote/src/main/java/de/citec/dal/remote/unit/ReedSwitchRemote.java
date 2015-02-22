/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.ReedSwitchInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.RSBRemoteService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.OpenClosedType;
import rst.homeautomation.unit.ReedSwitchType;

/**
 *
 * @author thuxohl
 */
public class ReedSwitchRemote extends RSBRemoteService<ReedSwitchType.ReedSwitch> implements ReedSwitchInterface {
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ReedSwitchType.ReedSwitch.getDefaultInstance()));
    }

    public ReedSwitchRemote() {
    }

    @Override
    public void notifyUpdated(ReedSwitchType.ReedSwitch data) {
    }

    @Override
    public OpenClosedType.OpenClosed.OpenClosedState getReedSwitch() throws CouldNotPerformException {
        return this.getData().getReedSwitchState().getState();
    }
     
}
