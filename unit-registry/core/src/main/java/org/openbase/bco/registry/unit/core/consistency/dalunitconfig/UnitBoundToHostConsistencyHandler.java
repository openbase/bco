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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.Registry;
import rst.domotic.registry.UnitRegistryDataType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.device.DeviceClassType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitBoundToHostConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    public static final boolean DEFAULT_BOUND_TO_DEVICE = true;

    private final Registry<String, IdentifiableMessage<String, DeviceClassType.DeviceClass, DeviceClassType.DeviceClass.Builder>> deviceClassRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryDataType.UnitRegistryData.Builder> deviceRegistry;

    public UnitBoundToHostConsistencyHandler(final Registry<String, IdentifiableMessage<String, DeviceClassType.DeviceClass, DeviceClassType.DeviceClass.Builder>> deviceClassRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryDataType.UnitRegistryData.Builder> deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
        this.deviceClassRegistry = deviceClassRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder dalUnitConfig = entry.getMessage().toBuilder();

        boolean modification = false;
        final UnitConfig deviceUnitConfig = deviceRegistry.getMessage(dalUnitConfig.getUnitHostId());

        // Setup default bounding
        if (!dalUnitConfig.hasBoundToUnitHost()) {
            dalUnitConfig.setBoundToUnitHost(deviceUnitConfig.getBoundToUnitHost());
            modification = true;
        }

        // Overwrite unit bounds by device bounds if device bounds is true and unit bounds 
        if (deviceUnitConfig.getBoundToUnitHost() && !dalUnitConfig.getBoundToUnitHost()) {
            dalUnitConfig.setBoundToUnitHost(deviceUnitConfig.getBoundToUnitHost());
            modification = true;
        }

        // Copy device placement and location if bound to host
        if (dalUnitConfig.getBoundToUnitHost()) {

            // copy location id
            if (!dalUnitConfig.getPlacementConfig().getLocationId().equals(deviceUnitConfig.getPlacementConfig().getLocationId())) {
                dalUnitConfig.getPlacementConfigBuilder().setLocationId(deviceUnitConfig.getPlacementConfig().getLocationId());
                logger.debug("Updated location to : " + deviceUnitConfig.getPlacementConfig().getLocationId());
                modification = true;
            }
 
            // copy position
            if (deviceUnitConfig.getPlacementConfig().hasPosition() && !dalUnitConfig.getPlacementConfig().getPosition().equals(deviceUnitConfig.getPlacementConfig().getPosition())) {
                dalUnitConfig.getPlacementConfigBuilder().setPosition(deviceUnitConfig.getPlacementConfig().getPosition());
                modification = true;
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(dalUnitConfig), this);
        }
    }
}
