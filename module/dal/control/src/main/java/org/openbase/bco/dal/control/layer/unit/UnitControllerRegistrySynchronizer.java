package org.openbase.bco.dal.control.layer.unit;

/*-
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.Factory;
import org.openbase.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.openbase.jul.storage.registry.RemoteRegistry;
import org.openbase.jul.storage.registry.SynchronizableRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;

public class UnitControllerRegistrySynchronizer<CONTROLLER extends UnitController<?,?>> extends ActivatableEntryRegistrySynchronizer<String, CONTROLLER, UnitConfig, Builder> {

    public UnitControllerRegistrySynchronizer(final SynchronizableRegistry<String, CONTROLLER> registry, RemoteRegistry<String, UnitConfig, UnitConfig.Builder> remoteRegistry, Factory<CONTROLLER, UnitConfig> factory) throws InstantiationException {
        super(registry, remoteRegistry, Registries.getUnitRegistry(UnitControllerRegistrySynchronizer.class), factory);

        // filter non enabled/active units.
        addFilter(unitConfig -> !activationCondition(unitConfig));
    }

    @Override
    public boolean activationCondition(final UnitConfig unitConfig) {
        return UnitConfigProcessor.isEnabled(unitConfig);
    }
}
