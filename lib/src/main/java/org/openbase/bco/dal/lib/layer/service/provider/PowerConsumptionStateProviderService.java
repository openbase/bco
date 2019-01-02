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
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.type.domotic.state.PowerConsumptionStateType.PowerConsumptionState;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.POWER_CONSUMPTION_STATE_SERVICE;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface PowerConsumptionStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default PowerConsumptionState getPowerConsumptionState() throws NotAvailableException {
        return (PowerConsumptionState) getServiceProvider().getServiceState(POWER_CONSUMPTION_STATE_SERVICE);
    }

    static void verifyPowerConsumptionState(final PowerConsumptionState powerConsumptionState) throws VerificationFailedException {
        if (powerConsumptionState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        if (!powerConsumptionState.hasConsumption() && !powerConsumptionState.hasVoltage() && !powerConsumptionState.hasCurrent()) {
            throw new VerificationFailedException("PowerConsumptionState does not contain any values!");
        }

        // range check
        if (powerConsumptionState.hasConsumption()) {
            OperationService.verifyValueRange("Consumption", powerConsumptionState.getConsumption(), 0, Double.MAX_VALUE);
        }

        if (powerConsumptionState.hasVoltage()) {
            OperationService.verifyValueRange("Voltage", powerConsumptionState.getVoltage(), 0, Double.MAX_VALUE);
        }

        if (powerConsumptionState.hasCurrent()) {
            OperationService.verifyValueRange("Current", powerConsumptionState.getCurrent(), 0, Double.MAX_VALUE);
        }
    }
}
