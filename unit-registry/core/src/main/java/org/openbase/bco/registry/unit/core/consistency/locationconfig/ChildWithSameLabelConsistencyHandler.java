package org.openbase.bco.registry.unit.core.consistency.locationconfig;

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
import java.util.HashMap;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ChildWithSameLabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final Map<String, String> labelConsistencyMap;

    public ChildWithSameLabelConsistencyHandler() {
        labelConsistencyMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig locationUnitConfig = entry.getMessage();

        for (String childLocationId : new ArrayList<>(locationUnitConfig.getLocationConfig().getChildIdList())) {
            UnitConfig childLocationUnitConfig = registry.getMessage(childLocationId);

            if (labelConsistencyMap.containsKey(childLocationUnitConfig.getLabel()) && !labelConsistencyMap.get(childLocationUnitConfig.getLabel()).equals(childLocationId)) {
                throw new InvalidStateException("Location [" + locationUnitConfig.getId() + "," + locationUnitConfig.getLabel() + "] has more than on child with the same label [" + childLocationUnitConfig.getLabel() + "]");
            } else {
                labelConsistencyMap.put(childLocationUnitConfig.getLabel(), childLocationId);
            }
        }
    }

    @Override
    public void reset() {
        labelConsistencyMap.clear();
    }
}
