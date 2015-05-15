/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.RollershutterInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.ShutterStateType.ShutterState;
import rst.homeautomation.unit.RollershutterType.Rollershutter;

/**
 *
 * @author thuxohl
 */
public class RollershutterRemote extends DALRemoteService<Rollershutter> implements RollershutterInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Rollershutter.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ShutterState.getDefaultInstance()));
    }

    public RollershutterRemote() {
    }

    @Override
    public void notifyUpdated(Rollershutter data) {
    }

    @Override
    public void setShutter(ShutterState.State state) throws CouldNotPerformException {
        callMethod("setShutter", ShutterState.newBuilder().setValue(state).build());
    }

    @Override
    public ShutterState getShutter() throws CouldNotPerformException {
        return this.getData().getShutterState();
    }

    @Override
    public void setOpeningRatio(final Double openingRatio) throws CouldNotPerformException {
        callMethod("setOpeningRatio", openingRatio);
    }

    @Override
    public Double getOpeningRatio() throws CouldNotPerformException {
        return this.getData().getOpeningRatio();
    }

}
