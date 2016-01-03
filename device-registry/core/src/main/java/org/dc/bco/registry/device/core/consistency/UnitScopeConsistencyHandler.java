/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core.consistency;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 */
public class UnitScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

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
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }

    @Override
    public void reset() {
    }
}
