/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.unit;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dc.bco.dal.lib.layer.unit.TemperatureControllerInterface;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.unit.TemperatureControllerType.TemperatureController;

/**
 *
 * @author mpohling
 */
public class TemperatureControllerRemote extends AbstractUnitRemote<TemperatureController> implements TemperatureControllerInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureController.getDefaultInstance()));
    }

    public TemperatureControllerRemote() {
    }

    @Override
    public void notifyUpdated(TemperatureController data) throws CouldNotPerformException {
    }

    @Override
    public Double getTemperature() throws CouldNotPerformException {
        return getData().getActualTemperature();
    }

    @Override
    public void setTargetTemperature(Double value) throws CouldNotPerformException {
        try {
            RPCHelper.callRemoteMethod(value, this).get();
        } catch (InterruptedException ex) {
            Logger.getLogger(TemperatureControllerRemote.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(TemperatureControllerRemote.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Double getTargetTemperature() throws CouldNotPerformException {
        return getData().getTargetTemperature();
    }
}
