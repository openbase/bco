package org.openbase.bco.dal.lib.layer.service.provider;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.EMPHASIS_STATE_SERVICE;

/**
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public interface EmphasisStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default EmphasisState getEmphasisState() throws NotAvailableException {
        return (EmphasisState) getServiceProvider().getServiceState(EMPHASIS_STATE_SERVICE);
    }

    static void verifyEmphasisState(final EmphasisState emphasisState) throws VerificationFailedException {

        if (emphasisState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        boolean emphasisFound = false;

        if (emphasisState.hasComfort()) {
            OperationService.verifyValueRange("comfort", emphasisState.getComfort(), 0d, 1d);
            emphasisFound = true;
        }

        if (emphasisState.hasEconomy()) {
            OperationService.verifyValueRange("economy", emphasisState.getEconomy(), 0d, 1d);
            emphasisFound = true;
        }

        if (emphasisState.hasSecurity()) {
            OperationService.verifyValueRange("security", emphasisState.getSecurity(), 0d, 1d);
            emphasisFound = true;
        }

        if(!emphasisFound) {
            throw new VerificationFailedException("EmphasisState does not contain emphasis!");
        }

        OperationService.verifyValue("emphasis sum", emphasisState.getComfort() + emphasisState.getEconomy() + emphasisState.getSecurity(), 1d, 0.01d);
    }
}
