package org.openbase.bco.registry.device.core.consistency;

/*
 * #%L
 * REM DeviceRegistry Core
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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.ArrayList;
import java.util.List;
import org.openbase.jul.exception.VerificationFailedException;
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

    private final ProtoBufFileSynchronizedRegistry<String, DeviceClassType.DeviceClass, DeviceClassType.DeviceClass.Builder, DeviceRegistryType.DeviceRegistry.Builder> deviceClassRegistry;

    public DeviceConfigDeviceClassUnitConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, DeviceClassType.DeviceClass, DeviceClassType.DeviceClass.Builder, DeviceRegistryType.DeviceRegistry.Builder> deviceClassRegistry) {
        this.deviceClassRegistry = deviceClassRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        if (!deviceConfig.hasDeviceClassId() || deviceConfig.getDeviceClassId().isEmpty()) {
            throw new NotAvailableException("deviceclass.id");
        }

        boolean modification = false;
        DeviceClass deviceClass = deviceClassRegistry.get(deviceConfig.getDeviceClassId()).getMessage();
        List<UnitConfig> unitConfigs = new ArrayList<>(deviceConfig.getUnitConfigList());
        deviceConfig.clearUnitConfig();

        for (UnitTemplateConfig unitTemplate : deviceClass.getUnitTemplateConfigList()) {
            if (!unitWithRelatedTemplateExists(unitConfigs, unitTemplate)) {
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
            if (!templateForUnitExists(deviceClass.getUnitTemplateConfigList(), unitConfig.getUnitTemplateConfigId())) {
                throw new VerificationFailedException("Unit Config [" + unitConfig.getId() + "] in device [" + deviceConfig.getId() + "] has no according unit template config in device class [" + deviceClass.getId() + "]");
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }

    private boolean unitWithRelatedTemplateExists(List<UnitConfig> units, UnitTemplateConfig unitTemplate) {
        for (UnitConfig unit : units) {
            if (unit.getUnitTemplateConfigId().equals(unitTemplate.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean templateForUnitExists(List<UnitTemplateConfig> units, String id) {
        for (UnitTemplateConfig unit : units) {
            if (unit.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
