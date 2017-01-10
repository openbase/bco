package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
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
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.AlarmStateType.AlarmState;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.unit.dal.TemperatureSensorDataType.TemperatureSensorData;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TemperatureSensorController extends AbstractUnitController<TemperatureSensorData, TemperatureSensorData.Builder> implements TemperatureSensor {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureSensorData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureState.getDefaultInstance()));
    }

    public TemperatureSensorController(final UnitHost unitHost, final TemperatureSensorData.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(TemperatureSensorController.class, unitHost, builder);
    }

    public void updateTemperatureStateProvider(final TemperatureState temperatureState) throws CouldNotPerformException {
        logger.debug("Apply temperatureState Update[" + temperatureState + "] for " + this + ".");

        try (ClosableDataBuilder<TemperatureSensorData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setTemperatureState(temperatureState);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply temperatureState Update[" + temperatureState + "] for " + this + "!", ex);
        }
    }

    @Override
    public TemperatureState getTemperatureState() throws NotAvailableException {
        try {
            return getData().getTemperatureState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("temperatureState", ex);
        }
    }

    public void updateTemperatureAlarmStateProvider(final AlarmState value) throws CouldNotPerformException {
        logger.debug("Apply temperatureAlarmState Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<TemperatureSensorData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setTemperatureAlarmState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not temperatureAlarmState Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public AlarmState getTemperatureAlarmState() throws NotAvailableException {
        try {
            return getData().getTemperatureAlarmState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("temperatureAlarmState", ex);
        }
    }
}
