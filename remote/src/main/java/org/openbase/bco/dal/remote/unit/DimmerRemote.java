package org.openbase.bco.dal.remote.unit;

import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.IntensityStateType.IntensityState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.dal.DimmerDataType.DimmerData;
import org.openbase.bco.dal.lib.layer.unit.Dimmer;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DimmerRemote extends AbstractUnitRemote<DimmerData> implements Dimmer {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DimmerData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(IntensityState.getDefaultInstance()));
    }

    public DimmerRemote() {
        super(DimmerData.class);
    }

    @Override
    public Future<Void> setPowerState(PowerState powerState) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(powerState, this, Void.class);
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PowerState", ex);
        }
    }

    @Override
    public Future<Void> setIntensityState(IntensityState intensityState) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(intensityState, this, Void.class);
    }

    @Override
    public IntensityState getIntensityState() throws NotAvailableException {
        try {
            return getData().getIntensityState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("IntensityState", ex);
        }
    }
}
