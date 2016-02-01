/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core.consistency;

/*
 * #%L
 * REM DeviceRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType;
import rst.homeautomation.service.BindingServiceConfigType;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateConfigType.UnitTemplateConfig;

/**
 *
 * @author mpohling
 */
public class DeviceConfigDeviceClassUnitConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private static final Logger logger = LoggerFactory.getLogger(DeviceConfigDeviceClassUnitConsistencyHandler.class);

    private ProtoBufFileSynchronizedRegistry<String, DeviceClassType.DeviceClass, DeviceClassType.DeviceClass.Builder, DeviceRegistryType.DeviceRegistry.Builder> deviceClassRegistry;

    public DeviceConfigDeviceClassUnitConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, DeviceClassType.DeviceClass, DeviceClassType.DeviceClass.Builder, DeviceRegistryType.DeviceRegistry.Builder> deviceClassRegistry) {
        this.deviceClassRegistry = deviceClassRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        if (!deviceConfig.hasDeviceClassId()) {
            throw new NotAvailableException("deviceclass");
        }

        if (!deviceConfig.hasDeviceClassId() || deviceConfig.getDeviceClassId().isEmpty()) {
            throw new NotAvailableException("deviceclass.id");
        }

        boolean modification = false;
        DeviceClass deviceClass = deviceClassRegistry.get(deviceConfig.getDeviceClassId()).getMessage();
        List<UnitConfig> unitConfigs = new ArrayList<>(deviceConfig.getUnitConfigList());
        deviceConfig.clearUnitConfig();

        for (UnitTemplateConfig unitTemplate : deviceClass.getUnitTemplateConfigList()) {
            if (!unitToTemplateExists(unitConfigs, unitTemplate.getId())) {
                List<ServiceConfig> serviceConfigs = new ArrayList<>();
                for (ServiceTemplate serviceTemplate : unitTemplate.getServiceTemplateList()) {
                    serviceConfigs.add(ServiceConfig.newBuilder().setType(serviceTemplate.getServiceType()).setBindingServiceConfig(BindingServiceConfigType.BindingServiceConfig.newBuilder().setType(deviceClass.getBindingConfig().getType())).build());
                }
                unitConfigs.add(UnitConfig.newBuilder().setType(unitTemplate.getType()).addAllServiceConfig(serviceConfigs).setUnitTemplateConfigId(unitTemplate.getId()).build());
                modification = true;
            }
        }
        deviceConfig.addAllUnitConfig(unitConfigs);

        for (UnitConfig unitConfig : unitConfigs) {
            if (!TemplateToUnitExists(deviceClass.getUnitTemplateConfigList(), unitConfig.getUnitTemplateConfigId())) {
                logger.warn("Unit Config [" + unitConfig.getId() + "] in device [" + deviceConfig.getId() + "] has no according unit template config in device class [" + deviceClass.getId() + "]");
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }

    private boolean unitToTemplateExists(List<UnitConfig> units, String id) {
        for (UnitConfig unit : units) {
            if (unit.getUnitTemplateConfigId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private boolean TemplateToUnitExists(List<UnitTemplateConfig> units, String id) {
        for (UnitTemplateConfig unit : units) {
            if (unit.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private boolean sameServices(UnitConfig config, UnitTemplateConfig templateConfig) {
        if (config.getServiceConfigCount() != templateConfig.getServiceTemplateCount()) {
            return false;
        }

        for (int i = 0; i < config.getServiceConfigCount(); i++) {
            if (!(config.getServiceConfig(i).getType().equals(templateConfig.getServiceTemplate(i).getServiceType()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reset() {
    }
}
