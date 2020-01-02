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

import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.registry.UnitRegistryDataType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitBoundToHostConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryDataType.UnitRegistryData.Builder> deviceRegistry;

    public UnitBoundToHostConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryDataType.UnitRegistryData.Builder> deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder dalUnitConfig = entry.getMessage().toBuilder();

        // filter virtual units
        if (UnitConfigProcessor.isVirtualUnit(dalUnitConfig)) {
            return;
        }

        if (!UnitConfigProcessor.isHostUnitAvailable(dalUnitConfig)) {
            throw new NotAvailableException("dalUnitConfig.unitHostId");
        }

        final UnitConfig deviceUnitConfig = deviceRegistry.getMessage(dalUnitConfig.getUnitHostId());
        boolean modification = false;

        // Setup default bounding
        if (!dalUnitConfig.hasBoundToUnitHost()) {
            dalUnitConfig.setBoundToUnitHost(deviceUnitConfig.getBoundToUnitHost());
            modification = true;
        }

        // Overwrite unit bounds by device bounds if device bounds is true and unit bounds 
        if (deviceUnitConfig.getBoundToUnitHost() && !dalUnitConfig.getBoundToUnitHost()) {
            dalUnitConfig.setBoundToUnitHost(deviceUnitConfig.getBoundToUnitHost());
            modification = true;
        }

        // Copy device placement and location if bound to host
        if (dalUnitConfig.getBoundToUnitHost()) {

            // copy location id
            if (!dalUnitConfig.getPlacementConfig().getLocationId().equals(deviceUnitConfig.getPlacementConfig().getLocationId())) {
                dalUnitConfig.getPlacementConfigBuilder().setLocationId(deviceUnitConfig.getPlacementConfig().getLocationId());
                logger.debug("Updated location to : " + deviceUnitConfig.getPlacementConfig().getLocationId());
                modification = true;
            }

            // copy position
            if (deviceUnitConfig.getPlacementConfig().hasPose() && !dalUnitConfig.getPlacementConfig().getPose().equals(deviceUnitConfig.getPlacementConfig().getPose())) {
                dalUnitConfig.getPlacementConfigBuilder().setPose(deviceUnitConfig.getPlacementConfig().getPose());
                modification = true;
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(dalUnitConfig, this), this);
        }
    }
}
