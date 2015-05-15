/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.LightInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.LightType.Light;

/**
 *
 * @author thuxohl
 */
public class LightRemote extends DALRemoteService<Light> implements LightInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Light.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
    }

    public LightRemote() {
    }

    @Override
    public void notifyUpdated(Light data) {
    }

    @Override
    public void setPower(PowerState.State state) throws CouldNotPerformException {
        callMethod("setPower", PowerState.newBuilder().setValue(state).build());
    }

    @Override
    public PowerState getPower() throws CouldNotPerformException {
        return this.getData().getPowerState();
    }
}
