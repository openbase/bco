package org.openbase.bco.registry.unit.core.consistency;

/*-
 * #%L
 * BCO Registry Unit Core
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

import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 * Consistency handler validating that for all service configs in a unit config the service type matches
 * the service template id.
 * If one of these values is not set the other will be recovered.
 * If both are set the service type is set matching the service template id.
 * If both are not available this check fails.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitServiceConfigConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    /**
     * {@inheritDoc}
     *
     * @param id       {@inheritDoc}
     * @param entry    {@inheritDoc}
     * @param entryMap {@inheritDoc}
     * @param registry {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws EntryModification        {@inheritDoc}
     */
    @Override
    public void processData(final String id,
                            final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry,
                            final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap,
                            final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();


        // save if a modification occurs
        boolean modification = false;
        // iterate over all service configs
        for (final ServiceConfig.Builder serviceConfig : unitConfig.getServiceConfigBuilderList()) {
            // extract the service description
            final ServiceDescription.Builder serviceDescription = serviceConfig.getServiceDescriptionBuilder();

            if (serviceDescription.hasServiceTemplateId() && !serviceDescription.getServiceTemplateId().isEmpty()) {
                // service template id is available so match service type
                final ServiceTemplate serviceTemplate = CachedTemplateRegistryRemote.getRegistry().getServiceTemplateById(serviceDescription.getServiceTemplateId());

                if (serviceDescription.getServiceType() != serviceTemplate.getServiceType()) {
                    modification = true;
                    serviceDescription.setServiceType(serviceTemplate.getServiceType());
                }

                continue;
            }

            if (!serviceDescription.hasServiceType() || serviceDescription.getServiceType() == ServiceType.UNKNOWN) {
                // service template id and service type not available so fail
                throw new NotAvailableException("ServiceType and ServiceTemplateId");
            }

            // service template if not available but service type so recover the id
            serviceDescription.setServiceTemplateId(CachedTemplateRegistryRemote.getRegistry().getServiceTemplateByType(serviceDescription.getServiceType()).getId());
            modification = true;
        }

        // if modification occurred push it
        if (modification) {
            throw new EntryModification(entry.setMessage(unitConfig.build(), this), this);
        }
    }
}
