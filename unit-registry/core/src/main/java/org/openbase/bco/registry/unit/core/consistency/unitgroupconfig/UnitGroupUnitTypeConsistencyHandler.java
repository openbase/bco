package org.openbase.bco.registry.unit.core.consistency.unitgroupconfig;

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
import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupUnitTypeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, UnitRegistryData.Builder> unitTemplateRegistry;

    public UnitGroupUnitTypeConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, UnitRegistryData.Builder> unitTemplateRegistry) {
        this.unitTemplateRegistry = unitTemplateRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitGroupUnitConfig = entry.getMessage().toBuilder();
        UnitGroupConfig.Builder unitGroup = unitGroupUnitConfig.getUnitGroupConfigBuilder();

        if (unitGroup.hasUnitType() && !(unitGroup.getUnitType() == UnitType.UNKNOWN)) {
            if (unitGroup.getServiceTemplateList().isEmpty()) {
                unitGroup.addAllServiceTemplate(unitTemplateRegistry.get(unitGroup.getUnitType().toString()).getMessage().getServiceTemplateList());
                throw new EntryModification(entry.setMessage(unitGroupUnitConfig), this);
            }
            if (!unitTemplateHasSameServices(unitGroup.getUnitType(), unitGroup.getServiceTemplateList())) {
                unitGroup.setUnitType(UnitType.UNKNOWN);
                throw new EntryModification(entry.setMessage(unitGroupUnitConfig), this);
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
