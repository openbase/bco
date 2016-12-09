package org.openbase.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * DAL Library
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
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.operation.ActivationStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.state.ActivationStateType.ActivationState;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface ActivationStateOperationServiceCollection extends ActivationStateOperationService {

    @Override
    default public Future<Void> setActivationState(final ActivationState activationState) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf((ActivationStateOperationService input) -> input.setActivationState(activationState), getActivationStateOperationServices());
    }

    //TODO: is implemented in the service remotes but still used in the LocationController because else it would lead to too many unitRemots
    //remove when remote cashing is implemented
    /**
     * Returns on if at least one of the power services is on and else off.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public ActivationState getActivationState() throws NotAvailableException {
        try {
            for (ActivationStateOperationService service : getActivationStateOperationServices()) {
                if (service.getActivationState().getValue() == ActivationState.State.ACTIVE) {
                    return ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build();
                }
            }
            return ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ActivationState", ex);
        }
    }

    public Collection<ActivationStateOperationService> getActivationStateOperationServices() throws CouldNotPerformException;
}
