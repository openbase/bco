package org.openbase.bco.registry.unit.core.consistency;

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
import java.util.Arrays;
import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.geometry.AxisAlignedBoundingBox3DFloatType.AxisAlignedBoundingBox3DFloat;
import rst.geometry.TranslationType.Translation;
import rst.math.Vec3DDoubleType.Vec3DDouble;
import rst.spatial.ShapeType.Shape;


public class BoundingBoxCleanerConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();
        
        // filter if config does not contain placement or shape
        if (!unitConfig.hasPlacementConfig() || !unitConfig.getPlacementConfig().hasShape() || 
                unitConfig.getPlacementConfig().getShape().getFloorList().isEmpty() || unitConfig.getPlacementConfig().getShape().getCeilingList().isEmpty()) {
            return;
        }
        
        final Shape shape = unitConfig.getPlacementConfig().getShape();
        final AxisAlignedBoundingBox3DFloat newBoundingBox = updateBoundingBox(shape);

        //detect changes
        if(!shape.getBoundingBox().equals(newBoundingBox)) { 
            unitConfig.getPlacementConfigBuilder().getShapeBuilder().setBoundingBox(newBoundingBox);
            throw new EntryModification(entry.setMessage(unitConfig), this);
        }
    }

    private AxisAlignedBoundingBox3DFloat updateBoundingBox(final Shape shape) {

        double maxX = Double.MIN_VALUE;
        double minX = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxZ = Double.MIN_VALUE;
        double minZ = Double.MAX_VALUE;

        // Iterate over all vertices, find biggest values in all dimensions
        for (List<Vec3DDouble> vecList : Arrays.asList(shape.getFloorList(), shape.getCeilingList())) {
            for (final Vec3DDouble rstVertex : vecList) {

                minX = Math.min(rstVertex.getX(), minX);
                maxX = Math.max(rstVertex.getX(), maxX);

                minY = Math.min(rstVertex.getY(), minY);
                maxY = Math.max(rstVertex.getY(), maxY);

                minZ = Math.min(rstVertex.getZ(), minZ);
                maxZ = Math.max(rstVertex.getZ(), maxZ);
            }
        }
        
        final AxisAlignedBoundingBox3DFloat.Builder builder = AxisAlignedBoundingBox3DFloat.newBuilder();
        builder.setDepth((float) (maxY - minY));
        builder.setHeight((float) (maxZ - minZ));
        builder.setWidth((float) (maxX - minX));
        builder.setLeftFrontBottom(Translation.newBuilder().setX(minX).setY(minY).setZ(minZ));
        return builder.build();
    }
}
