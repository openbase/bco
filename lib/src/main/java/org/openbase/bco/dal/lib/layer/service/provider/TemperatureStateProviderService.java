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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.TEMPERATURE_STATE_SERVICE;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface TemperatureStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default TemperatureState getTemperatureState() throws NotAvailableException {
        return (TemperatureState) getServiceProvider().getServiceState(TEMPERATURE_STATE_SERVICE);
    }

    static void verifyTemperatureState(final TemperatureState temperatureState) throws VerificationFailedException {
        if (!temperatureState.hasTemperature()) {
            throw new VerificationFailedException("Temperature value unknown!");
        }

        switch (temperatureState.getTemperatureDataUnit()) {
            case UNKNOWN:
                throw new VerificationFailedException("TemperatureState data unit unknown!");
            default:
                return;
        }
    }
}
