/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.unit;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dc.bco.dal.lib.layer.unit.DimmerInterface;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.DimmerType.Dimmer;

/**
 *
 * @author thuxohl
 */
public class DimmerRemote extends AbstractUnitRemote<Dimmer> implements DimmerInterface {

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
    public void setPower(PowerState.State value) throws CouldNotPerformException {
        try {
            RPCHelper.callRemoteMethod(PowerState.newBuilder().setValue(value).build(), this).get();
        } catch (InterruptedException ex) {
            Logger.getLogger(AmbientLightRemote.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(AmbientLightRemote.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public PowerState getPower() throws CouldNotPerformException {
        return getData().getPowerState();
    }

    @Override
    public void setDim(Double value) throws CouldNotPerformException {
        try {
            RPCHelper.callRemoteMethod(value, this).get();
        } catch (InterruptedException ex) {
            Logger.getLogger(DimmerRemote.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(DimmerRemote.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Double getDim() throws CouldNotPerformException {
        return getData().getValue();
    }

}
