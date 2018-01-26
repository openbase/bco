package org.openbase.bco.registry.unit.core.plugin;

/*-
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.Builder;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceTemplateCreatorRegistryPlugin extends ProtobufRegistryPluginAdapter<String, ServiceTemplate, Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, ServiceTemplate, ServiceTemplate.Builder, UnitRegistryData.Builder> registry;

    public ServiceTemplateCreatorRegistryPlugin(ProtoBufFileSynchronizedRegistry<String, ServiceTemplate, ServiceTemplate.Builder, UnitRegistryData.Builder> unitTemplateRegistry) {
        this.registry = unitTemplateRegistry;
    }

    @Override
    public void init(ProtoBufRegistry<String, ServiceTemplate, ServiceTemplate.Builder> config) throws InitializationException, InterruptedException {
        try {
            ServiceTemplate template;

            // create missing unit template
            if (registry.size() <= ServiceType.values().length - 1) {
                for (ServiceType serviceType : ServiceType.values()) {
                    if (serviceType == ServiceType.UNKNOWN) {
                        continue;
                    }
                    template = ServiceTemplate.newBuilder().setType(serviceType).build();
                    if (!containsServiceTemplateByType(serviceType)) {
                        registry.register(template);
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not init " + getClass().getSimpleName() + "!", ex);
        }
    }

    private boolean containsServiceTemplateByType(ServiceType type) throws CouldNotPerformException {
        for (ServiceTemplate serviceTemplate : registry.getMessages()) {
            if (serviceTemplate.getType() == type) {
                return true;
            }
        }
        return false;
    }
    
}
