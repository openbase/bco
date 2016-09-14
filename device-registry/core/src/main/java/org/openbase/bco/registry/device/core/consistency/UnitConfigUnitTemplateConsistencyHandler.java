package org.openbase.bco.registry.device.core.consistency;

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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import java.util.ArrayList;
import java.util.List;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryDataType.DeviceRegistryData;
import rst.homeautomation.binding.BindingConfigType.BindingConfig;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UnitConfigUnitTemplateConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistryData.Builder> unitTemplateRegistry;

    List<String> idList = new ArrayList();

    public UnitConfigUnitTemplateConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistryData.Builder> unitTemplateRegistry) {
        this.unitTemplateRegistry = unitTemplateRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMap<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistry<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        boolean modification = false;
        for (UnitConfig.Builder unitConfig : deviceConfig.getUnitConfigBuilderList()) {
            UnitTemplate unitTemplate = unitTemplateRegistry.get(unitConfig.getType().toString()).getMessage();
            for (ServiceTemplate serviceTemplate : unitTemplate.getServiceTemplateList()) {
                if (!unitConfigContainsServiceType(unitConfig, serviceTemplate)) {
                    unitConfig.addServiceConfig(ServiceConfig.newBuilder().setServiceTemplate(serviceTemplate).setBindingConfig(BindingConfig.getDefaultInstance()));
                    modification = true;
                }
            }

            for (int i = 0; i < unitConfig.getServiceConfigCount(); i++) {
                if (!unitTemplate.getServiceTemplateList().contains(unitConfig.getServiceConfig(i).getServiceTemplate())) {
                    unitConfig.removeServiceConfig(i);
                    i--;
                    modification = true;
                }
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }

    private boolean unitConfigContainsServiceType(UnitConfig.Builder unitConfig, ServiceTemplate serviceTemplate) {
        for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
            if (serviceConfig.getServiceTemplate().equals(serviceTemplate)) {
                return true;
            }
        }
        return false;
    }
}
