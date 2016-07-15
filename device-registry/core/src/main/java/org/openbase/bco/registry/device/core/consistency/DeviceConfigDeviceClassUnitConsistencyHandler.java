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
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryDataType;
import rst.homeautomation.binding.BindingConfigType.BindingConfig;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateConfigType.UnitTemplateConfig;

/**
 *
 * @author mpohling
 */
public class DeviceConfigDeviceClassUnitConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, DeviceClassType.DeviceClass, DeviceClassType.DeviceClass.Builder, DeviceRegistryDataType.DeviceRegistryData.Builder> deviceClassRegistry;

    public DeviceConfigDeviceClassUnitConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, DeviceClassType.DeviceClass, DeviceClassType.DeviceClass.Builder, DeviceRegistryDataType.DeviceRegistryData.Builder> deviceClassRegistry) {
        this.deviceClassRegistry = deviceClassRegistry;
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, final ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, final ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        if (!deviceConfig.hasDeviceClassId() || deviceConfig.getDeviceClassId().isEmpty()) {
            throw new NotAvailableException("deviceclass.id");
        }

        boolean modification = false;
        DeviceClass deviceClass = deviceClassRegistry.get(deviceConfig.getDeviceClassId()).getMessage();
        List<UnitConfig> unitConfigs = new ArrayList<>(deviceConfig.getUnitConfigList());
        deviceConfig.clearUnitConfig();

        for (UnitTemplateConfig unitTemplateConfig : deviceClass.getUnitTemplateConfigList()) {
            if (!unitWithRelatedTemplateExists(unitConfigs, unitTemplateConfig)) {
                List<ServiceConfig> serviceConfigs = new ArrayList<>();
                for (ServiceTemplateConfig serviceTemplateConfig : unitTemplateConfig.getServiceTemplateConfigList()) {
                    ServiceConfig.Builder serviceConfig = ServiceConfig.newBuilder().setBindingConfig(BindingConfig.newBuilder().setBindingId(deviceClass.getBindingConfig().getBindingId()));
                    serviceConfig.setServiceTemplate(ServiceTemplate.newBuilder().setType(serviceTemplateConfig.getServiceType()));
                    serviceConfigs.add(serviceConfig.build());
                }
                unitConfigs.add(UnitConfig.newBuilder().setType(unitTemplateConfig.getType()).addAllServiceConfig(serviceConfigs).setUnitTemplateConfigId(unitTemplateConfig.getId()).build());
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

    private boolean unitWithRelatedTemplateExists(final List<UnitConfig> units, final UnitTemplateConfig unitTemplate) {
        for (UnitConfig unit : units) {
            if (unit.getUnitTemplateConfigId().equals(unitTemplate.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean templateForUnitExists(final List<UnitTemplateConfig> units, String id) {
        for (UnitTemplateConfig unit : units) {
            if (unit.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
