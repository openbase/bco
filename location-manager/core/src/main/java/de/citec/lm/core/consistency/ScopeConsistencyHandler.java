/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.extension.rsb.scope.ScopeGenerator;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import rst.rsb.ScopeType;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class ScopeConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        LocationConfigType.LocationConfig locationConfig = entry.getMessage();

        ScopeType.Scope newScope = ScopeGenerator.generateLocationScope(locationConfig, entryMap);

        // verify and update scope
		if(!ScopeGenerator.generateStringRep(locationConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            entry.setMessage(locationConfig.toBuilder().setScope(newScope));
			throw new EntryModification(entry, this);
		}
    }

    @Override
    public void reset() {
    }
}