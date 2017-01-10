package org.openbase.bco.registry.unit.core.consistency.dalunitconfig;

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
import org.openbase.jul.extension.rst.storage.registry.consistency.AbstractTransformationFrameConsistencyHandler;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.EntryModification;
import rst.spatial.PlacementConfigType.PlacementConfig;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitTransformationFrameConsistencyHandler extends AbstractTransformationFrameConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    public UnitTransformationFrameConsistencyHandler(final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> locationRegistry) {
        super(locationRegistry);
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        UnitConfig.Builder dalUnitConfig = entry.getMessage().toBuilder();
        boolean modification = false;
        PlacementConfig placementConfig;

        placementConfig = verifyAndUpdatePlacement(dalUnitConfig.getLabel(), dalUnitConfig.getPlacementConfig());

        if (placementConfig != null) {
            dalUnitConfig.setPlacementConfig(placementConfig);
            logger.debug("UnitTransformationFrameConsistencyHandler Upgrade Unit[" + dalUnitConfig.getId() + "] frame to " + placementConfig.getTransformationFrameId());
            modification = true;
        }

        if (modification) {
            logger.debug("UnitTransformationFrameConsistencyHandler Publish Device[" + dalUnitConfig.getId() + "]!");
            throw new EntryModification(entry.setMessage(dalUnitConfig), this);
        }
    }
}
