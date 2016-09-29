package org.openbase.bco.registry.device.core.consistency;

/*
 * #%L
 * REM DeviceRegistryData Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryDataType.DeviceRegistryData;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitBoundsToDeviceConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    public static final boolean DEFAULT_BOUND_TO_DEVICE = true;

    private final ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistryData.Builder> deviceClassRegistry;

    public UnitBoundsToDeviceConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistryData.Builder> deviceClassRegistry) throws InstantiationException {
        this.deviceClassRegistry = deviceClassRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMap<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistry<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        deviceConfig.clearUnitConfig();
        boolean modification = false;
        for (UnitConfig.Builder unitConfig : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {

            // Setup default bounding
            if (!unitConfig.hasBoundToUnitHost()) {
                unitConfig.setBoundToUnitHost(DEFAULT_BOUND_TO_DEVICE);
                modification = true;
            }

            // Copy device placement and label if bound to device is enabled.
            if (unitConfig.getBoundToUnitHost()) {

                // copy location id
                if (!unitConfig.getPlacementConfig().getLocationId().equals(deviceConfig.getPlacementConfig().getLocationId())) {
                    unitConfig.getPlacementConfigBuilder().setLocationId(deviceConfig.getPlacementConfig().getLocationId());
                    modification = true;
                }

                // copy position
                if (!unitConfig.getPlacementConfig().getPosition().equals(deviceConfig.getPlacementConfig().getPosition())) {
                    unitConfig.getPlacementConfigBuilder().setPosition(deviceConfig.getPlacementConfig().getPosition());
                    modification = true;
                }
            }
            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }
}
