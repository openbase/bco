package org.openbase.bco.registry.unit.core.plugin;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.binding.BindingConfigType;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceConfigType;
import rst.domotic.service.ServiceTemplateConfigType;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateConfigType;
import rst.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.domotic.unit.device.DeviceConfigType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceConfigDeviceClassUnitConsistencyPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Registry<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>> deviceClassRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceUnitRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> dalUnitRegistry;

    public DeviceConfigDeviceClassUnitConsistencyPlugin(final Registry<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>> deviceClassRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> dalUnitRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceUnitRegistry) {
        this.deviceClassRegistry = deviceClassRegistry;
        this.dalUnitRegistry = dalUnitRegistry;
        this.deviceUnitRegistry = deviceUnitRegistry;
    }

    @Override
    public void afterConsistencyCheck() throws CouldNotPerformException {
        for (IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry : getRegistry().getEntries()) {
            updateUnitConfigs(entry);
        }
    }

    @Override
    public void beforeRemove(IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) throws RejectedException {
        try {
            for (String unitId : entry.getMessage().getDeviceConfig().getUnitIdList()) {
                dalUnitRegistry.remove(unitId);
            }
        } catch (CouldNotPerformException ex) {
            throw new RejectedException("Could not remove all units for the removed device!", ex);
        }
    }

    public void updateUnitConfigs(IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) throws CouldNotPerformException {
        UnitConfig.Builder deviceUnitConfig = entry.getMessage().toBuilder();
        DeviceConfigType.DeviceConfig.Builder deviceConfig = deviceUnitConfig.getDeviceConfigBuilder();

        if (!deviceConfig.hasDeviceClassId() || deviceConfig.getDeviceClassId().isEmpty()) {
            throw new NotAvailableException("deviceclass.id");
        }

        boolean modification = false;
        DeviceClass deviceClass = deviceClassRegistry.get(deviceConfig.getDeviceClassId()).getMessage();
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (String unitId : deviceConfig.getUnitIdList()) {
            unitConfigs.add(dalUnitRegistry.getMessage(unitId));
        }

        // remove all units that do not have an according unitTemplateConfig in the deviceClass
        // has to be done after the id has been removed from the device config
        deviceConfig.clearUnitId();
        for (UnitConfig unitConfig : new ArrayList<>(unitConfigs)) {
            if (templateForUnitExists(deviceClass.getUnitTemplateConfigList(), unitConfig.getUnitTemplateConfigId())) {
                deviceConfig.addUnitId(unitConfig.getId());
            } else {
                dalUnitRegistry.remove(unitConfig);
                unitConfigs.remove(unitConfig);
                modification = true;
            }
        }

        // add all non existing units that have an according unitTemplateConfig in the deviceClass
        for (UnitTemplateConfigType.UnitTemplateConfig unitTemplateConfig : deviceClass.getUnitTemplateConfigList()) {
            if (!unitWithRelatedTemplateExists(unitConfigs, unitTemplateConfig)) {
                List<ServiceConfigType.ServiceConfig> serviceConfigs = new ArrayList<>();
                for (ServiceTemplateConfigType.ServiceTemplateConfig serviceTemplateConfig : unitTemplateConfig.getServiceTemplateConfigList()) {
                    ServiceConfigType.ServiceConfig.Builder serviceConfig = ServiceConfigType.ServiceConfig.newBuilder().setBindingConfig(BindingConfigType.BindingConfig.newBuilder().setBindingId(deviceClass.getBindingConfig().getBindingId()));
                    serviceConfig.setServiceTemplate(ServiceTemplateType.ServiceTemplate.newBuilder().setType(serviceTemplateConfig.getServiceType()));
                    serviceConfigs.add(serviceConfig.build());
                }

                UnitConfig dalUnitConfig = UnitConfig.newBuilder().setType(unitTemplateConfig.getType()).addAllServiceConfig(serviceConfigs).setUnitTemplateConfigId(unitTemplateConfig.getId()).setUnitHostId(deviceUnitConfig.getId()).build();
                dalUnitConfig = dalUnitRegistry.register(dalUnitConfig);
                deviceConfig.addUnitId(dalUnitConfig.getId());
                modification = true;
            }
        }

        if (modification) {
            deviceUnitRegistry.update(deviceUnitConfig.build());
        }
    }

    private boolean unitWithRelatedTemplateExists(final List<UnitConfig> units, final UnitTemplateConfig unitTemplate) {
        return units.stream().anyMatch((unit) -> (unit.getUnitTemplateConfigId().equals(unitTemplate.getId())));
    }

    private boolean templateForUnitExists(final List<UnitTemplateConfig> units, String id) {
        return units.stream().anyMatch((unit) -> (unit.getId().equals(id)));
    }
}
