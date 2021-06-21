package org.openbase.bco.dal.lib.layer.service.provider;

/*
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
import org.openbase.type.domotic.state.HandleStateType.HandleState;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.HANDLE_STATE_SERVICE;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface HandleStateProviderService extends ProviderService {

    static void verifyHandleState(final HandleState handleState) throws VerificationFailedException {
        if (handleState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        if (!handleState.hasPosition()) {
            throw new VerificationFailedException("HandleState does not contain position!");
        }
        OperationService.verifyValueRange("position", handleState.getPosition(), 0, 360);
    }

    @RPCMethod(legacy = true)
    default HandleState getHandleState() throws NotAvailableException {
        return (HandleState) getServiceProvider().getServiceState(HANDLE_STATE_SERVICE);
    }
}
