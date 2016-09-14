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
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import java.util.List;
import rst.homeautomation.device.DeviceRegistryDataType.DeviceRegistryData;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UnitGroupUnitTypeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitGroupConfig, UnitGroupConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistryData.Builder> unitTemplateRegistry;

    public UnitGroupUnitTypeConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistryData.Builder> unitTemplateRegistry) {
        this.unitTemplateRegistry = unitTemplateRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitGroupConfig, UnitGroupConfig.Builder> entry, ProtoBufMessageMap<String, UnitGroupConfig, UnitGroupConfig.Builder> entryMap, ProtoBufRegistry<String, UnitGroupConfig, UnitGroupConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitGroupConfig.Builder unitGroup = entry.getMessage().toBuilder();
        if (unitGroup.hasUnitType() && !(unitGroup.getUnitType() == UnitType.UNKNOWN)) {
            if (unitGroup.getServiceTemplateList().isEmpty()) {
                throw new EntryModification(entry.setMessage(unitGroup.addAllServiceTemplate(unitTemplateRegistry.get(unitGroup.getUnitType().toString()).getMessage().getServiceTemplateList())), this);
            }
            if (!unitTemplateHasSameServices(unitGroup.getUnitType(), unitGroup.getServiceTemplateList())) {
                throw new EntryModification(entry.setMessage(unitGroup.setUnitType(UnitType.UNKNOWN)), this);
            }
        }
    }

    private boolean unitTemplateHasSameServices(UnitType unitType, List<ServiceTemplate> serviceTemplates) throws CouldNotPerformException {
        UnitTemplate unitTemplate = unitTemplateRegistry.get(unitType.toString()).getMessage();
        if (!serviceTemplates.stream().noneMatch((serviceTemplate) -> (!unitTemplate.getServiceTemplateList().contains(serviceTemplate)))) {
            return false;
        }
        return unitTemplate.getServiceTemplateList().stream().noneMatch((serviceType) -> (!serviceTemplates.contains(serviceType)));
    }
}
