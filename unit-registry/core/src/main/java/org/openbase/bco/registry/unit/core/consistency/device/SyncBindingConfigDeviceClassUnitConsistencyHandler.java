package org.openbase.bco.registry.unit.core.consistency.device;

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
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.Registry;
import rst.homeautomation.binding.BindingConfigType.BindingConfig;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitRegistryDataType.UnitRegistryData;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SyncBindingConfigDeviceClassUnitConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final Registry<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>> deviceClassRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> dalUnitRegistry;

    public SyncBindingConfigDeviceClassUnitConsistencyHandler(final Registry<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>> deviceClassRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> dalUnitRegistry) {
        this.deviceClassRegistry = deviceClassRegistry;
        this.dalUnitRegistry = dalUnitRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig deviceUnitConfig = entry.getMessage();
        DeviceConfig deviceConfig = deviceUnitConfig.getDeviceConfig();

        for (String unitId : deviceConfig.getUnitIdList()) {
            boolean modification = false;
            UnitConfig.Builder unitConfig = dalUnitRegistry.getBuilder(unitId);
            UnitConfig.Builder unitConfigClone = unitConfig.clone();
            unitConfig.clearServiceConfig();
            for (ServiceConfig.Builder serviceConfig : unitConfigClone.getServiceConfigBuilderList()) {

                if (!serviceConfig.hasBindingConfig()) {
                    throw new NotAvailableException("serviceconfig.bindingserviceconfig");
                }

                BindingConfig bindingConfig = serviceConfig.getBindingConfig();

                if (!deviceConfig.hasDeviceClassId()) {
                    throw new NotAvailableException("deviceclass");
                }

                DeviceClass deviceClass = deviceClassRegistry.get(deviceConfig.getDeviceClassId()).getMessage();
                if (!deviceClass.hasBindingConfig()) {
                    throw new NotAvailableException("deviceclass.bindingconfig");
                }

                if (!deviceClass.getBindingConfig().hasBindingId() || deviceClass.getBindingConfig().getBindingId().equals("UNKNOWN")) {
                    throw new NotAvailableException("deviceclass.bindingconfig.bindingid");
                }

                String bindingId = deviceClass.getBindingConfig().getBindingId();

                if (!bindingConfig.hasBindingId() || !bindingConfig.getBindingId().equals(bindingId)) {
                    serviceConfig.setBindingConfig(bindingConfig.toBuilder().setBindingId(bindingId).build());
                    modification = true;
                }
                unitConfig.addServiceConfig(serviceConfig);
            }
            if (modification) {
                dalUnitRegistry.update(unitConfig.build());
            }
        }
    }
}
