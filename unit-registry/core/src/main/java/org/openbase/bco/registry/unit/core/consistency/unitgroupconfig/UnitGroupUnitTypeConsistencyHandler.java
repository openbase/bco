package org.openbase.bco.registry.unit.core.consistency.unitgroupconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;

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
            if (unitGroup.getServiceDescriptionList().isEmpty()) {
                unitGroup.addAllServiceDescription(getUnitTemplateByType(unitGroup.getUnitType()).getServiceDescriptionList());
                throw new EntryModification(entry.setMessage(unitGroupUnitConfig), this);
            }
            if (!unitTemplateHasSameServices(unitGroup.getUnitType(), unitGroup.getServiceDescriptionList())) {
                unitGroup.setUnitType(UnitType.UNKNOWN);
                throw new EntryModification(entry.setMessage(unitGroupUnitConfig), this);
            }
        }
    }

    private boolean unitTemplateHasSameServices(UnitType unitType, List<ServiceDescription> serviceDescription) throws CouldNotPerformException {
        UnitTemplate unitTemplate = getUnitTemplateByType(unitType);
        if (!serviceDescription.stream().noneMatch((serviceTemplate) -> (!unitTemplate.getServiceDescriptionList().contains(serviceTemplate)))) {
            return false;
        }
        return unitTemplate.getServiceDescriptionList().stream().noneMatch((serviceType) -> (!serviceDescription.contains(serviceType)));
    }

    private UnitTemplate getUnitTemplateByType(UnitType unitType) throws CouldNotPerformException {
        for (UnitTemplate unitTemplate : unitTemplateRegistry.getMessages()) {
            if (unitTemplate.getType() == unitType) {
                return unitTemplate;
            }
        }
        throw new NotAvailableException(UnitTemplate.class, unitType.name());
    }
}
