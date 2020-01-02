package org.openbase.bco.dal.remote.layer.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.collection.HandleStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.HandleStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.HandleStateType.HandleState;
import org.openbase.type.domotic.state.HandleStateType.HandleState.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class HandleStateServiceRemote extends AbstractServiceRemote<HandleStateProviderService, HandleState> implements HandleStateProviderServiceCollection {

    public HandleStateServiceRemote() {
        super(ServiceType.HANDLE_STATE_SERVICE, HandleState.class);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected HandleState computeServiceState() throws CouldNotPerformException {
        return getHandleState(UnitType.UNKNOWN);
    }

    @Override
    public HandleState getHandleState(final UnitType unitType) throws NotAvailableException {


        // prepare fields
        // TODO: rethink position in handle state
        int position = 0;
        //boolean tilted = false;
        Collection<HandleStateProviderService> handleStateProviderServices = getServices(unitType);
        int amount = handleStateProviderServices.size();
        long timestamp = 0;
        ActionDescription latestAction = null;

        // iterate over all services and collect available states
        for (HandleStateProviderService service : handleStateProviderServices) {
            final HandleState state = service.getHandleState();

            if (!((UnitRemote) service).isDataAvailable() || !state.hasPosition()) {
                amount--;
                continue;
            }

            position += state.getPosition();
            timestamp = Math.max(timestamp, state.getTimestamp().getTime());

            // select latest action
            latestAction = selectLatestAction(state, latestAction);
        }

        if (amount == 0) {
            throw new NotAvailableException("HandleState");
        }

        // finally compute average
        position /= amount;

        // setup state
        Builder serviceStateBuilder = HandleState.newBuilder().setPosition(position);

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
