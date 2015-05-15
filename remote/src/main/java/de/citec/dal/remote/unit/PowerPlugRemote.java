/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.PowerPlugInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.PowerPlugType.PowerPlug;

/**
 *
 * @author thuxohl
 */
public class PowerPlugRemote extends DALRemoteService<PowerPlug> implements PowerPlugInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerPlug.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
    }

    public PowerPlugRemote() {
    }

    @Override
    public void notifyUpdated(PowerPlug data) {
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
