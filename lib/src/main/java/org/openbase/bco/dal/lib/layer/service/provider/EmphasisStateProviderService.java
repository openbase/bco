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

import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import rst.domotic.state.EmphasisStateType.EmphasisState;

import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.EMPHASIS_STATE_SERVICE;

/**
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public interface EmphasisStateProviderService extends ProviderService {

    static void verifyEmphasisState(final EmphasisState emphasisState) throws VerificationFailedException {

        if (emphasisState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        if (emphasisState.hasComfort()) {
            OperationService.verifyValueRange("comfort", emphasisState.getComfort(), 0, 100);
            return;
        } else if (emphasisState.hasEnergy()) {
            OperationService.verifyValueRange("energy saving", emphasisState.getEnergy(), 0, 100);
            return;
        } else if (emphasisState.hasSecurity()) {
            OperationService.verifyValueRange("security", emphasisState.getSecurity(), 0, 100);
            return;
        }
        throw new VerificationFailedException("EmphasisState does not contain emphasis!");
    }

    @RPCMethod
    default EmphasisState getEmphasisState() throws NotAvailableException {
        return (EmphasisState) getServiceProvider().getServiceState(EMPHASIS_STATE_SERVICE);
    }
}
