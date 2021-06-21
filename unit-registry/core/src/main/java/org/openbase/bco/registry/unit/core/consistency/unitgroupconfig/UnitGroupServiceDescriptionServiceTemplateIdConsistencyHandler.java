package org.openbase.bco.registry.unit.core.consistency.unitgroupconfig;

/*-
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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupServiceDescriptionServiceTemplateIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        boolean modification = false;
        for (ServiceDescription.Builder serviceDescription : unitConfig.getUnitGroupConfigBuilder().getServiceDescriptionBuilderList()) {
            if (serviceDescription.getServiceTemplateId().isEmpty()) {
                if (!serviceDescription.hasServiceType()) {
                    throw new NotAvailableException("ServiceType");
                }

                serviceDescription.setServiceTemplateId(getServiceIdByType(serviceDescription.getServiceType()));
                modification = true;
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(unitConfig, this), this);
        }
    }

    public String getServiceIdByType(ServiceTemplateType.ServiceTemplate.ServiceType serviceType) throws CouldNotPerformException {
        for (ServiceTemplateType.ServiceTemplate serviceTemplate : CachedTemplateRegistryRemote.getRegistry().getServiceTemplates()) {
            if (serviceTemplate.getServiceType() == serviceType) {
                return serviceTemplate.getId();
            }
        }
        throw new NotAvailableException("Now service template for type[" + serviceType.name() + "] found!", "service id");
    }
    
}
