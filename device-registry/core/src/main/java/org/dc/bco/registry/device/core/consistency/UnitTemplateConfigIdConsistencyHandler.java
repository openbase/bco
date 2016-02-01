/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core.consistency;

/*
 * #%L
 * REM DeviceRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.Map;
import java.util.TreeMap;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.unit.UnitTemplateConfigType.UnitTemplateConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UnitTemplateConfigIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceClass, DeviceClass.Builder> {

    private final Map<String, UnitTemplateConfig> unitTemplateMap;

    public UnitTemplateConfigIdConsistencyHandler() {
        this.unitTemplateMap = new TreeMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder> entry, ProtoBufMessageMapInterface<String, DeviceClass, DeviceClass.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceClass, DeviceClass.Builder> registry) throws CouldNotPerformException, EntryModification {
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
