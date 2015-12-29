/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import de.citec.lm.remote.LocationRegistryRemote;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.rsb.ScopeType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class DeviceScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;

    public DeviceScopeConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig deviceConfig = entry.getMessage();

        if (!deviceConfig.hasPlacementConfig()) {
            throw new NotAvailableException("deviceconfig.placementconfig");
        }
        if (!deviceConfig.getPlacementConfig().hasLocationId() || deviceConfig.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("deviceconfig.placementconfig.locationid");
        }
        
        LocationConfig test = locationRegistryRemote.getLocationConfigById(deviceConfig.getPlacementConfig().getLocationId());
        ScopeType.Scope newScope = ScopeGenerator.generateDeviceScope(deviceConfig, test);

        // verify and update scope
        if (!ScopeGenerator.generateStringRep(deviceConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            entry.setMessage(deviceConfig.toBuilder().setScope(newScope));
            throw new EntryModification(entry, this);
        }
    }

    @Override
    public void reset() {
    }
}
