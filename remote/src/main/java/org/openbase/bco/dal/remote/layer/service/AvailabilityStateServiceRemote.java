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

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.collection.AvailabilityStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.AvailabilityStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.AvailabilityStateType.AvailabilityState;
import org.openbase.type.domotic.state.AvailabilityStateType.AvailabilityState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class AvailabilityStateServiceRemote extends AbstractServiceRemote<AvailabilityStateProviderService, AvailabilityState> implements AvailabilityStateProviderServiceCollection {

    public AvailabilityStateServiceRemote() {
        super(ServiceType.AVAILABILITY_STATE_SERVICE, AvailabilityState.class);
    }

    /**
     * {@inheritDoc} Computes the availability state as on if at least one underlying service is on and else off.
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected AvailabilityState computeServiceState() throws CouldNotPerformException {
        return getAvailabilityState(UnitType.UNKNOWN);
    }

    @Override
    public AvailabilityState getAvailabilityState(final UnitType unitType) throws NotAvailableException {
        try {
            return (AvailabilityState) generateAggregatedState(unitType, State.OFFLINE, State.ONLINE).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException(Services.getServiceStateName(getServiceType()), ex);
        }
    }

    @Override
    public Future<ActionDescription> setAvailabilityState(final AvailabilityState availabilityState, final UnitType unitType) {
        try {
            return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(availabilityState, getServiceType(), unitType));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }
}
