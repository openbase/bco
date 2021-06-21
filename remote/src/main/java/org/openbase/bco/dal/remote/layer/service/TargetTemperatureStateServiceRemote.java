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

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.collection.TargetTemperatureStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TargetTemperatureStateServiceRemote extends AbstractServiceRemote<TargetTemperatureStateOperationService, TemperatureState> implements TargetTemperatureStateOperationServiceCollection {

    public TargetTemperatureStateServiceRemote() {
        super(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE, TemperatureState.class);
    }

    @Override
    public Future<ActionDescription> setTargetTemperatureState(final TemperatureState temperatureState, final UnitType unitType) {
        try {
            return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(temperatureState, getServiceType(), unitType));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    /**
     * {@inheritDoc}
     * Computes the average temperature value.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected TemperatureState computeServiceState() throws CouldNotPerformException {
        return getTargetTemperatureState(UnitType.UNKNOWN);
    }

    @Override
    public TemperatureState getTargetTemperatureState(final UnitType unitType) throws NotAvailableException {

        // prepare fields
        Double average = 0d;
        long timestamp = 0;
        final Collection<TargetTemperatureStateOperationService> targetTemperatureStateOperationServices = getServices(unitType);
        int amount = targetTemperatureStateOperationServices.size();
        ActionDescription latestAction = null;

        // iterate over all services and collect available states
        for (TargetTemperatureStateOperationService service : targetTemperatureStateOperationServices) {
            final TemperatureState state = service.getTargetTemperatureState();

            if (!((UnitRemote) service).isDataAvailable() || !state.hasTemperature()) {
                amount--;
                continue;
            }

            average += state.getTemperature();
            timestamp = Math.max(timestamp, state.getTimestamp().getTime());

            // select latest action
            latestAction = selectLatestAction(state, latestAction);
        }

        if (amount == 0) {
            throw new NotAvailableException("TargetTemperatureState");
        }

        // finally compute average
        average /= amount;

        // setup state
        Builder serviceStateBuilder = TemperatureState.newBuilder().setTemperature(average);

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
