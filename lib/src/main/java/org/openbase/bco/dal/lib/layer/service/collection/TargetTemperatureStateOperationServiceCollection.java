package org.openbase.bco.dal.lib.layer.service.collection;

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

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.Future;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface TargetTemperatureStateOperationServiceCollection extends TargetTemperatureStateOperationService {

    default Future<ActionDescription> setTargetTemperatureState(final TemperatureState temperatureState, final UnitType unitType) {
        try {
            return getServiceProvider().applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(temperatureState, ServiceType.TARGET_TEMPERATURE_STATE_SERVICE, unitType));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    default Future<ActionDescription> setTargetTemperatureState(final double temperature, final UnitType unitType) {
        return setTargetTemperatureState(TemperatureState.newBuilder().setTemperature(temperature).build(), unitType);
    }

    /**
     * Returns the average target temperature value for a collection of target
     * temperature services.
     *
     * @return
     *
     * @throws NotAvailableException
     */
    @Override
    default TemperatureState getTargetTemperatureState() throws NotAvailableException {
        return TargetTemperatureStateOperationService.super.getTargetTemperatureState();
    }

    /**
     * Returns the average target temperature value for a collection of target
     * temperature services with the given unitType.
     *
     * @param unitType
     *
     * @return
     *
     * @throws NotAvailableException
     */
    TemperatureState getTargetTemperatureState(final UnitType unitType) throws NotAvailableException;
}
