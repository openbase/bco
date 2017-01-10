package org.openbase.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * BCO DAL Library
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
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.state.PowerStateType.PowerState;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface PowerStateOperationServiceCollection extends PowerStateOperationService {

    @Override
    default public Future<Void> setPowerState(final PowerState powerState) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf((PowerStateOperationService input) -> input.setPowerState(powerState), getPowerStateOperationServices());
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
    default public PowerState getPowerState() throws NotAvailableException {
        try {
            for (PowerStateOperationService service : getPowerStateOperationServices()) {
                if (service.getPowerState().getValue() == PowerState.State.ON) {
                    return PowerState.newBuilder().setValue(PowerState.State.ON).build();
                }
            }
            return PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PowerState", ex);
        }
    }

    public Collection<PowerStateOperationService> getPowerStateOperationServices() throws CouldNotPerformException;
}
