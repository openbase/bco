package org.openbase.bco.dal.lib.layer.service.provider;

/*-
 * #%L
 * BCO DAL Library
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

import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.GlobalPositionStateType.GlobalPositionState;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface GlobalPositionStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default GlobalPositionState getGlobalPositionState() throws NotAvailableException {
        return (GlobalPositionState) getServiceProvider().getServiceState(ServiceType.GLOBAL_POSITION_STATE_SERVICE);
    }

    static void verifyGlobalPositionState(final GlobalPositionState globalPositionState) throws VerificationFailedException {
        if (globalPositionState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        if (!globalPositionState.hasLatitude()) {
            throw new VerificationFailedException("GlobalPositionState does not contain latitude!");
        }
        if (!globalPositionState.hasLongitude()) {
            throw new VerificationFailedException("GlobalPositionState does not contain longitude!");
        }
        OperationService.verifyValueRange("globalPosition", globalPositionState.getLatitude(), -90, 90);
        OperationService.verifyValueRange("globalPosition", globalPositionState.getLongitude(), -180, 180);
    }
}
