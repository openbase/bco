/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.unit;

/*
 * #%L
 * DAL Library
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


import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.AlarmStateType.AlarmState;
import rst.homeautomation.unit.TemperatureSensorType.TemperatureSensor;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class TemperatureSensorController extends AbstractUnitController<TemperatureSensor, TemperatureSensor.Builder> implements TemperatureSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureSensor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmState.getDefaultInstance()));
    }

    public TemperatureSensorController(final UnitConfigType.UnitConfig config, final UnitHost unitHost, final TemperatureSensor.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(config, TemperatureSensorController.class, unitHost, builder);
    }

    public void updateTemperature(final Double value) throws CouldNotPerformException {
        logger.debug("Apply temperature Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<TemperatureSensor.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setTemperature(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply temperature Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public Double getTemperature() throws NotAvailableException {
        try {
            return getData().getTemperature();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("temperature", ex);
        }
    }

    public void updateTemperatureAlarmState(final AlarmState value) throws CouldNotPerformException {
        logger.debug("Apply alarm state Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<TemperatureSensor.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setTemperatureAlarmState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not alarm state Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public AlarmState getTemperatureAlarmState() throws CouldNotPerformException {
        try {
            return getData().getTemperatureAlarmState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("temperaturealarmstate", ex);
        }
    }
}
