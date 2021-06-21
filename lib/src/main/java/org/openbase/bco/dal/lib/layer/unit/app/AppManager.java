package org.openbase.bco.dal.lib.layer.unit.app;

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

import org.openbase.bco.dal.lib.layer.unit.HostUnitManager;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface AppManager extends HostUnitManager {

    /**
     * Enables access of the controller registry of this manager.
     * <p>
     * Note: Mainly used for accessing the controller via test routines.
     *
     * @return the controller registry.
     */
    UnitControllerRegistry<AppController> getAppControllerRegistry();

    /**
     * All apps will be supported by default. Feel free to overwrite method
     * to changing this behavior.
     *
     * @param config
     *
     * @return true if supported
     */
    @Override
    default boolean isUnitSupported(final UnitConfig config) {
        return config.getUnitType() == UnitType.APP;
    }

    /**
     * All apps will be supported by default. Feel free to overwrite method
     * to changing this behavior.
     *
     * @param config
     *
     * @return true if supported
     */
    @Override
    default boolean isGatewaySupported(final UnitConfig config) {
        // gateways are not yet supported for apps.
        return false;
    }
}
