package org.openbase.bco.registry.unit.core.consistency.dalunitconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.binding.BindingConfigType.BindingConfig;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.domotic.unit.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SyncBindingConfigDeviceClassUnitConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceUnitRegistry;

    public SyncBindingConfigDeviceClassUnitConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceUnitRegistry) {
        this.deviceUnitRegistry = deviceUnitRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder dalUnitConfig = entry.getMessage().toBuilder();

        // filter virtual units
        if(UnitConfigProcessor.isVirtualUnit(dalUnitConfig)) {
            return;
        }

        if (!UnitConfigProcessor.isHostUnitAvailable(dalUnitConfig)) {
            throw new NotAvailableException("dalUnitConfig.unitHostId");
        }

        DeviceConfig deviceConfig = deviceUnitRegistry.getMessage(dalUnitConfig.getUnitHostId()).getDeviceConfig();

        if (!deviceConfig.hasDeviceClassId() || deviceConfig.getDeviceClassId().isEmpty()) {
            throw new NotAvailableException("deviceConfig.deviceClassId");
        }

        final DeviceClass deviceClass = CachedClassRegistryRemote.getRegistry().getDeviceClassById(deviceConfig.getDeviceClassId());

        if (!deviceClass.hasBindingConfig() || !deviceClass.getBindingConfig().hasBindingId() || deviceClass.getBindingConfig().getBindingId().isEmpty()) {
            // nothing to sync
            return;
        }

        boolean modification = false;
        List<ServiceConfig.Builder> serviceConfigList = new ArrayList<>(dalUnitConfig.getServiceConfigBuilderList());
        dalUnitConfig.clearServiceConfig();
        for (final ServiceConfig.Builder serviceConfig : serviceConfigList) {
            BindingConfig bindingConfig;
            if (!serviceConfig.hasBindingConfig()) {
                bindingConfig = BindingConfig.getDefaultInstance();
            } else {
                bindingConfig = serviceConfig.getBindingConfig();
            }

            final String bindingId = deviceClass.getBindingConfig().getBindingId();

            if (!bindingConfig.hasBindingId() || !bindingConfig.getBindingId().equals(bindingId)) {
                serviceConfig.setBindingConfig(bindingConfig.toBuilder().setBindingId(bindingId).build()).build();
                modification = true;
            }
            dalUnitConfig.addServiceConfig(serviceConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(dalUnitConfig.build(), this), this);
        }
    }
}
