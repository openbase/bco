package org.openbase.bco.dal.lib.layer.service.provider;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.LocalPositionStateType.LocalPositionState;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface LocalPositionStateProviderService extends ProviderService {

    @RPCMethod
    default LocalPositionState getLocalPositionState() throws NotAvailableException {
        return (LocalPositionState) getServiceProvider().getServiceState(ServiceType.LOCAL_POSITION_STATE_SERVICE);
    }

    static void verifyLocalPositionState(final LocalPositionState localPositionState) throws VerificationFailedException {
        if (localPositionState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        // if the local position state has a position it should also have at least the id of the root location
        // because if the location id is not set it is seen is not at home
        if (!localPositionState.hasLocationId() && localPositionState.hasPosition()) {
            throw new VerificationFailedException(new NotAvailableException("location id"));
        }

        if (localPositionState.hasLocationId()) {
            try {
                Registries.getUnitRegistry().getUnitConfigById(localPositionState.getLocationId());
            } catch (CouldNotPerformException ex) {
                throw new VerificationFailedException("Invalid location id", ex);
            }
        }
    }
}
