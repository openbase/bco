package org.openbase.bco.registry.unit.core.consistency.unitgroupconfig;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.rct.GlobalTransformReceiver;
import org.openbase.rct.TransformerException;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfigOrBuilder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.geometry.AxisAlignedBoundingBox3DFloatType.AxisAlignedBoundingBox3DFloat;
import org.openbase.type.geometry.PoseType.Pose;
import org.openbase.type.geometry.RotationType.Rotation;
import org.openbase.type.geometry.TranslationType.Translation;
import org.openbase.type.spatial.PlacementConfigType.PlacementConfig;
import org.openbase.type.spatial.ShapeType.Shape;

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
     * @param unitConfigRegistryList     the list of all the unitConfigRegistries.
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
     * @param id       {@inheritDoc}
     * @param entry    {@inheritDoc}
     * @param entryMap {@inheritDoc}
     * @param registry {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws EntryModification        {@inheritDoc}
     */
    @Override
    public void processData(final String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap,
                            final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        final UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();
        final String locationFrameId;

        try {
            final UnitConfig locationConfig = locationUnitConfigRegistry.get(unitConfig.getPlacementConfig().getLocationId()).getMessage();
            if (locationConfig.hasPlacementConfig() && locationConfig.getPlacementConfig().hasTransformationFrameId()) {
                locationFrameId = locationConfig.getPlacementConfig().getTransformationFrameId();
            } else {
                throw new CouldNotPerformException("No TransformationFrameId available for location " + locationConfig.getLabel());
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not get the locationFrameId for the location of UnitGroup " + unitConfig.getLabel());
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
                    //Handled below if memberConf == null!
                    continue;
                }
            }
            if (memberConf == null) {
                throw new CouldNotPerformException("Group member not found in registries.");
            }
            if (memberConf.hasPlacementConfig() && memberConf.getPlacementConfig().hasPose()) {
                try {
                    List<Point3d> pointsToCheck = getPointsToCheck(locationFrameId, memberConf);
                    minPosition = pointsToCheck.stream().reduce(minPosition, (result, element) -> getMin(result, element));
                    maxPosition = pointsToCheck.stream().reduce(maxPosition, (result, element) -> getMax(result, element));
                } catch (NotAvailableException ex) {
                    throw new CouldNotPerformException("Could not get Transformation for member with position.", ex);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new CouldNotPerformException("Shutdown in progress.");
                }
            }
        }

        if (minPosition.x == Double.POSITIVE_INFINITY) {
            return;
        }
        Point3d dimensions = new Point3d(maxPosition);
        dimensions.sub(minPosition);

        // update PlacementConfig
        final PlacementConfig placementConfig = unitConfig.getPlacementConfig();
        PlacementConfig newPlacementConfig = updatePlacementConfig(placementConfig, minPosition, dimensions);
        if (!placementConfig.equals(newPlacementConfig)) {
            unitConfig.setPlacementConfig(newPlacementConfig);
            throw new EntryModification(entry.setMessage(unitConfig, this), this);
        }
    }

    /**
     * Updates the placement config based on the minimal coordinates and
     * dimensions of the bounding box.
     *
     * @param placementConfig the PlacementConfig to be updated.
     * @param minPosition     the minimal coordinates of the new bounding box.
     * @param dimensions      the dimensions of the bounding box (x: width, y: depth,
     *                        z: height).
     *
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
                .setPose(Pose.newBuilder()
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
     *
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
     *
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
     * @param targetFrameId The frame id of the target location.
     * @param unitConfig    Config of the member unit.
     *
     * @return Corner points of the member's bounding box in root coordinates.
     *
     * @throws NotAvailableException is thrown if the points are not available
     *                               due to a missing unit transformation.
     * @throws InterruptedException  is thrown in case of an external
     *                               interruption.
     */
    private List<Point3d> getPointsToCheck(final String targetFrameId, final UnitConfigOrBuilder unitConfig) throws NotAvailableException, InterruptedException {
        try {
            final List<Point3d> pointsToCheck = new ArrayList<>();
            final Transform3D unitTransformation;

            try {
                unitTransformation = getUnitToTargetTransform3D(targetFrameId, unitConfig);
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not get the unit to target transformation for unit " + unitConfig.getId() + " and target frame id " + targetFrameId, ex);
            }

            try {
                Shape unitShape = getUnitShape(unitConfig);
                if (unitShape.hasBoundingBox()) {
                    AxisAlignedBoundingBox3DFloat boundingBox = unitShape.getBoundingBox();
                    return getCorners(boundingBox).stream().map(corner -> {
                        unitTransformation.transform(corner);
                        return corner;
                    }).collect(Collectors.toList());
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
                if (!(ex instanceof NotAvailableException)) {
                    throw new CouldNotPerformException("Registry not available.", ex);
                }
                // The other case should lead to the following code aswell.
            }
            Point3d position = new Point3d(0, 0, 0);
            unitTransformation.transform(position);
            pointsToCheck.add(position);
            return pointsToCheck;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Points to check", ex);
        }
    }

    /**
     * Returns the corners of the given bounding box in local coordinates.
     *
     * @param boundingBox The bounding box.
     *
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
     * Returns the Transformation from unit to target location coordinates for
     * the given target frame id and unit config.
     *
     * @param targetFrameId the frame id of the target location.
     * @param unitConfig    UnitConfig of the unit to transform from.
     *
     * @return The Transformation from unit to root coordinates.
     *
     * @throws NotAvailableException is thrown if the transformation is not
     *                               available.
     */
    private static Transform3D getUnitToTargetTransform3D(final String targetFrameId, final UnitConfigOrBuilder unitConfig) throws NotAvailableException {
        try {
            // lookup global transformation
            return GlobalTransformReceiver.getInstance().lookupTransform(
                    targetFrameId,
                    unitConfig.getPlacementConfig().getTransformationFrameId(),
                    System.currentTimeMillis()).getTransform();
        } catch (TransformerException ex) {
            throw new NotAvailableException("UnitToRootTransform", ex);
        }
    }

    /**
     * Method returns the unit shape of the given unit referred by the id.
     * <p>
     * If this unit configuration does not provide any shape information the shape of the unit host will be returned.
     * In case the unit host even does not provide any shape information and the unit is a device than the shape of the device class will be used.
     *
     * @param unitId the id to resolve the unit shape.
     *
     * @return the shape representing the unit.
     *
     * @throws NotAvailableException is thrown if the unit shape is not available or the resolution has been failed.
     */
    private Shape getUnitShape(final String unitId) throws NotAvailableException {
        try {
            for (ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> registry : unitConfigRegistryList) {
                if (registry.contains(unitId)) {
                    return getUnitShape(registry.getMessage(unitId));
                }
            }
            throw new NotAvailableException("Unit with id[" + unitId + "]");
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("Shape", "of unit " + unitId, ex);
        }
    }

    /**
     * Method returns the unit shape of the given unit configuration.
     * <p>
     * If this unit configuration does not provide any shape information the shape of the unit host will be returned.
     * In case the unit host even does not provide any shape information and the unit is a device than the shape of the device class will be used.
     *
     * @param unitConfig the unit configuration to resolve the unit shape.
     *
     * @return the shape representing the unit.
     *
     * @throws NotAvailableException is thrown if the unit shape is not available or the resolution has been failed.
     */
    private Shape getUnitShape(final UnitConfigOrBuilder unitConfig) throws NotAvailableException {
        try {

            // resolve shape via unit config
            if (unitConfig.hasPlacementConfig() && unitConfig.getPlacementConfig().hasShape()) {
                Shape shape = unitConfig.getPlacementConfig().getShape();
                if (shape.hasBoundingBox() || shape.getCeilingCount() != 0 || shape.getFloorCount() != 0 || shape.getFloorCeilingEdgeCount() != 0) {
                    // Only if shape is not empty!
                    return unitConfig.getPlacementConfig().getShape();
                }
            }

            // resolve shape via unit host
            if (UnitConfigProcessor.isHostUnitAvailable(unitConfig)) {
                return getUnitShape(unitConfig.getUnitHostId());
            }

            // resolve shape via device class
            if (unitConfig.getUnitType().equals(UnitType.DEVICE)) {
                return CachedClassRegistryRemote.getRegistry().getDeviceClassById(unitConfig.getDeviceConfig().getDeviceClassId()).getShape();
            }

            // inform that the resolution is not possible.
            throw new CouldNotPerformException("Shape could not be resolved by any source.");

        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("Shape", "of Unit [" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "]", ex);
        }
    }
}
