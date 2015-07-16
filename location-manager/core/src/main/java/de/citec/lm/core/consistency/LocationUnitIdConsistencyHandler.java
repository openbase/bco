/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.core.consistency;

import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.state.InventoryStateType;
import rst.homeautomation.unit.UnitConfigType;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class LocationUnitIdConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    private final DeviceRegistryRemote deviceRegistryRemote;

    public LocationUnitIdConsistencyHandler(final DeviceRegistryRemote deviceRegistryRemote) {
        this.deviceRegistryRemote = deviceRegistryRemote;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        LocationConfigType.LocationConfig locationConfig = entry.getMessage();

        Collection<String> lookupUnitIds;

        try {
            lookupUnitIds = lookupUnitIds(locationConfig);
        } catch (NotAvailableException ex) {
            lookupUnitIds = new ArrayList<>();
        }

        // verify and update unit ids
        if (generateListHash(locationConfig.getUnitIdList()) != generateListHash(lookupUnitIds)) {
            entry.setMessage(locationConfig.toBuilder().clearUnitId().addAllUnitId(lookupUnitIds));
            throw new EntryModification(entry, this);
        }
    }

    private Collection<String> lookupUnitIds(final LocationConfig locationConfig) throws CouldNotPerformException {
        try {

            if (!locationConfig.hasId() || locationConfig.getId().isEmpty()) {
                throw new NotAvailableException("location id");
            }

            Set<String> unitIdList = new HashSet<>();
            try {
                for (UnitConfigType.UnitConfig unitConfig : deviceRegistryRemote.getUnitConfigs()) {

                    // skip units with no placenment config
                    if (!unitConfig.hasPlacementConfig()) {
                        continue;
                    } else if (!unitConfig.getPlacementConfig().hasLocationId()) {
                        continue;
                    } else if (unitConfig.getPlacementConfig().getLocationId().isEmpty()) {
                        continue;
                    }

                    // filter units which are currently not installed
                    DeviceConfigType.DeviceConfig deviceConfig = deviceRegistryRemote.getDeviceConfigById(unitConfig.getDeviceId());
                    if (deviceConfig.getInventoryState().getValue() != InventoryStateType.InventoryState.State.INSTALLED) {
                        continue;
                    }

                    // add direct assigned units
                    if (unitConfig.getPlacementConfig().getLocationId().equals(locationConfig.getId())) {
                        unitIdList.add(unitConfig.getId());
                    }

                    // add child units
                    for (LocationConfig childLocationConfig : locationConfig.getChildList()) {
                        unitIdList.addAll(childLocationConfig.getUnitIdList());
                    }
                }
            } catch (NotAvailableException ex) {
                // no units available for this location...
            }
            return unitIdList;
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Unit id loockup failed!", ex);
        }
    }

    private int generateListHash(Collection<String> list) {
        int hash = 0;
        for (String entry : list) {
            hash += entry.hashCode();
        }
        return hash;
    }

    @Override
    public void reset() {
    }
}
