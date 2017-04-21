package org.openbase.bco.dal.remote.service;

/*
 * #%L
 * BCO DAL Remote
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
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.collection.StandbyStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.StandbyStateType.StandbyState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class StandbyStateServiceRemote extends AbstractServiceRemote<StandbyStateOperationService, StandbyState> implements StandbyStateOperationServiceCollection {

    public StandbyStateServiceRemote() {
        super(ServiceType.STANDBY_STATE_SERVICE, StandbyState.class);
    }

    @Override
    public Future<Void> setStandbyState(final StandbyState state) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf(super.getServices(), (StandbyStateOperationService input) -> input.setStandbyState(state));
    }

    @Override
    public Future<Void> setStandbyState(final StandbyState state, final UnitType unitType) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf(super.getServices(unitType), (StandbyStateOperationService input) -> input.setStandbyState(state));
    }

    public Collection<StandbyStateOperationService> getStandbyStateOperationServices() {
        return getServices();
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
    public StandbyState getStandbyState() throws NotAvailableException {
        return getServiceState();
    }

    @Override
    public StandbyState getStandbyState(final UnitType unitType) throws NotAvailableException {
        StandbyState.State standbyValue = StandbyState.State.STANDBY;
        long timestamp = 0;
        for (StandbyStateOperationService service : getServices(unitType)) {
            if (!((UnitRemote) service).isDataAvailable()) {
                continue;
            }

            if (service.getStandbyState().getValue() == StandbyState.State.RUNNING) {
                standbyValue = StandbyState.State.RUNNING;
            }

            timestamp = Math.max(timestamp, service.getStandbyState().getTimestamp().getTime());
        }

        return TimestampProcessor.updateTimestamp(timestamp, StandbyState.newBuilder().setValue(standbyValue), TimeUnit.MICROSECONDS, logger).build();
    }
}
