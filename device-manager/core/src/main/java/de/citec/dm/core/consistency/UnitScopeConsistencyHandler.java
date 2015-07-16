/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.extension.rsb.scope.ScopeGenerator;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import de.citec.lm.remote.LocationRegistryRemote;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 */
public class UnitScopeConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;

    public UnitScopeConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
    }
    
    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        deviceConfig.clearUnitConfig();
        boolean modification = false;
        for (UnitConfig.Builder unitConfig : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {
            UnitConfig unitConfigClone = UnitConfig.newBuilder(unitConfig.build()).build();

            if(!unitConfigClone.hasPlacementConfig()) {
                throw new NotAvailableException("placementconfig");
            }

            if(!unitConfigClone.getPlacementConfig().hasLocationId() || unitConfigClone.getPlacementConfig().getLocationId().isEmpty()) {
                throw new NotAvailableException("placementconfig.locationid");
            }

            ScopeType.Scope newScope = ScopeGenerator.generateUnitScope(unitConfigClone, locationRegistryRemote.getLocationConfigById(unitConfigClone.getPlacementConfig().getLocationId()));

            // verify and update scope
            if (!ScopeGenerator.generateStringRep(unitConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
                unitConfig.setScope(newScope);
                modification = true;
            }
            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig).getMessage(), this);
        }
    }

    @Override
    public void reset() {
    }
}
