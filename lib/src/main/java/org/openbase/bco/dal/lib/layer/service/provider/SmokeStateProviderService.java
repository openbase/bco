package org.openbase.bco.dal.lib.layer.service.provider;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import org.openbase.type.domotic.state.BatteryStateType.BatteryState;
import org.openbase.type.domotic.state.SmokeStateType.SmokeState;
import org.openbase.type.domotic.state.SmokeStateType.SmokeState.Builder;
import org.openbase.type.domotic.state.SmokeStateType.SmokeState.State;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.SMOKE_STATE_SERVICE;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface SmokeStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default SmokeState getSmokeState() throws NotAvailableException {
        return (SmokeState) getServiceProvider().getServiceState(SMOKE_STATE_SERVICE);
    }

    static SmokeState verifySmokeState(final SmokeState smokeState) throws VerificationFailedException {
        final Builder builder = smokeState.toBuilder();
        revalidate(builder);
        Services.verifyServiceState(builder);
        return builder.build();
    }

    static void revalidate(SmokeState.Builder builder) throws VerificationFailedException {
        if (builder.hasSmokeLevel()) {
            builder.setSmokeLevel(ProviderService.oldValueNormalization(builder.getSmokeLevel(), 100));
            OperationService.verifyValueRange("batterylevel", builder.getSmokeLevel(), 0, 1d);
            if (builder.getSmokeLevel() == 0.00d) {
                builder.setValue(SmokeState.State.NO_SMOKE);
            } else if (builder.getSmokeLevel() <= 0.15d) {
                builder.setValue(SmokeState.State.SOME_SMOKE);
            } else {
                builder.setValue(SmokeState.State.SMOKE);
            }
        } else if (!builder.hasSmokeLevel() && builder.getValue() != SmokeState.State.UNKNOWN) {
            if(builder.getValue() == State.NO_SMOKE) {
                builder.setSmokeLevel(0.00d);
            }else if(builder.getValue() == State.SOME_SMOKE) {
                builder.setSmokeLevel(0.15d);
            }else if(builder.getValue() == State.SMOKE) {
                builder.setSmokeLevel(1.0d);
            }
        }
    }
}
