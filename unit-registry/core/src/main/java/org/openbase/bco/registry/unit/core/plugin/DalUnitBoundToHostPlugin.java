package org.openbase.bco.registry.unit.core.plugin;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class DalUnitBoundToHostPlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry;

    public DalUnitBoundToHostPlugin(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
    }

    @Override
    public void beforeUpdate(IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) throws RejectedException {
        UnitConfig unitConfig = entry.getMessage();
        if (unitConfig.getBoundToUnitHost() && UnitConfigProcessor.isHostUnitAvailable(unitConfig)) {
            try {
                UnitConfig deviceUnitConfig = deviceRegistry.getMessage(unitConfig.getUnitHostId());
                PlacementConfig unitPlacement = unitConfig.getPlacementConfig();
                PlacementConfig.Builder devicePlacement = deviceUnitConfig.toBuilder().getPlacementConfigBuilder();

                boolean modification = false;
                if (!unitPlacement.getLocationId().equals(deviceUnitConfig.getPlacementConfig().getLocationId())) {
                    devicePlacement.setLocationId(unitPlacement.getLocationId());
                    modification = true;
                }

                if (!unitPlacement.hasPose() && deviceUnitConfig.getPlacementConfig().hasPose()) {
                    devicePlacement.clearPose();
                    modification = true;
                }

                if (unitPlacement.hasPose() && !unitPlacement.getPose().equals(devicePlacement.getPose())) {
                    devicePlacement.setPose(unitPlacement.getPose());
                    modification = true;
                }

                if (modification) {
                    deviceRegistry.update(deviceUnitConfig.toBuilder().setPlacementConfig(devicePlacement).build());
                }
            } catch (CouldNotPerformException ex) {
                throw new RejectedException("Could not update position in device from a change in a unit", ex);
            }
        }
    }
}
