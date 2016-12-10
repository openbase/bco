package org.openbase.bco.registry.unit.core.consistency.dalunitconfig;

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
import org.openbase.bco.registry.lib.util.DeviceConfigUtils;
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
            System.out.println("add BoundToUnitHost flag for " + dalUnitConfig.getLabel());
            modification = true;
        }

        // Overwrite unit bounds by device bounds
        if (deviceUnitConfig.getBoundToUnitHost() && !dalUnitConfig.getBoundToUnitHost()) {
            dalUnitConfig.setBoundToUnitHost(true);
        }

        // Copy device placement, location and label if bound to device is enabled.
        if (dalUnitConfig.getBoundToUnitHost()) {
            final DeviceClassType.DeviceClass deviceClass = deviceClassRegistry.get(deviceUnitConfig.getDeviceConfig().getDeviceClassId()).getMessage();

            boolean hasDuplicatedUnitType = DeviceConfigUtils.checkDuplicatedUnitType(deviceUnitConfig, deviceClass, registry);

//            if (dalUnitConfig.getLabel().equals("Temperature_Controller_Unit_Test")) {
//                System.out.println("break;");
//            }
            // Setup device label if unit has no label configured.
            UnitConfig.Builder unitConfigCopy = UnitConfig.newBuilder(dalUnitConfig.build());
            DeviceConfigUtils.setupUnitLabelByDeviceConfig(unitConfigCopy, deviceUnitConfig, deviceClass, hasDuplicatedUnitType);

            if (!unitConfigCopy.getLabel().equals(dalUnitConfig)) {
                if (unitConfigCopy.getLabel().startsWith(deviceUnitConfig.getLabel())) {
                    modification = modification || DeviceConfigUtils.setupUnitLabelByDeviceConfig(dalUnitConfig, deviceUnitConfig, deviceClass, hasDuplicatedUnitType);
                }
            }

            if (modification) {
                System.out.println("Updated label to : " + dalUnitConfig.getLabel());
            }

            // copy location id
            if (!dalUnitConfig.getPlacementConfig().getLocationId().equals(deviceUnitConfig.getPlacementConfig().getLocationId())) {
                dalUnitConfig.getPlacementConfigBuilder().setLocationId(deviceUnitConfig.getPlacementConfig().getLocationId());
                System.out.println("Updated location to : " + deviceUnitConfig.getPlacementConfig().getLocationId());
                modification = true;
            }

            // copy position
            if (!dalUnitConfig.getPlacementConfig().getPosition().equals(deviceUnitConfig.getPlacementConfig().getPosition())) {
                dalUnitConfig.getPlacementConfigBuilder().setPosition(deviceUnitConfig.getPlacementConfig().getPosition());
                modification = true;
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(dalUnitConfig), this);
        }
    }
}
