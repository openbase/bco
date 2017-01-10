package org.openbase.bco.registry.unit.core.consistency.deviceconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import org.openbase.bco.registry.lib.util.LocationUtils;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.jp.JPRecoverDB;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DeviceLocationIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;

    public DeviceLocationIdConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry) {
        this.locationRegistry = locationRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        // check if placementconfig is available
        if (!unitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("device.placementconfig");
        }

        // setup base location of device has no location configured.
        if (!unitConfig.getPlacementConfig().hasLocationId() || unitConfig.getPlacementConfig().getLocationId().isEmpty()) {
            unitConfig.setPlacementConfig(PlacementConfig.newBuilder(unitConfig.getPlacementConfig()).setLocationId(LocationUtils.getRootLocation(locationRegistry.getMessages()).getId()));
            throw new EntryModification(entry.setMessage(unitConfig), this);
        }

        // verify if configured location exists.
        if (!locationRegistry.contains(unitConfig.getPlacementConfig().getLocationId())) {
            try {
                if (!JPService.getProperty(JPRecoverDB.class).getValue()) {
                    throw new InvalidStateException("The configured Location[" + unitConfig.getPlacementConfig().getLocationId() + "] of Device[" + unitConfig.getId() + "] is unknown!");
                }
            } catch (JPServiceException ex) {
                throw new InvalidStateException("The configured Location[" + unitConfig.getPlacementConfig().getLocationId() + "] of Device[" + unitConfig.getId() + "] is unknown and can not be recovered!", ex);
            }
            // recover device location with root location.
            unitConfig.setPlacementConfig(PlacementConfig.newBuilder(unitConfig.getPlacementConfig()).setLocationId(LocationUtils.getRootLocation(locationRegistry.getMessages()).getId()));
            throw new EntryModification(entry.setMessage(unitConfig), this);
        }
    }

    private String getRootLocationId() throws CouldNotPerformException {
        for (UnitConfig location : locationRegistry.getMessages()) {
            if (location.getLocationConfig().getRoot()) {
                return location.getId();
            }
        }
        throw new NotAvailableException("rootLocation");
    }
}
