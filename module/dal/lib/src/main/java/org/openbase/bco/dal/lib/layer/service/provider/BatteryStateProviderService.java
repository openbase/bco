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

import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState.Builder;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState.State;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.BATTERY_STATE_SERVICE;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface BatteryStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default BatteryState getBatteryState() throws NotAvailableException {
        return (BatteryState) getServiceProvider().getServiceState(BATTERY_STATE_SERVICE);
    }

    static BatteryState verifyBatteryState(BatteryState batteryState) throws VerificationFailedException {
        final Builder builder = batteryState.toBuilder();
        revalidate(builder);
        Services.verifyServiceState(builder);
        return builder.build();
    }

    static void revalidate(BatteryState.Builder builder) throws VerificationFailedException {
        if (builder.hasLevel()) {
            builder.setLevel(ProviderService.oldValueNormalization(builder.getLevel(), 100));
            OperationService.verifyValueRange("batterylevel", builder.getLevel(), 0, 1d);

            if (builder.getLevel() <= 0.05d) {
                builder.setValue(State.INSUFFICIENT);
            } else if (builder.getLevel() <= 0.15d) {
                builder.setValue(State.CRITICAL);
            } else if (builder.getLevel() <= 0.3d) {
                builder.setValue(State.LOW);
            } else {
                builder.setValue(State.OK);
            }
        } else if (!builder.hasLevel() && builder.getValue() != BatteryState.State.UNKNOWN) {
            if (builder.getValue() == State.INSUFFICIENT) {
                builder.setLevel(0.05d);
            } else if (builder.getValue() == State.CRITICAL) {
                builder.setLevel(0.15d);
            } else if (builder.getValue() == State.LOW) {
                builder.setLevel(0.30d);
            } else if (builder.getValue() == State.OK) {
                builder.setLevel(1.0d);
            }
        }
    }
}
