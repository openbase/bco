/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.DimmerInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.DimmerType.Dimmer;

/**
 *
 * @author thuxohl
 */
public class DimmerRemote extends DALRemoteService<Dimmer> implements DimmerInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Dimmer.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
    }

    public DimmerRemote() {
    }

    @Override
    public void notifyUpdated(Dimmer data) {
    }

    @Override
    public void setPower(PowerState.State state) throws CouldNotPerformException {
        callMethod("setPower", PowerState.newBuilder().setValue(state).build());
    }

    @Override
    public PowerState getPower() throws CouldNotPerformException {
        return this.getData().getPowerState();
    }

    @Override
    public void setDim(Double dimm) throws CouldNotPerformException {
        callMethod("setDimm", dimm);
    }

    @Override
    public Double getDim() throws CouldNotPerformException {
        return this.getData().getValue();
    }

}
