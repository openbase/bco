package org.openbase.bco.registry.lib.provider;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.List;

public interface UnitConfigCollectionProvider {

    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsUnitConfigById(final String unitConfigId) throws CouldNotPerformException;

    @RPCMethod
    public UnitConfig getUnitConfigById(final String unitConfigId) throws CouldNotPerformException;

    /**
     * Method returns a list of all globally registered units of the given {@code type}.
     * <p>
     * Note: The type {@code UnitType.UNKNOWN} is used as wildcard and will return a list of all registered units.
     *
     * @param type the unit type to filter.
     * @return a list of unit configurations.
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     */
    public List<UnitConfig> getUnitConfigs(final UnitType type) throws CouldNotPerformException;
}
