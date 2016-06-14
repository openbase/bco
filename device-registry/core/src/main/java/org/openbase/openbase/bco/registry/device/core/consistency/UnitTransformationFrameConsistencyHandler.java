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

import org.openbase.jul.extension.rst.storage.registry.consistency.AbstractTransformationFrameConsistencyHandler;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType;
import rst.spatial.LocationConfigType;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author mpohling
 */
public class UnitTransformationFrameConsistencyHandler extends AbstractTransformationFrameConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    public UnitTransformationFrameConsistencyHandler(final ProtoBufRegistryInterface<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> locationRegistry) {
        super(locationRegistry);
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, final ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, final ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        DeviceConfig.Builder deviceConfigBuilder = entry.getMessage().toBuilder();
        deviceConfigBuilder.clearUnitConfig();
        boolean modification = false;
        PlacementConfig placementConfig;

        for (UnitConfigType.UnitConfig.Builder unitConfigBuilder : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {

            placementConfig = verifyAndUpdatePlacement(unitConfigBuilder.getLabel(), unitConfigBuilder.getPlacementConfig());

            if (placementConfig != null) {
                unitConfigBuilder.setPlacementConfig(placementConfig);
                logger.debug("UnitTransformationFrameConsistencyHandler Upgrade Unit["+unitConfigBuilder.getId()+"] frame to "+placementConfig.getTransformationFrameId());
                modification = true;
            }

            deviceConfigBuilder.addUnitConfig(unitConfigBuilder);
        }

        if (modification) {
            logger.debug("UnitTransformationFrameConsistencyHandler Publish Device["+deviceConfigBuilder.getId()+"]!");
            throw new EntryModification(entry.setMessage(deviceConfigBuilder), this);
        }
    }
}
