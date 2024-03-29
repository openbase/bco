package org.openbase.bco.registry.unit.lib.generator;

/*-
 * #%L
 * BCO Registry Unit Library
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

import org.openbase.bco.registry.clazz.lib.ClassRegistry;
import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.communication.ScopeType.Scope;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

public class GenericUnitScopeGenerator implements UnitScopeGenerator {

    private static UnitScopeGenerator instance;

    public synchronized static UnitScopeGenerator getInstance() {
        if (instance == null) {
            instance = new UnitIdScopeGenerator();
        }

        return instance;
    }

    public static Scope generateScope(UnitConfig unitConfig, UnitRegistry unitRegistry) throws CouldNotPerformException {
        return getInstance().generateScope(unitConfig, unitRegistry, CachedClassRegistryRemote.getRegistry());
    }

    @Override
    public Scope generateScope(UnitConfig unitConfig, UnitRegistry unitRegistry, ClassRegistry classRegistry) throws CouldNotPerformException {
        return getInstance().generateScope(unitConfig, unitRegistry, classRegistry);
    }
}
