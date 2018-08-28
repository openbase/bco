/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.VerificationFailedException;
import rst.domotic.state.IlluminanceStateType.IlluminanceState;

import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.ILLUMINANCE_STATE_SERVICE;

/**
 *
 * @author pleminoq
 */
public interface IlluminanceStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default IlluminanceState getIlluminanceState() throws NotAvailableException {
        return (IlluminanceState) getServiceProvider().getServiceState(ILLUMINANCE_STATE_SERVICE);
    }

    static void verifyIlluminanceState(final IlluminanceState illuminanceState) throws VerificationFailedException {
        if (illuminanceState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        if (!illuminanceState.hasIlluminance()) {
            throw new VerificationFailedException("IlluminanceState does not contain illuminance!");
        }
        OperationService.verifyValueRange("illuminance", illuminanceState.getIlluminance(), 0, Double.MAX_VALUE);
    }
}
