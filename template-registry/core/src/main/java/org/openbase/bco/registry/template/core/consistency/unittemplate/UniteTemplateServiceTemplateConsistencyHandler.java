package org.openbase.bco.registry.template.core.consistency.unittemplate;

/*-
 * #%L
 * BCO Registry Template Core
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.registry.TemplateRegistryDataType.TemplateRegistryData;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UniteTemplateServiceTemplateConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitTemplate, UnitTemplate.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, ServiceTemplate, ServiceTemplate.Builder, TemplateRegistryData.Builder> serviceTemplateRegistry;

    public UniteTemplateServiceTemplateConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, ServiceTemplate, ServiceTemplate.Builder, TemplateRegistryData.Builder> serviceTemplateRegistry) {
        this.serviceTemplateRegistry = serviceTemplateRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder> entry, ProtoBufMessageMap<String, UnitTemplate, UnitTemplate.Builder> entryMap, ProtoBufRegistry<String, UnitTemplate, UnitTemplate.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitTemplate.Builder unitTemplate = entry.getMessage().toBuilder();

        ServiceTemplate serviceTemplate;
        boolean modification = false;
        List<ServiceDescription> serviceDescriptionList = new ArrayList<>();
        for (ServiceDescription serviceDescription : unitTemplate.getServiceDescriptionList()) {
            if (serviceDescription.getServiceTemplateId().isEmpty() || !serviceTemplateRegistry.contains(serviceDescription.getServiceTemplateId())) {
                // recover id from type:
                if (!serviceDescription.hasServiceType()) {
                    throw new NotAvailableException("ServiceDescription [" + serviceDescription + "] does not contain a valid id and type!");
                }
                serviceTemplate = getServiceTemplateByType(serviceDescription.getServiceType());
                serviceDescriptionList.add(serviceDescription.toBuilder().setServiceTemplateId(serviceTemplate.getId()).build());
                modification = true;
            } else {
                serviceTemplate = serviceTemplateRegistry.getMessage(serviceDescription.getServiceTemplateId());
                
                // sync type to service description
                if(serviceDescription.hasServiceType() && serviceTemplate.getServiceType() == serviceDescription.getServiceType()) {
                    // id and type match
                    serviceDescriptionList.add(serviceDescription);
                } else {
                    // sync type to description
                    serviceDescriptionList.add(serviceDescription.toBuilder().setServiceType(serviceTemplate.getServiceType()).build());
                    modification = true;
                }
            }
        }

        if (modification) {
            unitTemplate.clearServiceDescription();
            unitTemplate.addAllServiceDescription(serviceDescriptionList);
            throw new EntryModification(entry.setMessage(unitTemplate, this), this);
        }
    }

    private ServiceTemplate getServiceTemplateByType(ServiceType type) throws CouldNotPerformException {
        for (ServiceTemplate serviceTemplate : serviceTemplateRegistry.getMessages()) {
            if (serviceTemplate.getServiceType() == type) {
                return serviceTemplate;
            }
        }
        throw new NotAvailableException("ServiceTemplate with type[" + type + "]");
    }
}
