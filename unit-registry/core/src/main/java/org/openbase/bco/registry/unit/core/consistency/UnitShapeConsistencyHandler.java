package org.openbase.bco.registry.unit.core.consistency;

/*-
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;
import org.openbase.bco.registry.lib.util.LocationUtils;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.rct.GlobalTransformReceiver;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rct.Transform;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.math.Vec3DDoubleType.Vec3DDouble;
import rst.spatial.FloorCeilingEdgeIndicesType.FloorCeilingEdgeIndices;
import rst.spatial.ShapeType.Shape;

/**
 * This consistency handler adds missing ceiling coordinates and links between
 * floor and ceiling positions to the shape data.
 *
 * @author <a href="mailto:thuppke@techfak.uni-bielefeld.de">Thoren Huppke</a>
 */
public class UnitShapeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    /**
     * Default height which is used to create non-existant ceiling coordinates.
     */
    private static final double DEFAULT_HEIGHT = 2.79;
    
    private final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> locationRegistry;

    public UnitShapeConsistencyHandler(final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> locationRegistry) {
        this.locationRegistry = locationRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @param id {@inheritDoc}
     * @param entry {@inheritDoc}
     * @param entryMap {@inheritDoc}
     * @param registry {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws EntryModification {@inheritDoc}
     */
    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        final UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        // filter if config does not contain placement or shape
        if (!unitConfig.hasPlacementConfig() || !unitConfig.getPlacementConfig().hasShape() || unitConfig.getPlacementConfig().getShape().getFloorList().isEmpty()
                || !unitConfig.getPlacementConfig().getShape().getCeilingList().isEmpty()) {
            return;
        }
        
        logger.info("in processData of UnitShapeConsistencyHandler for unit " + unitConfig.getLabel());
        
        String rootLocationId;

        try {
            // resolvement via more frequently updated entryMap via consistency checks if this entryMap contains the locations.
            rootLocationId = LocationUtils.getRootLocation(entryMap).getId();
            logger.info("type 1");
        } catch (CouldNotPerformException ex) {
            try {
//                logger.info("type 2");
                // resolvement via the location registry.
                rootLocationId = LocationUtils.getRootLocation(locationRegistry.getMessages()).getId();
            } catch (CouldNotPerformException exx) {
                logger.info("type 3");
                // if the root location could not be detected this consistency check is not needed.
                return;
            }
        }
        
        logger.info("after root id");
        Transform3D unitTransformation;
        try {
            Future<Transform> requestTransform = GlobalTransformReceiver.getInstance().requestTransform(
                    unitConfig.getPlacementConfig().getTransformationFrameId(),
                    registry.get(rootLocationId).getMessage().getPlacementConfig().getTransformationFrameId(),
                    System.currentTimeMillis());
            unitTransformation = requestTransform.get().getTransform();
            logger.info("getUnitTransformation success");
        } catch(CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("could not get requestTransform", ex), logger);
            return;
        } catch (InterruptedException ex) {
            logger.error("Getting unitTransformation fails.");
            Thread.currentThread().interrupt();
            throw new FatalImplementationErrorException(this, ex);
        } catch (ExecutionException ex) {
            logger.warn("Could not get unitTransformation for unit " + unitConfig.getLabel());
            return;
        }
        logger.info("after unitTransformation");

        final Shape shape = unitConfig.getPlacementConfig().getShape();
        logger.info("Shape before, ceiling points: " + shape.getCeilingCount() + " connections: " + shape.getFloorCeilingEdgeCount());
        Shape newShape = updateCeilingAndLinks(shape, unitTransformation);
        logger.info("new shape, ceiling points: " + newShape.getCeilingCount() + " connections: " + newShape.getFloorCeilingEdgeCount());
        if (!shape.equals(newShape)) {
            logger.info("New shape difference should throw entry modification!");
            unitConfig.getPlacementConfigBuilder().setShape(newShape);
            logger.info("new shape, ceiling points: " + unitConfig.getPlacementConfig().getShape().getCeilingCount() + " connections: " + unitConfig.getPlacementConfig().getShape().getFloorCeilingEdgeCount());
            throw new EntryModification(entry.setMessage(unitConfig), this);
        }
    }

    /**
     * Uses a the default height to create the ceiling positions and links
     * between floor and ceiling positions.
     *
     * @param shape The original shape.
     * @param unitTransform Transform from root to unit coordinates.
     * @return The shape with updated information.
     */
    private Shape updateCeilingAndLinks(final Shape shape, final Transform3D unitTransform) {
        Transform3D inverseTransform = new Transform3D(unitTransform);
        inverseTransform.invert();

        Shape.Builder newBuilder = shape.toBuilder();
        newBuilder.clearCeiling();
        newBuilder.clearFloorCeilingEdge();
        
        for (int i = 0; i < shape.getFloorCount(); i++) {
            newBuilder.addCeiling(addHeight(shape.getFloor(i), unitTransform, inverseTransform));
            newBuilder.addFloorCeilingEdge(FloorCeilingEdgeIndices.newBuilder().setFloorIndex(i).setCeilingIndex(i));
        }
        return newBuilder.build();
    }

    /**
     * Sets the height of the vector to the default height in root coordinates,
     * then transforms it back to unit coordinates.
     *
     * @param vector original vector in unit coordinates.
     * @param transform transform from root to unit coordinates.
     * @param inverseTransform transform from unit to root coordinates.
     * @return Transformed original vector in unit coordinates.
     */
    private Vec3DDouble addHeight(final Vec3DDouble vector, final Transform3D transform, final Transform3D inverseTransform) {
        Vector3d vector3d = new Vector3d(vector.getX(), vector.getY(), vector.getZ());
        inverseTransform.transform(vector3d);
        vector3d.z += DEFAULT_HEIGHT;
        transform.transform(vector3d);
        return Vec3DDouble.newBuilder().setX(vector3d.x).setY(vector3d.y).setZ(vector3d.z).build();
    }
}
