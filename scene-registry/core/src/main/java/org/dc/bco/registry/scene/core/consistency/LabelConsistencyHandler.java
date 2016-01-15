/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.scene.core.consistency;

import java.util.HashMap;
import java.util.Map;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class LabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, SceneConfig, SceneConfig.Builder> {

    private final Map<String, SceneConfig> sceneMap;

    public LabelConsistencyHandler() {
        this.sceneMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, SceneConfig, SceneConfig.Builder> entry, ProtoBufMessageMapInterface<String, SceneConfig, SceneConfig.Builder> entryMap, ProtoBufRegistryInterface<String, SceneConfig, SceneConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
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
