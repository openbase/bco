package org.openbase.bco.registry.unit.core.consistency.appconfig;

/*
 * #%L
 * BCO Registry Unit Core
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
import java.util.HashMap;
import java.util.Map;

import javafx.beans.DefaultProperty;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
@Deprecated
public class AppLabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final Map<String, UnitConfig> appMap;

    public AppLabelConsistencyHandler() {
        this.appMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig app = entry.getMessage();

        if (!app.hasLabel() || app.getLabel().isEmpty()) {
            throw new NotAvailableException("app.label");
        }

        if (!app.hasPlacementConfig()) {
            throw new NotAvailableException("agent.placementConfig");
        }

        if (!app.getPlacementConfig().hasLocationId() || app.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("app.placementConfig.locationId");
        }

        String key = app.getLabel() + app.getPlacementConfig().getLocationId();
        if (!appMap.containsKey(key)) {
            appMap.put(key, app);
        } else {
            throw new InvalidStateException("App [" + app + "] and app [" + appMap.get(key) + "] are registered with the same label at the same location");
        }
    }

    @Override
    public void reset() {
        appMap.clear();
    }
}
