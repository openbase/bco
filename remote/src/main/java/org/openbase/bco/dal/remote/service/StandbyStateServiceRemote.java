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
import org.openbase.bco.dal.lib.layer.service.collection.StandbyStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.StandbyStateType.StandbyState;
import rst.timing.TimestampType.Timestamp;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class StandbyStateServiceRemote extends AbstractServiceRemote<StandbyStateOperationService, StandbyState> implements StandbyStateOperationServiceCollection {

    public StandbyStateServiceRemote() {
        super(ServiceType.STANDBY_STATE_SERVICE);
    }

    @Override
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
        StandbyState.State standbyValue = StandbyState.State.STANDBY;
        for (StandbyStateOperationService service : getStandbyStateOperationServices()) {
            if (!((UnitRemote) service).isDataAvailable()) {
                continue;
            }

            if (service.getStandbyState().getValue() == StandbyState.State.RUNNING) {
                standbyValue = StandbyState.State.RUNNING;
            }
        }

        return StandbyState.newBuilder().setValue(standbyValue).setTimestamp(Timestamp.newBuilder().setTime(System.currentTimeMillis())).build();
    }

    @Override
    public StandbyState getStandbyState() throws NotAvailableException {
        return getServiceState();
    }
}
