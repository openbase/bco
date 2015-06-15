/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.ScreenInterface;
import de.citec.jul.exception.CouldNotPerformException;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.unit.ScreenType;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.state.StandbyStateType;
import rst.homeautomation.state.StandbyStateType.StandbyState;

/**
 *
 * @author mpohling
 */
public class ScreenRemote extends DALRemoteService<ScreenType.Screen> implements ScreenInterface {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ScreenRemote.class);

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ScreenType.Screen.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(StandbyState.getDefaultInstance()));
    }

    public ScreenRemote() {
    }

    @Override
    public void notifyUpdated(ScreenType.Screen data) {
    }

    @Override
    public PowerState getPower() throws CouldNotPerformException {
        return this.getData().getPowerState();
    }

    @Override
    public void setPower(PowerState.State state) throws CouldNotPerformException {
        callMethod("setPower", PowerState.newBuilder().setValue(state).build());
    }

    @Override
    public StandbyStateType.StandbyState getStandby() throws CouldNotPerformException {
        return this.getData().getStandbyState();
    }

    @Override
    public void setStandby(StandbyStateType.StandbyState.State state) throws CouldNotPerformException {
        callMethod("setStandby", StandbyState.newBuilder().setValue(state).build());
    }
}
