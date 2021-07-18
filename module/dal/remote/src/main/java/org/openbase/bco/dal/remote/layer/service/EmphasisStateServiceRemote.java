package org.openbase.bco.dal.remote.layer.service;

/*-
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
import org.openbase.bco.dal.lib.layer.service.collection.EmphasisStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.EmphasisStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState;
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class EmphasisStateServiceRemote extends AbstractServiceRemote<EmphasisStateOperationService, EmphasisState> implements EmphasisStateOperationServiceCollection {

    public EmphasisStateServiceRemote() {
        super(ServiceTemplateType.ServiceTemplate.ServiceType.EMPHASIS_STATE_SERVICE, EmphasisState.class);
    }

    @Override
    protected EmphasisState computeServiceState() throws CouldNotPerformException {
        return getEmphasisState(UnitType.UNKNOWN);
    }

    @Override
    public Future<ActionDescriptionType.ActionDescription> setEmphasisState(final EmphasisState emphasisState, UnitType unitType) {
        try {
            return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(emphasisState, getServiceType(), unitType));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    @Override
    public EmphasisState getEmphasisState(UnitType unitType) throws NotAvailableException {

        // prepare fields
        Collection<EmphasisStateOperationService> emphasisStateOperationServices = getServices(unitType);
        int amount = emphasisStateOperationServices.size();
        Double averageComfort = 0d;
        Double averageEconomy = 0d;
        Double averageSecurity = 0d;
        long timestamp = 0;
        ActionDescription latestAction = null;

        // iterate over all services and collect available states
        for (EmphasisStateOperationService service : emphasisStateOperationServices) {
            final EmphasisState state = service.getEmphasisState();

            if (!((UnitRemote) service).isDataAvailable()|| !(
                    state.hasComfort() ||
                    state.hasEconomy() ||
                    state.hasSecurity())) {
                amount--;
                continue;
            }

            // finally compute average
            averageComfort += state.getComfort();
            averageEconomy += state.getEconomy();
            averageSecurity += state.getSecurity();
            timestamp = Math.max(timestamp, state.getTimestamp().getTime());

            // select latest action
            latestAction = selectLatestAction(state, latestAction);
        }

        if (amount == 0) {
            throw new NotAvailableException("EmphasisState");
        }

        // finally compute average
        averageComfort /= amount;
        averageEconomy /= amount;
        averageSecurity /= amount;

        // setup state
        Builder serviceStateBuilder = EmphasisState.newBuilder().setComfort(averageComfort).setEconomy(averageEconomy).setSecurity(averageSecurity);

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
