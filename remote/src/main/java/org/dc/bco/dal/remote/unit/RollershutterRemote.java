/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.unit;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dc.bco.dal.lib.layer.unit.RollershutterInterface;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.ShutterStateType.ShutterState;
import rst.homeautomation.unit.RollershutterType.Rollershutter;

/**
 *
 * @author thuxohl
 */
public class RollershutterRemote extends AbstractUnitRemote<Rollershutter> implements RollershutterInterface {

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
    public void setShutter(ShutterState.State value) throws CouldNotPerformException {
        try {
            RPCHelper.callRemoteMethod(ShutterState.newBuilder().setValue(value).build(), this).get();
        } catch (InterruptedException ex) {
            Logger.getLogger(RollershutterRemote.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(RollershutterRemote.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public ShutterState getShutter() throws CouldNotPerformException {
        return getData().getShutterState();
    }

    @Override
    public void setOpeningRatio(final Double value) throws CouldNotPerformException {
        try {
            RPCHelper.callRemoteMethod(value, this).get();
        } catch (InterruptedException ex) {
            Logger.getLogger(RollershutterRemote.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(RollershutterRemote.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Double getOpeningRatio() throws CouldNotPerformException {
        return getData().getOpeningRatio();
    }

}
