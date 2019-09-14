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

import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState.Builder;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState.State;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.ILLUMINANCE_STATE_SERVICE;

/**
 * @author pleminoq
 */
public interface IlluminanceStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default IlluminanceState getIlluminanceState() throws NotAvailableException {
        return (IlluminanceState) getServiceProvider().getServiceState(ILLUMINANCE_STATE_SERVICE);
    }

    static IlluminanceState verifyIlluminanceState(final IlluminanceState illuminanceState) throws VerificationFailedException {

        if (illuminanceState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        final Builder builder = illuminanceState.toBuilder();
        revalidate(builder);
        Services.verifyServiceState(builder);
        return builder.build();
    }

    // illuminance border values based on https://en.wikipedia.org/wiki/Lux
    double SUNNY_VALUE_UPPER_BORDER = 25000;
    double SHADY_VALUE_UPPER_BORDER = 10000;
    double DUSKY_VALUE_UPPER_BORDER = 400;
    double DARK_VALUE_UPPER_BORDER = 3.4;

    static void revalidate(IlluminanceState.Builder builder) throws VerificationFailedException {
        if (builder.hasIlluminance()) {
            OperationService.verifyValueRange("illuminance", builder.getIlluminance(), 0, Double.MAX_VALUE);

            if (builder.getIlluminance() <= DARK_VALUE_UPPER_BORDER) {
                builder.setValue(IlluminanceState.State.DARK);
            } else if (builder.getIlluminance() <= DUSKY_VALUE_UPPER_BORDER) {
                builder.setValue(IlluminanceState.State.DUSKY);
            } else if (builder.getIlluminance() <= SHADY_VALUE_UPPER_BORDER) {
                builder.setValue(IlluminanceState.State.SHADY);
            } else {
                builder.setValue(IlluminanceState.State.SUNNY);
            }
        } else if (!builder.hasIlluminance() && builder.getValue() != IlluminanceState.State.UNKNOWN) {
            if (builder.getValue() == State.DARK) {
                builder.setIlluminance(DARK_VALUE_UPPER_BORDER);
            } else if (builder.getValue() == State.DUSKY) {
                builder.setIlluminance(DUSKY_VALUE_UPPER_BORDER);
            } else if (builder.getValue() == State.SHADY) {
                builder.setIlluminance(SHADY_VALUE_UPPER_BORDER);
            } else if (builder.getValue() == State.SUNNY) {
                builder.setIlluminance(SUNNY_VALUE_UPPER_BORDER);
            }
        } else {
            throw new VerificationFailedException("IlluminanceState does not contain no illuminance nor any related discrete state!");
        }
    }
}
