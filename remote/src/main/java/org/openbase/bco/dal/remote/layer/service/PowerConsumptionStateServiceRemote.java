package org.openbase.bco.dal.remote.layer.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import java.util.concurrent.TimeUnit;

import org.openbase.bco.dal.lib.layer.service.collection.PowerConsumptionStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.PowerConsumptionStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.PowerConsumptionStateType.PowerConsumptionState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerConsumptionStateServiceRemote extends AbstractServiceRemote<PowerConsumptionStateProviderService, PowerConsumptionState> implements PowerConsumptionStateProviderServiceCollection {

    public PowerConsumptionStateServiceRemote() {
        super(ServiceType.POWER_CONSUMPTION_STATE_SERVICE, PowerConsumptionState.class);
    }

    /**
     * {@inheritDoc}
     * Computes the average current and voltage and the sum of the consumption of the underlying services.
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected PowerConsumptionState computeServiceState() throws CouldNotPerformException {
        return getPowerConsumptionState(UnitType.UNKNOWN);
    }

    @Override
    public PowerConsumptionState getPowerConsumptionState(final UnitType unitType) throws NotAvailableException {

        long timestamp = 0;
        int voltageValueAmount = 0;
        double voltageAverage = 0;
        double currentSum = 0;
        double consumptionSum = 0;

        for (PowerConsumptionStateProviderService service : getServices(unitType)) {

            // skip service if data is not available
            if (!((UnitRemote) service).isDataAvailable()) {
                continue;
            }

            if (service.getPowerConsumptionState().hasVoltage() && service.getPowerConsumptionState().getVoltage() > 0) {
                voltageValueAmount++;
                voltageAverage += service.getPowerConsumptionState().getVoltage();
            }

            currentSum += service.getPowerConsumptionState().getCurrent();
            consumptionSum += service.getPowerConsumptionState().getConsumption();
            timestamp = Math.max(timestamp, service.getPowerConsumptionState().getTimestamp().getTime());
        }

        voltageAverage = voltageAverage / voltageValueAmount;

        return TimestampProcessor.updateTimestamp(timestamp, PowerConsumptionState.newBuilder().setConsumption(consumptionSum).setCurrent(currentSum).setVoltage(voltageAverage), TimeUnit.MICROSECONDS, logger).build();
    }
}
