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
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.Future;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface PowerStateOperationServiceCollection extends PowerStateOperationService {

    default Future<ActionDescription> setPowerState(final PowerState powerState, final UnitType unitType) {
        try {
            return getServiceProvider().applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(powerState, ServiceType.POWER_STATE_SERVICE, unitType));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    default Future<ActionDescription> setPowerState(final PowerState.State powerState, final UnitType unitType) {
        return setPowerState(PowerState.newBuilder().setValue(powerState).build(), unitType);
    }

    default Future<ActionDescription> setPowerState(final PowerState powerState, final UnitType unitType, final ActionParameter actionParameter) {
        try {
            return getServiceProvider().applyAction(actionParameter.toBuilder().setServiceStateDescription(ActionDescriptionProcessor.generateServiceStateDescription(powerState, ServiceType.POWER_STATE_SERVICE).setUnitType(unitType)));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    default Future<ActionDescription> setPowerState(final PowerState.State powerState, final UnitType unitType, final ActionParameter actionParameter) {
        return setPowerState(PowerState.newBuilder().setValue(powerState).build(), unitType, actionParameter);
    }

    /**
     * Returns on if at least one of the power services is on and else off.
     *
     * @return
     *
     * @throws NotAvailableException
     */
    @Override
    default PowerState getPowerState() throws NotAvailableException {
        return PowerStateOperationService.super.getPowerState();
    }

    /**
     * Returns on if at least one of the powerServices with given unitType is on
     * and else off.
     *
     * @param unitType the unit type to filter.
     *
     * @return the aggregated power state.
     *
     * @throws NotAvailableException is throws
     */
    PowerState getPowerState(final UnitType unitType) throws NotAvailableException;
}
