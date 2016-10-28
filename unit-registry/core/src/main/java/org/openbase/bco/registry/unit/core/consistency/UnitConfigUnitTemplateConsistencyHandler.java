package org.openbase.bco.registry.unit.core.consistency;

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
import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.binding.BindingConfigType.BindingConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitConfigUnitTemplateConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, UnitRegistryData.Builder> unitTemplateRegistry;

//    List<String> idList = new ArrayList();
    public UnitConfigUnitTemplateConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, UnitRegistryData.Builder> unitTemplateRegistry) {
        this.unitTemplateRegistry = unitTemplateRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        if (!unitConfig.hasType()) {
            throw new NotAvailableException("unitConfig.type");
        }

        boolean modification = false;
        UnitTemplate unitTemplate = getUnitTemplateByType(unitConfig.getType());
        for (ServiceTemplate serviceTemplate : unitTemplate.getServiceTemplateList()) {
            if (!unitConfigContainsServiceType(unitConfig, serviceTemplate)) {
                unitConfig.addServiceConfig(ServiceConfig.newBuilder().setServiceTemplate(serviceTemplate).setBindingConfig(BindingConfig.getDefaultInstance()));
                modification = true;
            }
        }

        for (int i = 0; i < unitConfig.getServiceConfigCount(); i++) {
            if (!serviceTemplateListContainsTemplate(unitTemplate.getServiceTemplateList(), unitConfig.getServiceConfig(i).getServiceTemplate())) {
                unitConfig.removeServiceConfig(i);
                i--;
                modification = true;
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(unitConfig), this);
        }
    }

    private boolean serviceTemplateListContainsTemplate(List<ServiceTemplate> serviceTemplateList, ServiceTemplate serviceTemplate) {
        return serviceTemplateList.stream().anyMatch((template) -> (template.getType() == serviceTemplate.getType() && template.getPattern() == serviceTemplate.getPattern()));
    }

    private boolean unitConfigContainsServiceType(UnitConfig.Builder unitConfig, ServiceTemplate serviceTemplate) {
        return unitConfig.getServiceConfigList().stream().map((serviceConfig) -> serviceConfig.getServiceTemplate()).anyMatch((template) -> (template.getType() == serviceTemplate.getType() && template.getPattern() == serviceTemplate.getPattern()));
    }

    private UnitTemplate getUnitTemplateByType(UnitType type) throws CouldNotPerformException {
        for (UnitTemplate unitTemplate : unitTemplateRegistry.getMessages()) {
            if (unitTemplate.getType() == type) {
                return unitTemplate;
            }
        }
        throw new NotAvailableException("UnitTemplate with type[" + type + "]");
    }
}
