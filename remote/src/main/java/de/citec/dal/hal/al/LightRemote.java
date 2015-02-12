/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.hal.unit.LightInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.RSBRemoteService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.LightType;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author thuxohl
 */
public class LightRemote extends RSBRemoteService<LightType.Light> implements LightInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LightType.Light.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }

    public LightRemote() {
    }

    @Override
    public void notifyUpdated(LightType.Light data) {
    }

    @Override
    public void setPower(PowerType.Power.PowerState state) throws CouldNotPerformException {
        callMethodAsync("setPowerState", PowerType.Power.newBuilder().setState(state).build());
    }

    @Override
    public PowerType.Power.PowerState getPowerState() throws CouldNotPerformException {
        return this.getData().getPowerState().getState();
    }
}
