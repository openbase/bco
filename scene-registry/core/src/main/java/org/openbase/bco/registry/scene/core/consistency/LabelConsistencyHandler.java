package org.openbase.bco.registry.scene.core.consistency;

/*
 * #%L
 * REM SceneRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, SceneConfig, SceneConfig.Builder> {

    private final Map<String, SceneConfig> sceneMap;

    public LabelConsistencyHandler() {
        this.sceneMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, SceneConfig, SceneConfig.Builder> entry, ProtoBufMessageMap<String, SceneConfig, SceneConfig.Builder> entryMap, ProtoBufRegistry<String, SceneConfig, SceneConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        SceneConfig scene = entry.getMessage();

        if (!scene.hasLabel() || scene.getLabel().isEmpty()) {
            throw new NotAvailableException("scene.label");
        }

        if (!scene.hasLocationId() || scene.getLocationId().isEmpty()) {
            throw new NotAvailableException("scene.locationId");
        }

        String key = scene.getLabel() + scene.getLocationId();
        if (!sceneMap.containsKey(key)) {
            sceneMap.put(key, scene);
        } else {
            throw new InvalidStateException("Scene [" + scene + "] and scene [" + sceneMap.get(key) + "] are registered with the same label at the same location");
        }
    }

    @Override
    public void reset() {
        sceneMap.clear();
    }
}
