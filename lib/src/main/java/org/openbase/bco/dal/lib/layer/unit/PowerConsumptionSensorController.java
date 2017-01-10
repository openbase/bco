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
import rst.domotic.state.PowerConsumptionStateType.PowerConsumptionState;
import rst.domotic.unit.dal.PowerConsumptionSensorDataType.PowerConsumptionSensorData;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PowerConsumptionSensorController extends AbstractUnitController<PowerConsumptionSensorData, PowerConsumptionSensorData.Builder> implements PowerConsumptionSensor {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionSensorData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionState.getDefaultInstance()));
    }

    public PowerConsumptionSensorController(final UnitHost unitHost, final PowerConsumptionSensorData.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(PowerConsumptionSensorController.class, unitHost, builder);
    }

    public void updatePowerConsumptionStateProvider(final PowerConsumptionState state) throws CouldNotPerformException {
        logger.debug("Apply powerConsumptionState Update[" + state + "] for " + this + ".");

        try (ClosableDataBuilder<PowerConsumptionSensorData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setPowerConsumptionState(state);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply powerConsumptionState Update[" + state + "] for " + this + "!", ex);
        }
    }

    @Override
    public PowerConsumptionState getPowerConsumptionState() throws NotAvailableException {
        try {
            return getData().getPowerConsumptionState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("powerConsumptionState", ex);
        }
    }
}
