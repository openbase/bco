/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.dal.lib.layer.service.collection;

/*-
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

import org.openbase.bco.dal.lib.layer.service.provider.IlluminanceStateProviderService;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author pleminoq
 */
public interface IlluminanceStateProviderServiceCollection extends IlluminanceStateProviderService {

    /**
     * Compute the average illuminance measured by the underlying services.
     *
     * @return
     *
     * @throws NotAvailableException
     */
    @Override
    default IlluminanceState getIlluminanceState() throws NotAvailableException {
        return IlluminanceStateProviderService.super.getIlluminanceState();
    }

    /**
     * Compute the average illuminance measured by the underlying services with given unitType.
     *
     * @param unitType
     *
     * @return
     *
     * @throws NotAvailableException
     */
    IlluminanceState getIlluminanceState(final UnitType unitType) throws NotAvailableException;

}
