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
import java.util.HashMap;
import java.util.Map;
import org.openbase.bco.registry.lib.util.DeviceConfigUtils;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.Registry;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DalUnitLabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final Registry<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>> deviceClassRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry;
    private final Map<String, String> oldUnitHostLabelMap;

    public DalUnitLabelConsistencyHandler(final Registry<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>> deviceClassRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry) {
        this.deviceClassRegistry = deviceClassRegistry;
        this.deviceRegistry = deviceRegistry;
        this.oldUnitHostLabelMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder dalUnitConfig = entry.getMessage().toBuilder();

        if (!dalUnitConfig.hasUnitHostId() || dalUnitConfig.getUnitHostId().isEmpty()) {
            throw new NotAvailableException("unitConfig.unitHostId");
        }

        UnitConfig deviceUnitConfig = deviceRegistry.getMessage(dalUnitConfig.getUnitHostId());
        DeviceClass deviceClass = deviceClassRegistry.get(deviceUnitConfig.getDeviceConfig().getDeviceClassId()).getMessage();

        if (!oldUnitHostLabelMap.containsKey(dalUnitConfig.getId())) {
            oldUnitHostLabelMap.put(dalUnitConfig.getId(), deviceUnitConfig.getLabel());
        }

        boolean hasDuplicatedUnitType = DeviceConfigUtils.checkDuplicatedUnitType(deviceUnitConfig, deviceClass, registry);

        // Setup device label if unit has no label configured.
        if (!dalUnitConfig.hasLabel() || dalUnitConfig.getLabel().isEmpty()) {
            if (DeviceConfigUtils.setupUnitLabelByDeviceConfig(dalUnitConfig, deviceUnitConfig, deviceClass, hasDuplicatedUnitType)) {
                throw new EntryModification(entry.setMessage(dalUnitConfig), this);
            }
        }

        String oldLabel = oldUnitHostLabelMap.get(dalUnitConfig.getId());
        if (!oldLabel.equals(deviceUnitConfig.getLabel())) {
            oldUnitHostLabelMap.put(dalUnitConfig.getId(), deviceUnitConfig.getLabel());
            if (dalUnitConfig.getLabel().equals(oldLabel)) {
                if (DeviceConfigUtils.setupUnitLabelByDeviceConfig(dalUnitConfig, deviceUnitConfig, deviceClass, hasDuplicatedUnitType)) {
                    throw new EntryModification(entry.setMessage(dalUnitConfig), this);
                }
            }
        }
    }
}
