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
import org.openbase.bco.dal.lib.layer.service.collection.PowerStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PowerStateServiceRemote extends AbstractServiceRemote<PowerStateOperationService, PowerState> implements PowerStateOperationServiceCollection {

    /**
     * Constructor creates a new service remote.
     * <p>
     * Note: This remote instance totally ignores infrastructure units.
     */
    public PowerStateServiceRemote() {
        this(true);
    }

    /**
     * Constructor creates a new service remote.
     * <p>
     * Note: This remote instance totally ignores infrastructure units.
     *
     * @param filterInfrastructureUnits this flag defines if units which are marked as infrastructure are filtered by this instance.
     */
    public PowerStateServiceRemote(final boolean filterInfrastructureUnits) {
        super(ServiceType.POWER_STATE_SERVICE, PowerState.class, filterInfrastructureUnits);
    }

    @Override
    public Future<ActionDescription> setPowerState(final PowerState powerState, final UnitType unitType) {
        try {
            return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(powerState, getServiceType(), unitType));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    /**
     * {@inheritDoc}
     * Computes the power state as on if at least one underlying service is on and else off.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected PowerState computeServiceState() throws CouldNotPerformException {
        return getPowerState(UnitType.UNKNOWN);
    }

    @Override
    public PowerState getPowerState(final UnitType unitType) throws NotAvailableException {
        try {
            return (PowerState) generateAggregatedState(unitType, State.OFF, State.ON).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException(Services.getServiceStateName(getServiceType()), ex);
        }
    }
}
