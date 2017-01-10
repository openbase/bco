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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.AlarmStateType.AlarmState;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.unit.dal.TemperatureSensorDataType.TemperatureSensorData;
import org.openbase.bco.dal.lib.layer.unit.TemperatureSensor;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TemperatureSensorRemote extends AbstractUnitRemote<TemperatureSensorData> implements TemperatureSensor {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureSensorData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmState.getDefaultInstance()));
    }

    public TemperatureSensorRemote() {
        super(TemperatureSensorData.class);
    }

    @Override
    public void notifyDataUpdate(TemperatureSensorData data) {
    }

    @Override
    public TemperatureState getTemperatureState() throws NotAvailableException {
        try {
            return getData().getTemperatureState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("TemperatureState", ex);
        }
    }

    @Override
    public AlarmState getTemperatureAlarmState() throws NotAvailableException {
        try {
            return getData().getTemperatureAlarmState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("TemperatureAlarmStateState", ex);
        }
    }

}
