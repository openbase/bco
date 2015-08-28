/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.PowerPlugInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.extension.rsb.com.RPCHelper;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    public void setPower(PowerState.State value) throws CouldNotPerformException {
        try {
            RPCHelper.callRemoteMethod(PowerState.newBuilder().setValue(value).build(), this).get();
        } catch (InterruptedException ex) {
            Logger.getLogger(PowerPlugRemote.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(PowerPlugRemote.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public PowerState getPower() throws CouldNotPerformException {
        return getData().getPowerState();
    }

}
