/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.PowerPlugInterface;
import de.citec.dal.hal.unit.PowerPlugInterface;
import de.citec.jul.rsb.RSBRemoteService;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.PowerPlugType;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author thuxohl
 */
public class PowerPlugRemote extends RSBRemoteService<PowerPlugType.PowerPlug> implements PowerPlugInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerPlugType.PowerPlug.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }

    public PowerPlugRemote() {
    }

    @Override
    public void notifyUpdated(PowerPlugType.PowerPlug data) {
    }

    @Override
    public void setPower(PowerType.Power.PowerState state) throws CouldNotPerformException {
        callMethodAsync("setPower", PowerType.Power.newBuilder().setState(state).build());
    }

    @Override
    public PowerType.Power.PowerState getPower() throws CouldNotPerformException {
        return this.getData().getPowerState().getState();
    }

}
