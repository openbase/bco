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
import rst.homeautomation.state.ShutterType;
import rst.homeautomation.unit.RollershutterType;

/**
 *
 * @author thuxohl
 */
public class RollershutterRemote extends DALRemoteService<RollershutterType.Rollershutter> implements RollershutterInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RollershutterType.Rollershutter.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ShutterType.Shutter.getDefaultInstance()));
    }

    public RollershutterRemote() {
    }

    @Override
    public void notifyUpdated(RollershutterType.Rollershutter data) {
    }

    @Override
    public void setShutter(ShutterType.Shutter.ShutterState state) throws CouldNotPerformException {
        callMethodAsync("setShutter", ShutterType.Shutter.newBuilder().setState(state).build());
    }

    @Override
    public ShutterType.Shutter.ShutterState getShutter() throws CouldNotPerformException {
        return this.getData().getShutterState().getState();
    }

    @Override
    public void setOpeningRatio(final Double openingRatio) throws CouldNotPerformException {
        callMethodAsync("setOpeningRatio", openingRatio);
    }

    @Override
    public Double getOpeningRatio() throws CouldNotPerformException {
        return this.getData().getOpeningRatio();
    }

}
