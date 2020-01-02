package org.openbase.bco.registry.template.core.consistency.servicetemplate;

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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.util.HashMap;
import java.util.Map;

/**
 * Consistency handler which makes sure that per unit type only one template is registered.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceTemplateUniqueTypeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, ServiceTemplate, ServiceTemplate.Builder> {

    final Map<ServiceType, ServiceTemplate> serviceTypeServiceTemplateMap;

    public ServiceTemplateUniqueTypeConsistencyHandler() {
        this.serviceTypeServiceTemplateMap = new HashMap<>();
    }

    @Override
    public void processData(final String id,
                            final IdentifiableMessage<String, ServiceTemplate, ServiceTemplate.Builder> entry,
                            final ProtoBufMessageMap<String, ServiceTemplate, ServiceTemplate.Builder> entryMap, ProtoBufRegistry<String, ServiceTemplate, ServiceTemplate.Builder> registry)
            throws CouldNotPerformException, EntryModification {

        final ServiceTemplate serviceTemplate = entry.getMessage();
        if (serviceTypeServiceTemplateMap.containsKey(serviceTemplate.getServiceType()) && !serviceTemplate.getId().equals(serviceTypeServiceTemplateMap.get(serviceTemplate.getServiceType()).getId())) {
            throw new VerificationFailedException("ServiceTemplate[" + serviceTypeServiceTemplateMap.get(serviceTemplate.getServiceType()) + "] and serviceTemplate[" + serviceTemplate + "] both contain the same type");
        }

        serviceTypeServiceTemplateMap.put(serviceTemplate.getServiceType(), serviceTemplate);
    }

    @Override
    public void reset() {
        serviceTypeServiceTemplateMap.clear();
    }
}
