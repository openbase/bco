package org.openbase.bco.dal.lib.layer.unit;

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

import org.openbase.bco.dal.lib.layer.service.OperationServiceFactoryProvider;
import org.openbase.bco.dal.lib.layer.service.UnitDataSourceFactoryProvider;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface HostUnitManager extends OperationServiceFactoryProvider, UnitDataSourceFactoryProvider {

    /**
     * Check if the given gateway is supported by this manager instance.
     *
     * @param clazz the class of the unit to check.
     *
     * @return true if supported.
     */
    boolean isGatewaySupported(final GatewayClass clazz);

    /**
     * Check if the given unit is supported by this manager instance.
     *
     * @param config the config of the unit to check.
     *
     * @return true if supported.
     */
    boolean isUnitSupported(final UnitConfig config);
}
