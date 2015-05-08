/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.consistency;

import de.citec.csra.dm.generator.UnitConfigIdGenerator;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.extension.rsb.container.IdentifiableMessage;
import de.citec.jul.extension.rsb.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author mpohling
 */
public class UnitIdConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    //TODO mpohling: verify unit id.
//    private final Registry<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> registry;

    public UnitIdConsistencyHandler() throws InstantiationException {
//        try {
//            this.registry = new Registry<>();
//        } catch (InstantiationException ex) {
//            throw new InstantiationException(this, ex);
        
//        }
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        deviceConfig.clearUnitConfig();
        boolean modification = false;
        for (UnitConfig.Builder unitConfig : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {
            if (!unitConfig.hasId() || unitConfig.getId().isEmpty() || !unitConfig.getId().equals(UnitConfigIdGenerator.getInstance().generateId(unitConfig.build()))) {
                unitConfig.setId(UnitConfigIdGenerator.getInstance().generateId(unitConfig.build()));
                modification = true;
            }
            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig).getMessage(), this);
        }
    }

//    private String generateUnitId(final UnitConfigType.UnitConfigOrBuilder unitConfig) throws CouldNotPerformException {
//        try {
//            if (unitConfig.hasScope()) {
//                throw new NotAvailableException("unitconfig.scope");
//            }
//            return ScopeGenerator.generateStringRep(unitConfig.getScope());
//        } catch (CouldNotPerformException ex) {
//            throw new CouldNotPerformException("Could not generate unti id!");
//        }
//    }
}
