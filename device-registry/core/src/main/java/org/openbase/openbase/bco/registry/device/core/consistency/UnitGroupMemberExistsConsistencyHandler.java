package org.openbase.bco.registry.device.core.consistency;

/*
 * #%L
 * REM DeviceRegistry Core
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

import java.util.ArrayList;
import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UnitGroupMemberExistsConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitGroupConfig, UnitGroupConfig.Builder> {

    private ProtoBufFileSynchronizedRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> deviceConfigRegistry;

    public UnitGroupMemberExistsConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> deviceConfigRegistry) {
        this.deviceConfigRegistry = deviceConfigRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitGroupConfig, UnitGroupConfig.Builder> entry, ProtoBufMessageMapInterface<String, UnitGroupConfig, UnitGroupConfig.Builder> entryMap, ProtoBufRegistryInterface<String, UnitGroupConfig, UnitGroupConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitGroupConfig.Builder unitGroup = entry.getMessage().toBuilder();

        // remove a member id if the according unit does not exists.. is the case if the unit gets removed globally
        boolean modification = false;
        List<String> unitConfigsIds = new ArrayList<>();
        unitGroup.clearMemberId();
        for (String unitConfigId : entry.getMessage().getMemberIdList()) {
            if (containsUnitConfigById(unitConfigId)) {
                unitConfigsIds.add(unitConfigId);
            } else {
                modification = true;
            }
        }
        unitGroup.addAllMemberId(unitConfigsIds);

        if (modification) {
            throw new EntryModification(entry.setMessage(unitGroup.build()), this);
        }
    }

    public boolean containsUnitConfigById(String unitConfigId) throws CouldNotPerformException {
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRegistry.getEntries()) {
            for (UnitConfig unitConfig : deviceConfig.getMessage().getUnitConfigList()) {
                if (unitConfig.getId().equals(unitConfigId)) {
                    return true;
                }
            }
        }
        return false;
    }
}
