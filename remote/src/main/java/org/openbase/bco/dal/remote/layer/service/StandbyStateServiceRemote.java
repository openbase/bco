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
import org.openbase.bco.dal.lib.layer.service.collection.StandbyStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.StandbyStateType.StandbyState;
import org.openbase.type.domotic.state.StandbyStateType.StandbyState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.Future;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class StandbyStateServiceRemote extends AbstractServiceRemote<StandbyStateOperationService, StandbyState> implements StandbyStateOperationServiceCollection {

    public StandbyStateServiceRemote() {
        super(ServiceType.STANDBY_STATE_SERVICE, StandbyState.class);
    }

    @Override
    public Future<ActionDescription> setStandbyState(final StandbyState standbyState, final UnitType unitType) {
        try {
            return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(standbyState, getServiceType(), unitType));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    /**
     * {@inheritDoc}
     * Computes the standby state as running if at least one underlying service is running and else standby.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected StandbyState computeServiceState() throws CouldNotPerformException {
        return getStandbyState(UnitType.UNKNOWN);
    }

    @Override
    public StandbyState getStandbyState(final UnitType unitType) throws NotAvailableException {
        try {
            return (StandbyState) generateAggregatedState(unitType, State.RUNNING, State.STANDBY).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException(Services.getServiceStateName(getServiceType()), ex);
        }
    }
}
