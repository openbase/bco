package org.openbase.bco.registry.unit.core.consistency;

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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.binding.BindingConfigType.BindingConfig;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitConfigUnitTemplateConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        if (!unitConfig.hasUnitType()) {
            throw new NotAvailableException("unitConfig.type");
        }

        boolean modification = false;
        final UnitTemplate unitTemplate = CachedTemplateRegistryRemote.getRegistry().getUnitTemplateByType(unitConfig.getUnitType());
        for (final ServiceDescription serviceDescription : unitTemplate.getServiceDescriptionList()) {
            if (!unitConfigContainsServiceDescription(unitConfig, serviceDescription)) {
                unitConfig.addServiceConfig(ServiceConfig.newBuilder().setServiceDescription(serviceDescription).setBindingConfig(BindingConfig.getDefaultInstance()));
                modification = true;
            }
        }

        for (int i = 0; i < unitConfig.getServiceConfigCount(); i++) {
            if (!serviceDescriptionListContainsDescription(unitTemplate.getServiceDescriptionList(), unitConfig.getServiceConfig(i).getServiceDescription())) {
                unitConfig.removeServiceConfig(i);
                i--;
                modification = true;
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(unitConfig, this), this);
        }
    }

    private boolean serviceDescriptionListContainsDescription(final List<ServiceDescription> serviceDescriptionList, final ServiceDescription serviceDescription) {
        for (final ServiceDescription description : serviceDescriptionList) {
            boolean patternMatch = description.getPattern() == serviceDescription.getPattern();
            if (!patternMatch) {
                continue;
            }

            if (description.getServiceTemplateId().equals(serviceDescription.getServiceTemplateId())) {
                return true;
            }

            if (description.getServiceType() == serviceDescription.getServiceType()) {
                return true;
            }
        }
        return false;
    }

    private boolean unitConfigContainsServiceDescription(final UnitConfig.Builder unitConfig, final ServiceDescription serviceDescription) {
        final List<ServiceDescription> serviceDescriptionList = new ArrayList<>();
        for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
            serviceDescriptionList.add(serviceConfig.getServiceDescription());
        }
        return serviceDescriptionListContainsDescription(serviceDescriptionList, serviceDescription);
    }
}
