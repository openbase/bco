package org.openbase.bco.dal.remote.unit;

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
import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.unit.dal.TemperatureControllerDataType.TemperatureControllerData;
import org.openbase.bco.dal.lib.layer.unit.TemperatureController;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class TemperatureControllerRemote extends AbstractUnitRemote<TemperatureControllerData> implements TemperatureController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureControllerData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureState.getDefaultInstance()));
    }

    public TemperatureControllerRemote() {
        super(TemperatureControllerData.class);
    }

    @Override
    public void notifyDataUpdate(TemperatureControllerData data) throws CouldNotPerformException {
    }

    @Override
    public TemperatureState getTemperatureState() throws NotAvailableException {
        try {
            return getData().getActualTemperatureState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("TemperatureState", ex);
        }
    }

    @Override
    public Future<Void> setTargetTemperatureState(TemperatureState value) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(value, this, Void.class);
    }

    @Override
    public TemperatureState getTargetTemperatureState() throws NotAvailableException {
        try {
            return getData().getTargetTemperatureState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("TargetTemperatureState", ex);
        }
    }
}
