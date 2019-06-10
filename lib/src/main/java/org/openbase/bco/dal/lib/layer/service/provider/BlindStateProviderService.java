package org.openbase.bco.dal.lib.layer.service.provider;

/*
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.type.domotic.state.BlindStateType.BlindState;
import org.openbase.type.domotic.state.BlindStateType.BlindState.Builder;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.BLIND_STATE_SERVICE;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface BlindStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default BlindState getBlindState() throws NotAvailableException {
        return (BlindState) getServiceProvider().getServiceState(BLIND_STATE_SERVICE);
    }

    static BlindState verifyBlindState(final BlindState blindState) throws VerificationFailedException {
        try {
            final Builder builder = blindState.toBuilder();
            if (!(blindState.hasValue() || blindState.hasOpeningRatio())) {
                throw new VerificationFailedException("MovementState or OpeningRatio not available!", new InvalidStateException(blindState.toString()));
            }

            if (blindState.hasOpeningRatio()) {
                builder.setOpeningRatio(ProviderService.oldValueNormalization(builder.getOpeningRatio(), 100));
                OperationService.verifyValueRange("openingRatio", builder.getOpeningRatio(), 0, 1);
            }

            if (blindState.hasValue()) {
                Services.verifyServiceState(blindState);
            }
            return builder.build();
        } catch (final CouldNotPerformException ex) {
            throw new VerificationFailedException("BlindState not valid!", ex);
        }
    }
}
