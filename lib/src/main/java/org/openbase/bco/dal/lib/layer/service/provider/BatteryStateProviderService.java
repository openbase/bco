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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState.Builder;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState.State;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.BATTERY_STATE_SERVICE;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface BatteryStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default BatteryState getBatteryState() throws NotAvailableException {
        return (BatteryState) getServiceProvider().getServiceState(BATTERY_STATE_SERVICE);
    }

    static BatteryState verifyBatteryState(BatteryState batteryState) throws VerificationFailedException {
        batteryState = revalidate(batteryState);
        Services.verifyServiceState(batteryState);
        return batteryState;
    }

    static BatteryState revalidate(BatteryState batteryState) {
        if (batteryState.hasLevel()) {
            final Builder batteryStateBuilder = batteryState.toBuilder();
            if (batteryState.getLevel() <= 5) {
                batteryStateBuilder.setValue(BatteryState.State.INSUFFICIENT);
            } else if (batteryState.getLevel() <= 15) {
                batteryStateBuilder.setValue(BatteryState.State.CRITICAL);
            } else {
                batteryStateBuilder.setValue(BatteryState.State.OK);
            }
            return batteryStateBuilder.build();
        } else if (!batteryState.hasLevel() && batteryState.getValue() != BatteryState.State.UNKNOWN) {
            final Builder batteryStateBuilder = batteryState.toBuilder();
            if(batteryState.getValue() == BatteryState.State.INSUFFICIENT) {
                batteryStateBuilder.setLevel(5);
            }else if(batteryState.getValue() == BatteryState.State.CRITICAL) {
                batteryStateBuilder.setLevel(15);
            }else if(batteryState.getValue() == State.OK) {
                batteryStateBuilder.setLevel(100);
            }
            return batteryStateBuilder.build();
        }
        return batteryState;
    }
}
