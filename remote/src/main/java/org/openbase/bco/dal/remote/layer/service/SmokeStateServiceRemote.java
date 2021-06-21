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
import org.openbase.bco.dal.lib.layer.service.collection.SmokeStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.SmokeStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.SmokeStateType.SmokeState;
import org.openbase.type.domotic.state.SmokeStateType.SmokeState.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SmokeStateServiceRemote extends AbstractServiceRemote<SmokeStateProviderService, SmokeState> implements SmokeStateProviderServiceCollection {

    public SmokeStateServiceRemote() {
        super(ServiceType.SMOKE_STATE_SERVICE, SmokeState.class);
    }

    /**
     * {@inheritDoc}
     * Computes the average smoke level and the state as smoke if at least one underlying services detects smoke.
     * If no service detects smoke and at least one detects some smoke then that is set and else no smoke.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected SmokeState computeServiceState() throws CouldNotPerformException {
        return getSmokeState(UnitType.UNKNOWN);
    }

    @Override
    public SmokeState getSmokeState(final UnitType unitType) throws NotAvailableException {

        // prepare fields
        boolean someSmoke = false;
        SmokeState.State smokeValue = SmokeState.State.NO_SMOKE;
        int amount = getServices(unitType).size();
        double averageSmokeLevel = 0;
        long timestamp = 0;
        ActionDescription latestAction = null;

        // iterate over all services and collect available states
        for (SmokeStateProviderService service : getServices(unitType)) {
            final SmokeState state = service.getSmokeState();
            if (!((UnitRemote) service).isDataAvailable() || !state.hasValue()) {
                amount--;
                continue;
            }

            SmokeState smokeState = state;
            if (smokeState.getValue() == SmokeState.State.SMOKE) {
                smokeValue = SmokeState.State.SMOKE;
                break;
            } else if (smokeState.getValue() == SmokeState.State.SOME_SMOKE) {
                someSmoke = true;
            }

            averageSmokeLevel += smokeState.getSmokeLevel();
            timestamp = Math.max(timestamp, smokeState.getTimestamp().getTime());

            // select latest action
            latestAction = selectLatestAction(state, latestAction);
        }

        if (amount == 0) {
            throw new NotAvailableException("SmokeState");
        }

        // finally compute average
        if (someSmoke) {
            smokeValue = SmokeState.State.SOME_SMOKE;
        }
        averageSmokeLevel /= amount;

        // setup state
        Builder serviceStateBuilder = SmokeState.newBuilder().setValue(smokeValue).setSmokeLevel(averageSmokeLevel);

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
