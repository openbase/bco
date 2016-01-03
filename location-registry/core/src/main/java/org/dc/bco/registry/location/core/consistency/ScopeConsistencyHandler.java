/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.consistency;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.rsb.ScopeType;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class ScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        LocationConfigType.LocationConfig locationConfig = entry.getMessage();

        ScopeType.Scope newScope = ScopeGenerator.generateLocationScope(locationConfig, entryMap);

        // verify and update scope
        if (!ScopeGenerator.generateStringRep(locationConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            entry.setMessage(locationConfig.toBuilder().setScope(newScope));
            throw new EntryModification(entry, this);
        }
    }

    @Override
    public void reset() {
    }
}
