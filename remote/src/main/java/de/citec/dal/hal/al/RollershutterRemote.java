/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.hal.unit.RollershutterInterface;
import de.citec.jul.rsb.RSBRemoteService;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.RollershutterType;
import rst.homeautomation.states.ShutterType;

/**
 *
 * @author thuxohl
 */
public class RollershutterRemote extends RSBRemoteService<RollershutterType.Rollershutter> implements RollershutterInterface {

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
        callMethodAsync("setShutterState", ShutterType.Shutter.newBuilder().setState(state).build());
    }

    @Override
    public ShutterType.Shutter.ShutterState getShutter() throws CouldNotPerformException {
        return this.getData().getShutterState().getState();
    }

    @Override
    public void setOpeningRatio(double openingRatio) throws CouldNotPerformException {
        callMethodAsync("setPosition", new Double(openingRatio));
    }

    @Override
    public double getOpeningRatio() throws CouldNotPerformException {
        return this.getData().getOpeningRatio();
    }

}
