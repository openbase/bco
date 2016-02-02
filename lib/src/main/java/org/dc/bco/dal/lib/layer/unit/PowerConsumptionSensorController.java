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
import rst.homeautomation.state.PowerConsumptionStateType.PowerConsumptionState;
import rst.homeautomation.unit.PowerConsumptionSensorType.PowerConsumptionSensor;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author mpohling
 */
public class PowerConsumptionSensorController extends AbstractUnitController<PowerConsumptionSensor, PowerConsumptionSensor.Builder> implements PowerConsumptionSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionSensor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionState.getDefaultInstance()));
    }

    public PowerConsumptionSensorController(final UnitConfigType.UnitConfig config, final UnitHost unitHost, final PowerConsumptionSensor.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(config, PowerConsumptionSensorController.class, unitHost, builder);
    }

    public void updatePowerConsumption(final PowerConsumptionState state) throws CouldNotPerformException {
        logger.debug("Apply power consumption Update[" + state + "] for " + this + ".");

        try (ClosableDataBuilder<PowerConsumptionSensor.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setPowerConsumptionState(state);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply power consumption Update[" + state + "] for " + this + "!", ex);
        }
    }

    @Override
    public PowerConsumptionState getPowerConsumption() throws NotAvailableException {
        try {
            return getData().getPowerConsumptionState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("power consumption", ex);
        }
    }
}
