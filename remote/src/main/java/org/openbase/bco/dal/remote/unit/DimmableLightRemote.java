package org.openbase.bco.dal.remote.unit;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.dal.DimmableLightDataType.DimmableLightData;
import org.openbase.bco.dal.lib.layer.unit.DimmableLight;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DimmableLightRemote extends AbstractUnitRemote<DimmableLightData> implements DimmableLight {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DimmableLightData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessState.getDefaultInstance()));
    }

    public DimmableLightRemote() {
        super(DimmableLightData.class);
    }

    @Override
    public void notifyDataUpdate(DimmableLightData data) {
    }

    public Future<Void> setPowerState(final PowerState.State value) throws CouldNotPerformException {
        return DimmableLightRemote.this.setPowerState(PowerState.newBuilder().setValue(value).build());
    }

    @Override
    public Future<Void> setPowerState(final PowerState value) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(value, this, Void.class);
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
    public Future<Void> setBrightnessState(BrightnessState brightness) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(brightness, this, Void.class);
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        try {
            return getData().getBrightnessState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("BrightnessState", ex);
        }
    }

}
