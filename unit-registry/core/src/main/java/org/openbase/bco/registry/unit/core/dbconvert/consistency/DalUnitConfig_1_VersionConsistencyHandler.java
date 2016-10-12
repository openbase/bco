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
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
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
import rst.domotic.unit.device.DeviceConfigType.DeviceConfig;

/**
 * DBVersuionConsistencyHandler that updates old unitHostIds in dalUnitConfigs to the newly generated UUIDs of deviceConfigs.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DalUnitConfig_1_VersionConsistencyHandler extends AbstractVersionConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private UnitRegistryRemote unitRegistry;
    private final Map<String, String> deviceConfigIdMap;

    public DalUnitConfig_1_VersionConsistencyHandler(final DBVersionControl versionControl, final FileSynchronizedRegistry<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> registry) throws InstantiationException, InterruptedException, org.openbase.jul.exception.InstantiationException {
        super(versionControl, registry);
        try {
            this.deviceConfigIdMap = new HashMap<>();
            unitRegistry = new UnitRegistryRemote();
            unitRegistry.init();
            unitRegistry.activate();
            unitRegistry.waitForData();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder dalUnitConfig = entry.getMessage().toBuilder();

        if (deviceConfigIdMap.isEmpty()) {
            try {
                for (UnitConfig deviceUnitConfig : unitRegistry.getDeviceUnitConfigRemoteRegistry().getMessages()) {
                    deviceConfigIdMap.put(getOldDeviceConfigId(deviceUnitConfig.getDeviceConfig()), deviceUnitConfig.getId());
                }
            } catch (CouldNotPerformException ex) {
                deviceConfigIdMap.clear();
                throw new CouldNotPerformException("Could not build deviceClass id map!", ex);
            }
        }

        dalUnitConfig.setUnitHostId(deviceConfigIdMap.get(dalUnitConfig.getUnitHostId()));
        throw new EntryModification(entry.setMessage(dalUnitConfig), this);
    }

    /**
     * Get the id of a deviceConfig before the change to UUIDs.
     *
     * @param devicConfig the deviceCibfug for which the old ID is generated
     * @return the old ID for the deviceConfig
     * @throws CouldNotPerformException if some fields in the deviceConfig which are needed are not set
     */
    public String getOldDeviceConfigId(DeviceConfig devicConfig) throws CouldNotPerformException {
        try {
            if (!devicConfig.hasDeviceClassId()) {
                throw new InvalidStateException("Field [DeviceClassId] is missing!");
            }

            if (devicConfig.getDeviceClassId().isEmpty()) {
                throw new InvalidStateException("Field [DeviceClass.id] is empty!");
            }

            if (!devicConfig.hasSerialNumber()) {
                throw new InvalidStateException("Field [SerialNumber] is missing!");
            }

            if (devicConfig.getSerialNumber().isEmpty()) {
                throw new InvalidStateException("Field [SerialNumber] is empty!");
            }

            String id;

            id = devicConfig.getDeviceClassId();
            id += "_";
            id += devicConfig.getSerialNumber();
            return StringProcessor.transformToIdString(id);

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }

}
