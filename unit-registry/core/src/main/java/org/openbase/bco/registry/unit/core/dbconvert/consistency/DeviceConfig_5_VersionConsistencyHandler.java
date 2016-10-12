package org.openbase.bco.registry.unit.core.dbconvert.consistency;

/*
 * #%L
 * BCO Registry Unit Core
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

import java.util.HashMap;
import java.util.Map;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.storage.registry.AbstractVersionConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.FileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.domotic.unit.device.DeviceConfigType.DeviceConfig;

/**
 * DBVersuionConsistencyHandler that updates old deviceClass IDs in DeviceConfigs to the newly generated UUIDs.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceConfig_5_VersionConsistencyHandler extends AbstractVersionConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private DeviceRegistryRemote deviceRegistry;
    private final Map<String, String> deviceClassIdMap;

    public DeviceConfig_5_VersionConsistencyHandler(final DBVersionControl versionControl, final FileSynchronizedRegistry<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> registry) throws InstantiationException, InterruptedException, org.openbase.jul.exception.InstantiationException {
        super(versionControl, registry);
        try {
            this.deviceClassIdMap = new HashMap<>();
            deviceRegistry = new DeviceRegistryRemote();
            deviceRegistry.init();
            deviceRegistry.activate();
            deviceRegistry.waitForData();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder deviceUnitConfig = entry.getMessage().toBuilder();
        DeviceConfig.Builder deviceConfig = deviceUnitConfig.getDeviceConfigBuilder();

        if (deviceClassIdMap.isEmpty()) {
            try {
                for (DeviceClass deviceClass : deviceRegistry.getDeviceClasses()) {
                    deviceClassIdMap.put(getOldDeviceClassId(deviceClass), deviceClass.getId());
                }
            } catch (CouldNotPerformException ex) {
                deviceClassIdMap.clear();
                throw new CouldNotPerformException("Could not build deviceClass id map!", ex);
            }
        }

        deviceConfig.setDeviceClassId(deviceClassIdMap.get(deviceConfig.getDeviceClassId()));
        throw new EntryModification(entry.setMessage(deviceUnitConfig), this);
    }

    /**
     * Get the id of a deviceClass before the change to UUIDs.
     *
     * @param deviceClass the device class for which the old ID is generated
     * @return the old ID for the deviceClass
     * @throws CouldNotPerformException if some fields in the deviceClass which are needed are not set
     */
    private String getOldDeviceClassId(DeviceClass deviceClass) throws CouldNotPerformException {
        String id;
        try {
            if (!deviceClass.hasProductNumber()) {
                throw new InvalidStateException("Field [ProductNumber] is missing!");
            }

            if (deviceClass.getProductNumber().isEmpty()) {
                throw new InvalidStateException("Field [ProductNumber] is empty!");
            }

            if (!deviceClass.hasCompany()) {
                throw new InvalidStateException("Field [Company] is missing!");
            }

            if (deviceClass.getCompany().isEmpty()) {
                throw new InvalidStateException("Field [Company] is empty!");
            }

            id = deviceClass.getCompany();
            id += "_";
            id += deviceClass.getProductNumber();

            return StringProcessor.transformToIdString(id);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }
}
