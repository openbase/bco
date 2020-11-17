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

import org.openbase.bco.dal.lib.layer.service.provider.AvailabilityStateProviderService;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.AvailabilityStateType.AvailabilityState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.Future;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface AvailabilityStateProviderServiceCollection extends AvailabilityStateProviderService {

    Future<ActionDescription> setAvailabilityState(final AvailabilityState availabilityState, final UnitType unitType);

    /**
     * Returns online if at least one instance is available, otherwise offline.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default AvailabilityState getAvailabilityState() throws NotAvailableException {
        return AvailabilityStateProviderService.super.getAvailabilityState();
    }

    /**
     * Returns online if at least one instance is available, otherwise offline.
     *
     * @param unitType the unit type to filter.
     * @return
     * @throws NotAvailableException
     */
    AvailabilityState getAvailabilityState(final UnitType unitType) throws NotAvailableException;
}
