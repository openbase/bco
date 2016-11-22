package org.openbase.bco.dal.remote.service;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.collection.ActivationStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.ActivationStateOperationService;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.timing.TimestampType.Timestamp;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ActivationStateServiceRemote extends AbstractServiceRemote<ActivationStateOperationService, ActivationState> implements ActivationStateOperationServiceCollection {

    public ActivationStateServiceRemote() {
        super(ServiceType.  ACTIVATION_STATE_SERVICE);
    }

    @Override
    public Collection<ActivationStateOperationService> getActivationStateOperationServices() {
        return getServices();
    }

    /**
     * {@inheritDoc}
     * Computes the activation state as on if at least one underlying service is on and else off.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected ActivationState computeServiceState() throws CouldNotPerformException {
        ActivationState.State activationStateValue = ActivationState.State.DEACTIVE;
        for (ActivationStateOperationService service : getActivationStateOperationServices()) {
            if (!((UnitRemote) service).isDataAvailable()) {
                continue;
            }

            if (service.getActivationState().getValue() == ActivationState.State.ACTIVE) {
                activationStateValue = ActivationState.State.ACTIVE;
            }
        }

        return ActivationState.newBuilder().setValue(activationStateValue).setTimestamp(Timestamp.newBuilder().setTime(System.currentTimeMillis())).build();
    }

    @Override
    public ActivationState getActivationState() throws NotAvailableException {
        return getServiceState();
    }
}
