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
import java.util.ArrayList;
import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.homeautomation.binding.BindingConfigType.BindingConfig;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryDataType.DeviceRegistryData;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateConfigType.UnitTemplateConfig;

/**
 *
 * @author mpohling
 */
public class DeviceConfigDeviceClassUnitConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistryData.Builder> deviceClassRegistry;

    public DeviceConfigDeviceClassUnitConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistryData.Builder> deviceClassRegistry) {
        this.deviceClassRegistry = deviceClassRegistry;
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();
        DeviceConfig.Builder deviceConfig = unitConfig.getDeviceConfigBuilder();

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
