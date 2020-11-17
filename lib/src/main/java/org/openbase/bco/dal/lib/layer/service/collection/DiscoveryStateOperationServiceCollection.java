package org.openbase.bco.dal.lib.layer.service.collection;

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

import org.openbase.bco.dal.lib.layer.service.operation.ActivationStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.DiscoveryStateOperationService;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.Future;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface DiscoveryStateOperationServiceCollection extends DiscoveryStateOperationService {

    Future<ActionDescription> setDiscoveryState(final ActivationState activationState, final UnitType unitType);

    /**
     * Returns active if at least one discovery is running, otherwise inactive.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default ActivationState getDiscoveryState() throws NotAvailableException {
        return DiscoveryStateOperationService.super.getDiscoveryState();
    }

    /**
     * Returns active if at least one discovery is running, otherwise inactive.
     *
     * @param unitType the unit type to filter.
     * @return
     * @throws NotAvailableException
     */
    ActivationState getDiscoveryState(final UnitType unitType) throws NotAvailableException;
}
