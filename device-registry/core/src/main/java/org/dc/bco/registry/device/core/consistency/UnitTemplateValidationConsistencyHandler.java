/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core.consistency;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class UnitTemplateValidationConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitTemplate, UnitTemplate.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder> entry, ProtoBufMessageMapInterface<String, UnitTemplate, UnitTemplate.Builder> entryMap, ProtoBufRegistryInterface<String, UnitTemplate, UnitTemplate.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitTemplate.Builder unitTemplate = entry.getMessage().toBuilder();
        
        // remove invalid unit template
        if(!unitTemplate.getId().equals(registry.getIdGenerator().generateId(unitTemplate.build()))) {
            registry.remove(entry);
            throw new EntryModification(entry, this);
        }
    }

    @Override
    public void reset() {
    }
}