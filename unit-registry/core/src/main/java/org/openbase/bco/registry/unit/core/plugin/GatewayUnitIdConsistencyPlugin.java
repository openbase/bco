package org.openbase.bco.registry.unit.core.plugin;

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

import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass;
import org.openbase.type.domotic.unit.gateway.GatewayConfigType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class GatewayUnitIdConsistencyPlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, Builder, UnitRegistryData.Builder> gatewayUnitRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, Builder, UnitRegistryData.Builder> deviceUnitRegistry;

    public GatewayUnitIdConsistencyPlugin(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, Builder, UnitRegistryData.Builder> deviceUnitRegistry,
                                          final ProtoBufFileSynchronizedRegistry<String, UnitConfig, Builder, UnitRegistryData.Builder> gatewayUnitRegistry) {
        this.deviceUnitRegistry = deviceUnitRegistry;
        this.gatewayUnitRegistry = gatewayUnitRegistry;
    }

    @Override
    public void afterConsistencyCheck() throws CouldNotPerformException {
        for (IdentifiableMessage<String, UnitConfig, Builder> entry : getRegistry().getEntries()) {
            updateUnitConfigs(entry);
        }
    }

    @Override
    public void beforeRemove(IdentifiableMessage<String, UnitConfig, Builder> entry) throws RejectedException {
        try {
            for (String unitId : entry.getMessage().getGatewayConfig().getUnitIdList()) {
                deviceUnitRegistry.remove(unitId);
            }
        } catch (CouldNotPerformException ex) {
            throw new RejectedException("Could not remove all units for the removed gateway!", ex);
        }
    }

    public void updateUnitConfigs(IdentifiableMessage<String, UnitConfig, Builder> entry) throws CouldNotPerformException {
        Builder gatewayUnitConfig = entry.getMessage().toBuilder();
        GatewayConfigType.GatewayConfig.Builder gatewayConfig = gatewayUnitConfig.getGatewayConfigBuilder();

        if (!gatewayConfig.hasGatewayClassId() || gatewayConfig.getGatewayClassId().isEmpty()) {
            throw new NotAvailableException("gatewayclass.id");
        }

        boolean modification = false;
        GatewayClass gatewayClass = CachedClassRegistryRemote.getRegistry().getGatewayClassById(gatewayConfig.getGatewayClassId());
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (String unitId : gatewayConfig.getUnitIdList()) {
            if (deviceUnitRegistry.contains(unitId)) {
                unitConfigs.add(deviceUnitRegistry.getMessage(unitId));
            } else {
                logger.warn("Dal unit[" + unitId + "] cannot be found anymore and will be removed.");
                modification = true;
            }
        }

        // remove all units that do not exist
        gatewayConfig.clearUnitId();
        for (UnitConfig unitConfig : new ArrayList<>(unitConfigs)) {
            gatewayConfig.addUnitId(unitConfig.getId());
        }

        if (modification) {
            gatewayUnitRegistry.update(gatewayUnitConfig.build());
        }
    }
}
