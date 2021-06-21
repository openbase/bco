package org.openbase.bco.registry.unit.core.consistency.locationconfig;

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
import org.openbase.bco.registry.lib.util.LocationUtils;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationTypeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder locationUnit = entry.getMessage().toBuilder();
        LocationConfig.Builder locationConfig = locationUnit.getLocationConfigBuilder();

        if (!locationConfig.hasLocationType()) {
            try {
                locationConfig.setLocationType(LocationUtils.detectLocationType(entry.getMessage(), registry));
                throw new EntryModification(entry.setMessage(locationUnit, this), this);
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("The locationType of location[" + locationUnit.getLabel() + "] has to be defined manually", ex);
            }
        } else {
            try {
                LocationType detectedType = LocationUtils.detectLocationType(entry.getMessage(), registry);
                if (detectedType != locationConfig.getLocationType()) {
                    locationConfig.setLocationType(detectedType);
                    throw new EntryModification(entry.setMessage(locationUnit, this), this);
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not detect locationType for location[" + locationUnit.getLabel() + "] with current type [" + locationConfig.getLocationType().name() + "]", ex, logger, LogLevel.DEBUG);
            }
        }
    }
}
