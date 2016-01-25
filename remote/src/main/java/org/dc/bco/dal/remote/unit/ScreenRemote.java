/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.unit;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dc.bco.dal.lib.layer.unit.ScreenInterface;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.state.StandbyStateType;
import rst.homeautomation.state.StandbyStateType.StandbyState;
import rst.homeautomation.unit.ScreenType;

/**
 *
 * @author mpohling
 */
public class ScreenRemote extends AbstractUnitRemote<ScreenType.Screen> implements ScreenInterface {

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
        return getData().getPowerState();
    }

    @Override
    public void setPower(PowerState.State state) throws CouldNotPerformException {
        try {
            RPCHelper.callRemoteMethod(PowerState.newBuilder().setValue(state).build(), this).get();
        } catch (InterruptedException ex) {
            Logger.getLogger(ScreenRemote.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(ScreenRemote.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public StandbyStateType.StandbyState getStandby() throws CouldNotPerformException {
        return getData().getStandbyState();
    }

    @Override
    public void setStandby(StandbyStateType.StandbyState.State value) throws CouldNotPerformException {
        try {
            RPCHelper.callRemoteMethod(StandbyState.newBuilder().setValue(value).build(), this).get();
        } catch (InterruptedException ex) {
            Logger.getLogger(ScreenRemote.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(ScreenRemote.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
