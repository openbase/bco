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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.type.domotic.state.PowerConsumptionStateType.PowerConsumptionState;
import org.openbase.type.domotic.state.PowerConsumptionStateType.PowerConsumptionState.Builder;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.POWER_CONSUMPTION_STATE_SERVICE;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface PowerConsumptionStateProviderService extends ProviderService {

    double DEFAULT_VOLTAGE = 230;

    @RPCMethod(legacy = true)
    default PowerConsumptionState getPowerConsumptionState() throws NotAvailableException {
        return (PowerConsumptionState) getServiceProvider().getServiceState(POWER_CONSUMPTION_STATE_SERVICE);
    }

    static PowerConsumptionState verifyPowerConsumptionState(final PowerConsumptionState powerConsumptionState) throws VerificationFailedException {
        if (powerConsumptionState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        if (!powerConsumptionState.hasConsumption() && !powerConsumptionState.hasVoltage() && !powerConsumptionState.hasCurrent()) {
            throw new VerificationFailedException("PowerConsumptionState does not contain any values!");
        }

        int availableParameterCount = 0;

        // consumption range check
        if (powerConsumptionState.hasConsumption()) {
            OperationService.verifyValueRange("Consumption", powerConsumptionState.getConsumption(), 0, Double.MAX_VALUE);
            availableParameterCount++;
        }

        // voltage range check
        if (powerConsumptionState.hasVoltage()) {
            OperationService.verifyValueRange("Voltage", powerConsumptionState.getVoltage(), 0, Double.MAX_VALUE);
            availableParameterCount++;
        }

        // current range check
        if (powerConsumptionState.hasCurrent()) {
            OperationService.verifyValueRange("Current", powerConsumptionState.getCurrent(), 0, Double.MAX_VALUE);
            availableParameterCount++;
        }

        final Builder powerConsumtionBuilder = powerConsumptionState.toBuilder();

        // fix voltage to default if not enough parameters are given to compute the missing ones.
        if (availableParameterCount == 1  && !powerConsumptionState.hasVoltage()) {
            powerConsumtionBuilder.setVoltage(DEFAULT_VOLTAGE);
        }

        // try to compute missing consumption
        if(!powerConsumtionBuilder.hasConsumption() && powerConsumtionBuilder.hasVoltage() && powerConsumtionBuilder.hasCurrent()) {
            return powerConsumtionBuilder.setConsumption(powerConsumtionBuilder.getVoltage() * powerConsumtionBuilder.getCurrent()).build();
        }

        // try to compute missing voltage
        if(!powerConsumtionBuilder.hasVoltage() && powerConsumtionBuilder.hasConsumption() && powerConsumtionBuilder.hasCurrent()) {
            return powerConsumtionBuilder.setVoltage(powerConsumtionBuilder.getConsumption() / powerConsumtionBuilder.getCurrent()).build();
        }

        // try to compute missing current
        if(!powerConsumtionBuilder.hasCurrent() && powerConsumtionBuilder.hasVoltage() && powerConsumtionBuilder.hasConsumption()) {
            return powerConsumtionBuilder.setCurrent(powerConsumtionBuilder.getConsumption() / powerConsumtionBuilder.getVoltage()).build();
        }

        return powerConsumptionState;
    }
}
