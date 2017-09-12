package org.openbase.bco.registry.unit.core.consistency.unitgroupconfig;

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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import org.openbase.bco.registry.lib.util.LocationUtils;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.rct.GlobalTransformReceiver;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rct.TransformerException;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfigOrBuilder;
import rst.geometry.AxisAlignedBoundingBox3DFloatType.AxisAlignedBoundingBox3DFloat;
import rst.geometry.PoseType.Pose;
import rst.geometry.RotationType.Rotation;
import rst.geometry.TranslationType.Translation;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 * This consinstency handler creates a bounding box around the bounding boxes of
 * its members and also fixed position and rotation.
 *
 * @author <a href="mailto:thuppke@techfak.uni-bielefeld.de">Thoren Huppke</a>
 */
public class UnitGroupPlacementConfigConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    /**
     * True and false as an array for loops.
     */
    private final static boolean[] BOOLEAN_VALUES = new boolean[]{false, true};

    /**
     * The list of all the unitConfigRegistries.
     */
    private final ArrayList<ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder>> unitConfigRegistryList;
    /**
     * The locationUnitConfigRegistry.
     */
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationUnitConfigRegistry;

    /**
     * Constructor.
     *
     * @param unitConfigRegistryList the list of all the unitConfigRegistries.
     * @param locationUnitConfigRegistry the locationUnitConfigRegistry.
     */
    public UnitGroupPlacementConfigConsistencyHandler(final ArrayList<ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder>> unitConfigRegistryList,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationUnitConfigRegistry) {
        this.unitConfigRegistryList = unitConfigRegistryList;
        this.locationUnitConfigRegistry = locationUnitConfigRegistry;
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
    public void processData(final String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap,
            final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        final String rootFrameId;
        final Transform3D parentTransformation;
        try {
            rootFrameId = getRootFrameId(locationUnitConfigRegistry);
            final UnitConfig parentConfig = locationUnitConfigRegistry.get(unitConfig.getPlacementConfig().getLocationId()).getMessage();
            parentTransformation = getRootToUnitTransform3D(rootFrameId, parentConfig);
        } catch (NotAvailableException ex) {
            throw new CouldNotPerformException("Could not get root to unit transformation.", ex);
        }

        Point3d minPosition = new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        Point3d maxPosition = new Point3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        for (String memberId : unitConfig.getUnitGroupConfig().getMemberIdList()) {
            UnitConfig memberConf = null;
            for (ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> reg : unitConfigRegistryList) {
                try {
                    memberConf = reg.get(memberId).getMessage();
                    break;
                } catch (CouldNotPerformException ex) {
                }
            }
            if (memberConf != null && memberConf.hasPlacementConfig() && memberConf.getPlacementConfig().hasPosition()) {
                try {
                    List<Point3d> pointsToCheck = getPointsToCheck(rootFrameId, memberConf);
                    minPosition = pointsToCheck.stream().reduce(minPosition, (result, element) -> getMin(result, element));
                    maxPosition = pointsToCheck.stream().reduce(maxPosition, (result, element) -> getMax(result, element));
                } catch (NotAvailableException ex) {
                    throw new CouldNotPerformException("Could not get Transformation for member with position.", ex);
                }
            }
        }
        if (minPosition.x == Double.POSITIVE_INFINITY) {
            return;
        }
        parentTransformation.transform(minPosition);
        parentTransformation.transform(maxPosition);

        // Create minimum and dimensions in the parents coordinate system
        Point3d newMin = getMin(minPosition, maxPosition);
        Point3d dimensions = getMax(minPosition, maxPosition);
        dimensions.sub(newMin);

        // update PlacementConfig
        final PlacementConfig placementConfig = unitConfig.getPlacementConfig();
        PlacementConfig newPlacementConfig = updatePlacementConfig(placementConfig, newMin, dimensions);
        if (!placementConfig.equals(newPlacementConfig)) {
            unitConfig.setPlacementConfig(newPlacementConfig);
            throw new EntryModification(entry.setMessage(unitConfig), this);
        }
    }

    /**
     * Updates the placement config based on the minimal coordinates and
     * dimensions of the bounding box.
     *
     * @param placementConfig the PlacementConfig to be updated.
     * @param minPosition the minimal coordinates of the new bounding box.
     * @param dimensions the dimensions of the bounding box (x: width, y: depth,
     * z: height).
     * @return the updated PlacementConfig including new Position, zero Rotation
     * and the updated bounding box.
     */
    private static PlacementConfig updatePlacementConfig(PlacementConfig placementConfig, Point3d minPosition, Point3d dimensions) {
        PlacementConfig.Builder newBuilder = placementConfig.toBuilder();
        newBuilder.setShape(newBuilder.getShapeBuilder()
                .setBoundingBox(AxisAlignedBoundingBox3DFloat.newBuilder()
                        .setLeftFrontBottom(Translation.newBuilder().setX(0).setY(0).setZ(0))
                        .setWidth((float) dimensions.x)
                        .setDepth((float) dimensions.y)
                        .setHeight((float) dimensions.z)))
                .setPosition(Pose.newBuilder()
                        .setRotation(Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0))
                        .setTranslation(Translation.newBuilder()
                                .setX(minPosition.x)
                                .setY(minPosition.y)
                                .setZ(minPosition.z)));
        return newBuilder.build();
    }

    /**
     * Returns the coordinate-wise minimum of the two points.
     *
     * @param point1 First point.
     * @param point2 Second point.
     * @return Point containing the minima of the two points' coordinates.
     */
    private static Point3d getMin(final Point3d point1, final Point3d point2) {
        return new Point3d(point1.x < point2.x ? point1.x : point2.x,
                point1.y < point2.y ? point1.y : point2.y,
                point1.z < point2.z ? point1.z : point2.z);
    }

    /**
     * Returns the coordinate-wise maximum of the two points.
     *
     * @param point1 First point.
     * @param point2 Second point.
     * @return Point containing the maxima of the two points' coordinates.
     */
    private static Point3d getMax(final Point3d point1, final Point3d point2) {
        return new Point3d(point1.x > point2.x ? point1.x : point2.x,
                point1.y > point2.y ? point1.y : point2.y,
                point1.z > point2.z ? point1.z : point2.z);
    }

    /**
     * Returns the points that need to be checked for minima and maxima to
     * create the unit group bounding box for a certain member.
     *
     * @param rootFrameId The frame id of the root location.
     * @param unitConfig Config of the member unit.
     * @return Corner points of the member's bounding box in root coordinates.
     * @throws NotAvailableException is thrown if the points are not available
     * due to a missing unit transformation.
     */
    private static List<Point3d> getPointsToCheck(final String rootFrameId, final UnitConfigOrBuilder unitConfig) throws NotAvailableException {
        final List<Point3d> pointsToCheck = new ArrayList<>();
        final Transform3D unitTransformation;
        try {
            unitTransformation = getUnitToRootTransform3D(rootFrameId, unitConfig);
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("Points to check", ex);
        }
        if (unitConfig.hasPlacementConfig() && unitConfig.getPlacementConfig().hasShape() && unitConfig.getPlacementConfig().getShape().hasBoundingBox()) {
            AxisAlignedBoundingBox3DFloat boundingBox = unitConfig.getPlacementConfig().getShape().getBoundingBox();
            return getCorners(boundingBox).stream().map(corner -> {
                unitTransformation.transform(corner);
                return corner;
            }).collect(Collectors.toList());
        } else {
            Point3d position = new Point3d(0, 0, 0);
            unitTransformation.transform(position);
            pointsToCheck.add(position);
        }
        return pointsToCheck;
    }

    /**
     * Returns the corners of the given bounding box in local coordinates.
     *
     * @param boundingBox The bounding box.
     * @return The corners of the bounding box in local coordinates.
     */
    private static List<Point3d> getCorners(AxisAlignedBoundingBox3DFloat boundingBox) {
        final List<Point3d> corners = new ArrayList<>();
        Translation lfb = boundingBox.getLeftFrontBottom();
        Point3d leftFrontBottom = new Point3d(lfb.getX(), lfb.getY(), lfb.getZ());
        for (boolean w : BOOLEAN_VALUES) {
            for (boolean d : BOOLEAN_VALUES) {
                for (boolean h : BOOLEAN_VALUES) {
                    Point3d p = new Point3d(leftFrontBottom);
                    if (w) {
                        p.x += boundingBox.getWidth();
                    }
                    if (d) {
                        p.y += boundingBox.getDepth();
                    }
                    if (h) {
                        p.z += boundingBox.getHeight();
                    }
                    corners.add(p);
                }
            }
        }
        return corners;
    }

    /**
     * Gets the frame id of the root location.
     *
     * @param locationRegistry the location registry.
     * @return the frame id of the root location.
     * @throws NotAvailableException is thrown if the frame id is not available.
     */
    private String getRootFrameId(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry) throws NotAvailableException {
        String rootLocationId;
        try {
            // resolvement via the location registry.
            rootLocationId = LocationUtils.getRootLocation(locationRegistry.getMessages()).getId();
        } catch (CouldNotPerformException ex) {
            // if the root location could not be detected this consistency check is not needed.
            throw new NotAvailableException("RootFrameId", ex);
        }

        try {
            // skip if root location is not ready
            final UnitConfig rootUnitConfig = locationRegistry.get(rootLocationId).getMessage();
            if (!rootUnitConfig.getPlacementConfig().hasTransformationFrameId()
                    || rootUnitConfig.getPlacementConfig().getTransformationFrameId().isEmpty()) {
                throw new CouldNotPerformException("root UnitConfig has not TransformationFrameId.");
            }
            return rootUnitConfig.getPlacementConfig().getTransformationFrameId();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("RootFrameId", ex);
        }
    }

    /**
     * Returns the Transformation from root to unit coordinates for the given
     * root frame id and unit config.
     *
     * @param rootFrameId the frame id of the root location.
     * @param unitConfig UnitConfig of the unit to transform to.
     * @return The Transformation from root to unit coordinates.
     * @throws NotAvailableException is thrown if the transformation is not
     * available.
     */
    private static Transform3D getRootToUnitTransform3D(final String rootFrameId, final UnitConfigOrBuilder unitConfig) throws NotAvailableException {
        try {
            // lookup global transformation
            return GlobalTransformReceiver.getInstance().lookupTransform(
                    unitConfig.getPlacementConfig().getTransformationFrameId(),
                    rootFrameId,
                    System.currentTimeMillis()).getTransform();
        } catch (TransformerException ex) {
            throw new NotAvailableException("RootToUnitTransform", ex);
        }
    }

    /**
     * Returns the Transformation from unit to root coordinates for the given
     * root frame id and unit config.
     *
     * @param rootFrameId the frame id of the root location.
     * @param unitConfig UnitConfig of the unit to transform from.
     * @return The Transformation from unit to root coordinates.
     * @throws NotAvailableException is thrown if the transformation is not
     * available.
     */
    private static Transform3D getUnitToRootTransform3D(final String rootFrameId, final UnitConfigOrBuilder unitConfig) throws NotAvailableException {
        try {
            // lookup global transformation
            return GlobalTransformReceiver.getInstance().lookupTransform(
                    rootFrameId,
                    unitConfig.getPlacementConfig().getTransformationFrameId(),
                    System.currentTimeMillis()).getTransform();
        } catch (TransformerException ex) {
            throw new NotAvailableException("UnitToRootTransform", ex);
        }
    }
}
