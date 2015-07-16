/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.extension.rsb.scope.ScopeGenerator;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import de.citec.lm.remote.LocationRegistryRemote;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 */
public class DeviceScopeConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;
    
    public DeviceScopeConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
    }
    
    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig deviceConfig = entry.getMessage();

        ScopeType.Scope newScope = ScopeGenerator.generateDeviceScope(deviceConfig, locationRegistryRemote.getLocationConfigById(deviceConfig.getPlacementConfig().getLocationId()));

        // verify and update scope
		if(!ScopeGenerator.generateStringRep(deviceConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            entry.setMessage(deviceConfig.toBuilder().setScope(newScope));
			throw new EntryModification(entry, this);
		}
    }
    
       @Override
    public void reset() {
    }
}