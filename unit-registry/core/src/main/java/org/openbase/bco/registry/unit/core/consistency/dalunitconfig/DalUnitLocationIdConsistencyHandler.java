package org.openbase.bco.registry.unit.core.consistency.dalunitconfig;

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
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.spatial.PlacementConfigType;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DalUnitLocationIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry;

    public DalUnitLocationIdConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry) {
        this.locationRegistry = locationRegistry;
        this.deviceRegistry = deviceRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder dalUnitConfig = entry.getMessage().toBuilder();

        boolean modification = false;

        // Check if placement is available
        if (!dalUnitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("unit.placementconfig");
        }

        // Setup device location if unit has no location configured.
        if (!dalUnitConfig.getPlacementConfig().hasLocationId() || dalUnitConfig.getPlacementConfig().getLocationId().isEmpty()) {
            if (!dalUnitConfig.hasUnitHostId() || dalUnitConfig.getUnitHostId().isEmpty()) {
                throw new NotAvailableException("unitConfig.unitHostId");
            }

            UnitConfig deviceUnitConfig = deviceRegistry.getMessage(dalUnitConfig.getUnitHostId());

            // Check if device placement is available
            if (!deviceUnitConfig.hasPlacementConfig()) {
                throw new NotAvailableException("device.placementconfig");
            }

            if (!deviceUnitConfig.getPlacementConfig().hasLocationId() || deviceUnitConfig.getPlacementConfig().getLocationId().isEmpty()) {
                throw new NotAvailableException("device.placementconfig.locationid");
            }

            dalUnitConfig.setPlacementConfig(PlacementConfigType.PlacementConfig.newBuilder(dalUnitConfig.getPlacementConfig()).setLocationId(deviceUnitConfig.getPlacementConfig().getLocationId()));
            modification = true;
        }

        // verify if configured location exists.
        if (!locationRegistry.contains(dalUnitConfig.getPlacementConfig().getLocationId())) {
            try {
                if (!JPService.getProperty(JPRecoverDB.class).getValue()) {
                    throw new InvalidStateException("The configured Location[" + dalUnitConfig.getPlacementConfig().getLocationId() + "] of Unit[" + dalUnitConfig.getId() + "] is unknown!");
                }
            } catch (JPServiceException ex) {
                throw new InvalidStateException("The configured Location[" + dalUnitConfig.getPlacementConfig().getLocationId() + "] of Unit[" + dalUnitConfig.getId() + "] is unknown and can not be recovered!", ex);
            }
            // recover unit location with device location.
            dalUnitConfig.setPlacementConfig(PlacementConfigType.PlacementConfig.newBuilder(dalUnitConfig.getPlacementConfig()).setLocationId(deviceRegistry.getMessage(dalUnitConfig.getUnitHostId()).getPlacementConfig().getLocationId()));
            modification = true;
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(dalUnitConfig), this);
        }
    }
}
