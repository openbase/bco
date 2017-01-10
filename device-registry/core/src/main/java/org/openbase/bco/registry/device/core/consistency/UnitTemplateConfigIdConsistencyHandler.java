package org.openbase.bco.registry.device.core.consistency;

/*
 * #%L
 * BCO Registry Device Core
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
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import java.util.Map;
import java.util.TreeMap;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitTemplateConfigIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceClass, DeviceClass.Builder> {

    private final Map<String, UnitTemplateConfig> unitTemplateMap;

    public UnitTemplateConfigIdConsistencyHandler() {
        this.unitTemplateMap = new TreeMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder> entry, ProtoBufMessageMap<String, DeviceClass, DeviceClass.Builder> entryMap, ProtoBufRegistry<String, DeviceClass, DeviceClass.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceClass.Builder deviceClass = entry.getMessage().toBuilder();

        deviceClass.clearUnitTemplateConfig();
        boolean modification = false;
        for (UnitTemplateConfig.Builder unitTemplateConfig : entry.getMessage().toBuilder().getUnitTemplateConfigBuilderList()) {
            if (!unitTemplateConfig.hasId() || unitTemplateConfig.getId().isEmpty()) {
                unitTemplateConfig.setId(generateUnitTemplateConfigId(deviceClass.getId(), unitTemplateConfig));
                modification = true;
            }
            unitTemplateMap.put(unitTemplateConfig.getId(), unitTemplateConfig.build());
            deviceClass.addUnitTemplateConfig(unitTemplateConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceClass), this);
        }
    }

    private String generateUnitTemplateConfigId(String deviceClassId, UnitTemplateConfig.Builder unitTemplateConfig) {
        int number = 0;
        String unitConfigTemplateTypeId = deviceClassId + "_" + unitTemplateConfig.getType().name() + "_" + number;
        while (unitTemplateMap.containsKey(unitConfigTemplateTypeId)) {
            number++;
            unitConfigTemplateTypeId = deviceClassId + "_" + unitTemplateConfig.getType().name() + "_" + number;
        }
        return unitConfigTemplateTypeId;
    }

    @Override
    public void reset() {
        unitTemplateMap.clear();
    }
}
