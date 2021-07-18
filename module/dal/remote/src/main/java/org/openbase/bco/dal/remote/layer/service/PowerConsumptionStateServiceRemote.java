package org.openbase.bco.dal.remote.layer.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.collection.PowerConsumptionStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.PowerConsumptionStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.PowerConsumptionStateType.PowerConsumptionState;
import org.openbase.type.domotic.state.PowerConsumptionStateType.PowerConsumptionState.Builder;
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

        // prepare fields
        long timestamp = 0;
        int voltageValueAmount = 0;
        double voltageAverage = 0;
        double currentSum = 0;
        double consumptionSum = 0;
        ActionDescription latestAction = null;

        // iterate over all services and collect available states
        for (PowerConsumptionStateProviderService service : getServices(unitType)) {
            final PowerConsumptionState state = service.getPowerConsumptionState();

            // skip service if data is not available
            if (!((UnitRemote) service).isDataAvailable()) {
                continue;
            }

            if (state.hasVoltage() && state.getVoltage() > 0) {
                voltageValueAmount++;
                voltageAverage += state.getVoltage();
            }

            currentSum += state.getCurrent();
            consumptionSum += state.getConsumption();
            timestamp = Math.max(timestamp, state.getTimestamp().getTime());

            // select latest action
            latestAction = selectLatestAction(state, latestAction);
        }

        if (voltageValueAmount == 0) {
            throw new NotAvailableException("PowerConsumptionState");
        }

        // finally compute average
        voltageAverage = voltageAverage / voltageValueAmount;

        // setup state
        Builder serviceStateBuilder = PowerConsumptionState.newBuilder().setConsumption(consumptionSum).setCurrent(currentSum).setVoltage(voltageAverage);

        // revalidate to update state value
        try {
            serviceStateBuilder = Services.verifyAndRevalidateServiceState(serviceStateBuilder);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not validate service state!", ex, logger);
        }

        // setup timestamp
        TimestampProcessor.updateTimestamp(timestamp, serviceStateBuilder, TimeUnit.MICROSECONDS, logger).build();

        // setup responsible action with latest action as cause.
        setupResponsibleActionForNewAggregatedServiceState(serviceStateBuilder, latestAction);

        return serviceStateBuilder.build();
    }
}
