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
import rst.homeautomation.state.PowerType;
import rst.homeautomation.unit.DimmerType;

/**
 *
 * @author thuxohl
 */
public class DimmerRemote extends DALRemoteService<DimmerType.Dimmer> implements DimmerInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DimmerType.Dimmer.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }

    public DimmerRemote() {
    }

    @Override
    public void notifyUpdated(DimmerType.Dimmer data) {
    }

    @Override
    public void setPower(PowerType.Power.PowerState state) throws CouldNotPerformException {
        callMethod("setPower", PowerType.Power.newBuilder().setState(state).build());
    }

    @Override
    public PowerType.Power.PowerState getPower() throws CouldNotPerformException {
        return this.getData().getPowerState().getState();
    }

    @Override
    public void setDimm(Double dimm) throws CouldNotPerformException {
        callMethod("setDimm", dimm);
    }

    @Override
    public Double getDimm() throws CouldNotPerformException {
        return this.getData().getValue();
    }

}
