package org.openbase.bco.registry.unit.core.plugin;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationConfigType.LocationConfig;
import rst.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import rst.domotic.unit.location.TileConfigType.TileConfig;
import rst.math.Vec3DDoubleType.Vec3DDouble;
import rst.spatial.PlacementConfigType.PlacementConfig;
import rst.spatial.ShapeType.Shape;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A plugin that handles changes that have to be done before removing a location.
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class RootLocationPlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootLocationPlugin.class);

    public static final String DEFAULT_ROOT_LOCATION_NAME = "Home";


    @Override
    public void init(ProtoBufRegistry<String, UnitConfig, Builder> registry) throws InitializationException, InterruptedException {
        super.init(registry);
        try {
            setupRootLocationIfNeeded();
        } catch (RejectedException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void beforeConsistencyCheck() throws RejectedException {
        setupRootLocationIfNeeded();
    }

    public void setupRootLocationIfNeeded() throws RejectedException  {
        try {
            // check if any location exist
            if (getRegistry().isEmpty()) {
                getRegistry().register(generateDefaultRootLocation());
            }
        } catch (final CouldNotPerformException ex) {
            throw new RejectedException("Could not setup default root location!", ex);
        }
    }

    private UnitConfig generateDefaultRootLocation() {
        return UnitConfig.newBuilder()
                .setType(UnitType.LOCATION)
                .setLabel(DEFAULT_ROOT_LOCATION_NAME)
                .setLocationConfig(LocationConfig.newBuilder()
                        .setType(LocationType.ZONE)
                        .build())
                .setPlacementConfig(PlacementConfig.newBuilder()
                        .setShape(Shape.newBuilder()
                                .addFloor(Vec3DDouble.newBuilder().setX(0).setY(0).setZ(0).build())
                                .addFloor(Vec3DDouble.newBuilder().setX(1).setY(0).setZ(0).build())
                                .addFloor(Vec3DDouble.newBuilder().setX(1).setY(1).setZ(0).build())
                                .addFloor(Vec3DDouble.newBuilder().setX(0).setY(1).setZ(0).build())
                                .build())
                        .build())
                .build();
    }
}
