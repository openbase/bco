package org.openbase.bco.registry.unit.core.consistency.unitgroupconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.StackTracePrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;

import java.util.List;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupUnitTypeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitGroupUnitConfig = entry.getMessage().toBuilder();
        UnitGroupConfig.Builder unitGroup = unitGroupUnitConfig.getUnitGroupConfigBuilder();

        if (unitGroup.hasUnitType() && !(unitGroup.getUnitType() == UnitType.UNKNOWN)) {
            if (unitGroup.getServiceDescriptionList().isEmpty()) {
                final UnitTemplate template = CachedTemplateRegistryRemote.getRegistry().getUnitTemplateByType(unitGroup.getUnitType());
                unitGroup.addAllServiceDescription(template.getServiceDescriptionList());
                throw new EntryModification(entry.setMessage(unitGroupUnitConfig, this), this);
            }
            if (!unitTemplateHasSameServices(unitGroup.getUnitType(), unitGroup.getServiceDescriptionList())) {
                unitGroup.setUnitType(UnitType.UNKNOWN);
                throw new EntryModification(entry.setMessage(unitGroupUnitConfig, this), this);
            }
        }
    }

    private boolean unitTemplateHasSameServices(UnitType unitType, List<ServiceDescription> serviceDescriptionList) throws CouldNotPerformException {
        final UnitTemplate unitTemplate = CachedTemplateRegistryRemote.getRegistry().getUnitTemplateByType(unitType);

        // validate if all registered services are included in the unit template
        for (ServiceDescription serviceDescription : serviceDescriptionList) {
            if (unitTemplate.getServiceDescriptionList().stream().noneMatch(serviceTemplate -> serviceTemplate.getServiceType() == serviceDescription.getServiceType() &&
                    serviceTemplate.getPattern() == serviceDescription.getPattern())) {
                return false;
            }
        }

        //validate if all services of the unit template are registered
        for (ServiceDescription serviceTemplate : unitTemplate.getServiceDescriptionList()) {
            if (serviceDescriptionList.stream().noneMatch(serviceDescription -> serviceTemplate.getServiceType() == serviceDescription.getServiceType() &&
                    serviceTemplate.getPattern() == serviceDescription.getPattern())) {
                return false;
            }
        }
        return true;
    }
}
