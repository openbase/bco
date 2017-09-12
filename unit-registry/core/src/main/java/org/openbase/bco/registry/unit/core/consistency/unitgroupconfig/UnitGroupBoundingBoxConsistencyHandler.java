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
import org.openbase.jul.exception.printer.ExceptionPrinter;
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
import rst.geometry.TranslationType.Translation;
import rst.spatial.ShapeType.Shape;

/**
 *
 * @author <a href="mailto:thuppke@techfak.uni-bielefeld.de">Thoren Huppke</a>
 */
public class UnitGroupBoundingBoxConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final static Point3d RIGHT = new Point3d(1, 0, 0);
    private final static Point3d FORWARD = new Point3d(0, 1, 0);
    private final static Point3d UP = new Point3d(0, 0, 1);
    private final static boolean[] BOOLEAN_VALUES = new boolean[]{false, true};

    private final ArrayList<ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder>> unitConfigRegistryList;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationUnitConfigRegistry;

    public UnitGroupBoundingBoxConsistencyHandler(final ArrayList<ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder>> unitConfigRegistryList,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationUnitConfigRegistry) {
        this.unitConfigRegistryList = unitConfigRegistryList;
        this.locationUnitConfigRegistry = locationUnitConfigRegistry;
    }

    @Override
    public void processData(final String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap,
            final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        // filter if config does not contain placement or shape
        if (!unitConfig.hasPlacementConfig()
                || !unitConfig.getPlacementConfig().hasShape()
                || unitConfig.getPlacementConfig().getShape().getFloorList().isEmpty()
                || !unitConfig.getPlacementConfig().getShape().getCeilingList().isEmpty()
                || !unitConfig.getPlacementConfig().hasTransformationFrameId()
                || unitConfig.getPlacementConfig().getTransformationFrameId().isEmpty()) {
            return;
        }

        final String rootFrameId;
        final Transform3D unitTransformation;
        try {
            rootFrameId = getRootFrameId(entryMap, locationUnitConfigRegistry);
            unitTransformation = getRootToUnitTransform3D(rootFrameId, unitConfig);
        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory(ex, logger);
            return;
        }

        Point3d minPosition = new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        Point3d maxPosition = new Point3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        for (String memberId : unitConfig.getUnitGroupConfig().getMemberIdList()) {
            UnitConfig memberConf;
            for (ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> reg : unitConfigRegistryList) {
                try {
                    memberConf = reg.get(id).getMessage();
                    List<Point3d> pointsToCheck = getPointsToCheck(rootFrameId, memberConf);
                    minPosition = pointsToCheck.stream().reduce(minPosition, (result, element) -> getMin(result, element));
                    maxPosition = pointsToCheck.stream().reduce(maxPosition, (result, element) -> getMax(result, element));
                    break;
                } catch (CouldNotPerformException ex) {
                }
            }
        }
        if (minPosition.x == Double.POSITIVE_INFINITY) {
            return;
        }
        unitTransformation.transform(minPosition);
        unitTransformation.transform(maxPosition);

        // Create minimum and dimensions in the current objects
        Point3d newMin = getMin(minPosition, maxPosition);
        Point3d dimensions = getMax(minPosition, maxPosition);
        dimensions.sub(newMin);

        //TODO: Continue from here.
        // update PlacementConfig
        final Shape shape = unitConfig.getPlacementConfig().getShape();
        Shape newShape = updateShape(shape, newMin, dimensions);
        if (!shape.equals(newShape)) {
            unitConfig.getPlacementConfigBuilder().setShape(newShape);
            throw new EntryModification(entry.setMessage(unitConfig), this);
        }
    }

    private static Shape updateShape(Shape shape, Point3d minPosition, Point3d dimensions) {
        return shape.toBuilder().setBoundingBox(
                AxisAlignedBoundingBox3DFloat.newBuilder().setLeftFrontBottom(
                        Translation.newBuilder().setX(minPosition.x).setY(minPosition.y).setZ(minPosition.z)
                ).setWidth((float) dimensions.x).setDepth((float) dimensions.y).setHeight((float) dimensions.z)
        ).build();
    }

    private static Point3d getMin(final Point3d point1, final Point3d point2) {
        return new Point3d(point1.x < point2.x ? point1.x : point2.x,
                point1.y < point2.y ? point1.y : point2.y,
                point1.z < point2.z ? point1.z : point2.z);
    }

    private static Point3d getMax(final Point3d point1, final Point3d point2) {
        return new Point3d(point1.x > point2.x ? point1.x : point2.x,
                point1.y > point2.y ? point1.y : point2.y,
                point1.z > point2.z ? point1.z : point2.z);
    }

    private static List<Point3d> getPointsToCheck(final String rootFrameId, final UnitConfigOrBuilder unitConfig) {
        final List<Point3d> pointsToCheck = new ArrayList<>();
        final Transform3D unitTransformation;
        try {
            unitTransformation = getUnitToRootTransform3D(rootFrameId, unitConfig);
        } catch (NotAvailableException ex) {
            return pointsToCheck;
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

    private static String getRootFrameId(final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry) throws NotAvailableException {
        String rootLocationId;
        try {
            // resolvement via more frequently updated entryMap via consistency checks if this entryMap contains the locations.
            rootLocationId = LocationUtils.getRootLocation(entryMap).getId();
        } catch (CouldNotPerformException ex) {
            try {
                // resolvement via the location registry.
                rootLocationId = LocationUtils.getRootLocation(locationRegistry.getMessages()).getId();
            } catch (CouldNotPerformException exx) {
                // if the root location could not be detected this consistency check is not needed.
                throw new NotAvailableException("RootFrameId");
            }
        }

        try {
            // skip if root location is not ready
            final UnitConfig rootUnitConfig = locationRegistry.get(rootLocationId).getMessage();
            if (!rootUnitConfig.getPlacementConfig().hasTransformationFrameId()
                    || rootUnitConfig.getPlacementConfig().getTransformationFrameId().isEmpty()) {
                throw new NotAvailableException("RootFrameId");
            }
            return rootUnitConfig.getPlacementConfig().getTransformationFrameId();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("RootFrameId");
        }
    }

    private static Transform3D getRootToUnitTransform3D(final String rootFrameId, final UnitConfigOrBuilder unitConfig) throws NotAvailableException {
        try {
            // lookup global transformation
            return GlobalTransformReceiver.getInstance().lookupTransform(
                    unitConfig.getPlacementConfig().getTransformationFrameId(),
                    rootFrameId,
                    System.currentTimeMillis()).getTransform();
        } catch (TransformerException ex) {
            throw new NotAvailableException("RootToUnitTransform");
        }
    }

    private static Transform3D getUnitToRootTransform3D(final String rootFrameId, final UnitConfigOrBuilder unitConfig) throws NotAvailableException {
        try {
            // lookup global transformation
            return GlobalTransformReceiver.getInstance().lookupTransform(
                    rootFrameId,
                    unitConfig.getPlacementConfig().getTransformationFrameId(),
                    System.currentTimeMillis()).getTransform();
        } catch (TransformerException ex) {
            throw new NotAvailableException("RootToUnitTransform");
        }
    }
}
