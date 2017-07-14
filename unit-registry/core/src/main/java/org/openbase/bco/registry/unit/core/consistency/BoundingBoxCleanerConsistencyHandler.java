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
import rst.math.Vec3DDoubleType;
import rst.spatial.ShapeType.Shape;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BoundingBoxCleanerConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        //todo boundingbox included in all types?
        if (!unitConfig.hasPlacementConfig() || !unitConfig.getPlacementConfig().hasShape()) {
            return;
        }

        Shape shape = unitConfig.getPlacementConfig().getShape();

        if (!shape.hasBoundingBox()) {
            return;
        }
        //TODO clean out empty bounding boxes?   

        AxisAlignedBoundingBox3DFloat newBoundingBox = updateBoundingBox(shape);

        //detect changes
   //     if(!shape.getBoundingBox().equals(newBoundingBox)) {
        if (shape.getBoundingBox().getDepth() != newBoundingBox.getDepth()
            || shape.getBoundingBox().getHeight() != newBoundingBox.getHeight()
            || shape.getBoundingBox().getWidth() != newBoundingBox.getWidth()) {

            unitConfig.getPlacementConfigBuilder().getShapeBuilder().setBoundingBox(newBoundingBox);
            throw new EntryModification(entry.setMessage(unitConfig), this);
        }
    }

    private AxisAlignedBoundingBox3DFloat updateBoundingBox(final Shape shape) {

        double maxX = 0.0;
        double minX = Double.MAX_VALUE;
        double maxY = 0.0;
        double minY = Double.MAX_VALUE;
        double maxZ = 0.0;
        double minZ = Double.MAX_VALUE;

        // Get the shape of the room
        final List<Vec3DDoubleType.Vec3DDouble> roomShape = shape.getFloorList();

        // Iterate over all vertices
        for (final Vec3DDoubleType.Vec3DDouble rstVertex : roomShape) {
            
            minX = Math.min(rstVertex.getX(), minX);
            maxX = Math.max(rstVertex.getX(), maxX);
            
            minY = Math.min(rstVertex.getY(), minY);
            maxY = Math.max(rstVertex.getY(), maxY);
            
            minZ = Math.min(rstVertex.getZ(), minZ);
            maxZ = Math.max(rstVertex.getZ(), maxZ);
            
        }
        AxisAlignedBoundingBox3DFloat.Builder builder = AxisAlignedBoundingBox3DFloat.newBuilder();
        builder.setDepth((float) (maxY - minY));
        builder.setHeight((float) (maxZ - minZ));
        builder.setWidth((float) (maxX - minX));
        Translation.Builder translationBuilder = Translation.newBuilder().setX(0).setY(0).setZ(0);
        builder.setLeftFrontBottom(translationBuilder);

        AxisAlignedBoundingBox3DFloat newBB = builder.build();

        return newBB;
    }
}
