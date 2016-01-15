/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.scene.core.consistency;

import java.util.Map;
import java.util.TreeMap;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.rsb.ScopeType.Scope;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, SceneConfig, SceneConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;

    public ScopeConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, SceneConfig, SceneConfig.Builder> entry, ProtoBufMessageMapInterface<String, SceneConfig, SceneConfig.Builder> entryMap, ProtoBufRegistryInterface<String, SceneConfig, SceneConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        SceneConfig sceneConfig = entry.getMessage();

        if (!sceneConfig.hasLocationId()) {
            throw new NotAvailableException("sceneConfig.locationId");
        }
        if (sceneConfig.getLocationId().isEmpty()) {
            throw new NotAvailableException("Field sceneConfig.locationId is empty");
        }

        Scope newScope = ScopeGenerator.generateSceneScope(sceneConfig, locationRegistryRemote.getLocationConfigById(sceneConfig.getLocationId()));

        // verify and update scope
        if (!ScopeGenerator.generateStringRep(sceneConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            entry.setMessage(sceneConfig.toBuilder().setScope(newScope));
            throw new EntryModification(entry, this);
        }
    }

    @Override
    public void reset() {
    }
}
