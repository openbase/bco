/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.service.rsb.RSBRemoteService;
import de.citec.dal.util.DALException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.LightType;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author thuxohl
 */
public class LightRemote extends RSBRemoteService<LightType.Light> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LightType.Light.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }

    public LightRemote() {
    }

    public void setPowerState(final PowerType.Power.PowerState state) throws DALException {
        callMethodAsync("setPowerState", state);
    }

    @Override
    public void notifyUpdated(LightType.Light data) {
    }
}
