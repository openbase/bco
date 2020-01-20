package org.openbase.bco.registry.unit.lib.provider;

/*-
 * #%L
 * BCO Registry Unit Library
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.bco.registry.lib.provider.RootLocationConfigProvider;
import org.openbase.bco.registry.lib.provider.UnitConfigCollectionProvider;
import org.openbase.bco.registry.unit.lib.generator.UnitShapeGenerator;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.rct.GlobalTransformReceiver;
import org.openbase.rct.Transform;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.geometry.AxisAlignedBoundingBox3DFloatType;
import org.openbase.type.geometry.RotationType;
import org.openbase.type.geometry.TranslationType;
import org.openbase.type.math.Vec3DDoubleType.Vec3DDouble;
import org.openbase.type.spatial.ShapeType.Shape;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import java.util.concurrent.Future;


public interface UnitTransformationProviderRegistry<D> extends RootLocationConfigProvider, DataProvider<D>, UnitConfigCollectionProvider {

    default UnitConfig getRootLocationConfig() throws CouldNotPerformException {
        for (UnitConfig locationConfig : getUnitConfigsByUnitType(UnitType.LOCATION)) {
            if (locationConfig.getLocationConfig().hasRoot() && locationConfig.getLocationConfig().getRoot()) {
                return locationConfig;
            }
        }
        throw new NotAvailableException("rootlocation");
    }

    /**
     * Method returns the transformation which leads from the root location to the given unit.
     *
     * @param unitConfigTarget the unit where the transformation leads to.
     *
     * @return a transformation future
     */
    default Future<Transform> getRootToUnitTransformation(final UnitConfig unitConfigTarget) {
        try {
            return getUnitTransformation(getRootLocationConfig(), unitConfigTarget);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(new NotAvailableException("UnitTransformation", ex));
        }
    }

    /**
     * Method returns the transformation which leads from the given unit to the root location.
     *
     * @param unitConfigTarget the unit where the transformation leads to.
     *
     * @return a transformation future
     */
    default Future<Transform> getUnitToRootTransformation(final UnitConfig unitConfigTarget) {
        try {
            return getUnitTransformation(unitConfigTarget, getRootLocationConfig());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(new NotAvailableException("UnitTransformation", ex));
        }
    }

    /**
     * Method returns the transformation between the given unit A and the given unit B.
     *
     * @param unitConfigSource the unit used as transformation base.
     * @param unitConfigTarget the unit where the transformation leads to.
     *
     * @return a transformation future
     */
    default Future<Transform> getUnitTransformation(final UnitConfig unitConfigSource, final UnitConfig unitConfigTarget) {

        if (unitConfigSource.getEnablingState().getValue() != State.ENABLED) {
            return FutureProcessor.canceledFuture(new InvalidStateException("Source Unit[" + unitConfigSource.getLabel() + ":" + unitConfigSource.getId() + "] is disbled and does not provide any transformation!"));
        }

        if (unitConfigTarget.getEnablingState().getValue() != State.ENABLED) {
            return FutureProcessor.canceledFuture(new InvalidStateException("Target Unit[" + unitConfigTarget.getLabel() + ":" + unitConfigTarget.getId() + "] is disbled and does not provide any transformation!"));
        }

        if (!unitConfigSource.hasPlacementConfig() || !unitConfigSource.getPlacementConfig().hasPose()) {
            return FutureProcessor.canceledFuture(new InvalidStateException("Source Unit[" + unitConfigSource.getLabel() + ":" + unitConfigSource.getId() + "] does not provide any position!"));
        }

        if (!unitConfigTarget.hasPlacementConfig() || !unitConfigTarget.getPlacementConfig().hasPose()) {
            return FutureProcessor.canceledFuture(new InvalidStateException("Target Unit[" + unitConfigTarget.getLabel() + ":" + unitConfigTarget.getId() + "] does not provide any position!"));
        }

        if (!unitConfigSource.getPlacementConfig().hasTransformationFrameId() || unitConfigSource.getPlacementConfig().getTransformationFrameId().isEmpty()) {
            return FutureProcessor.canceledFuture(new InvalidStateException("Source Unit[" + unitConfigSource.getLabel() + ":" + unitConfigSource.getId() + "] does not provide yet a transformation frame id!"));
        }

        if (!unitConfigTarget.getPlacementConfig().hasTransformationFrameId() || unitConfigTarget.getPlacementConfig().getTransformationFrameId().isEmpty()) {
            return FutureProcessor.canceledFuture(new InvalidStateException("Target Unit[" + unitConfigTarget.getLabel() + ":" + unitConfigTarget.getId() + "] does not provide yet a transformation frame id!"));
        }


        Future<Transform> transformationFuture = null;
        try {
            transformationFuture = GlobalTransformReceiver.getInstance().requestTransform(
                    unitConfigTarget.getPlacementConfig().getTransformationFrameId(),
                    unitConfigSource.getPlacementConfig().getTransformationFrameId(),
                    System.currentTimeMillis());
        } catch (NotAvailableException ex) {
            return FutureProcessor.canceledFuture(Transform.class, new NotAvailableException("UnitTransformation", ex));
        }
        return FutureProcessor.allOfInclusiveResultFuture(transformationFuture, getDataFuture());
    }

    /**
     * Gets the Transform3D of the transformation from root to unit coordinate system.
     *
     * @param unitConfig the unit config to refer the unit.
     *
     * @return a future of a transform relative to root location
     */
    default Future<Transform3D> getRootToUnitTransform3D(final UnitConfig unitConfig) {
        return FutureProcessor.postProcess((input, time, timeUnit) -> input.getTransform(), getRootToUnitTransformation(unitConfig));
    }

    /**
     * Gets the inverse Transform3D to getTransform3D().
     * This is basically rotation and translation of the object in the root coordinate system
     * and thereby the inverse transformation to the one returned by getTransform3D().
     *
     * @param unitConfig the unit config to refer the unit.
     *
     * @return a future of a transform relative to root location
     */
    default Future<Transform3D> getUnitToRootTransform3D(final UnitConfig unitConfig) {
        return FutureProcessor.postProcess((input, time, timeUnit)  -> input.getTransform(), getUnitToRootTransformation(unitConfig));
    }

    /**
     * Gets the position of the unit relative to the root location as a Point3d object.
     *
     * @param unitConfig the unit config to refer the unit.
     *
     * @return a future of a position relative to the root location
     */
    default Future<Point3d> getUnitPositionGlobalPoint3d(final UnitConfig unitConfig) {
        return FutureProcessor.postProcess((input, time, timeUnit)  -> new Point3d(input), getUnitPositionGlobalVector3d(unitConfig));
    }

    /**
     * Gets the position of the unit relative to the root location as a Point3d object.
     *
     * @param unitConfig the unit config to refer the unit.
     *
     * @return a future of a position relative to the root location
     */
    @RPCMethod
    default Future<Vec3DDouble> getUnitPositionGlobalVec3DDouble(final UnitConfig unitConfig) {
        return FutureProcessor.postProcess((vec, time, timeUnit) -> Vec3DDouble.newBuilder().setX(vec.x).setY(vec.y).setZ(vec.z).build(), getUnitPositionGlobalVector3d(unitConfig));
    }

    /**
     * Gets the position of the unit relative to the root location as a Vector3d object.
     *
     * @param unitConfig the unit config to refer the unit.
     *
     * @return a future of a position relative to the root location
     */
    default Future<Vector3d> getUnitPositionGlobalVector3d(final UnitConfig unitConfig) {
        return FutureProcessor.postProcess((transformation, time, timeUnit)  -> {
            final Vector3d pos = new Vector3d();
            transformation.get(pos);
            return pos;
        }, getUnitToRootTransform3D(unitConfig));
    }

    /**
     * Gets the position of the unit relative to the root location as a Translation object.
     *
     * @param unitConfig the unit config to refer the unit.
     *
     * @return a future of a position relative to the root location
     */
    default Future<TranslationType.Translation> getUnitPositionGlobal(final UnitConfig unitConfig) {
        return FutureProcessor.postProcess((pos, time, timeUnit) -> TranslationType.Translation.newBuilder().setX(pos.x).setY(pos.y).setZ(pos.z).build(), getUnitPositionGlobalPoint3d(unitConfig));
    }

    /**
     * Gets the rotation of the unit relative to the root location as a Quat4d object.
     *
     * @param unitConfig the unit config to refer the unit.
     *
     * @return a future of a rotation relative to the root location
     */
    default Future<Quat4d> getUnitRotationGlobalQuat4d(final UnitConfig unitConfig) {
        return FutureProcessor.postProcess(((transformation, time, timeUnit)  -> {
            final Quat4d quat = new Quat4d();
            transformation.get(quat);
            return quat;
        }), getUnitToRootTransform3D(unitConfig));
    }

    /**
     * Gets the rotation of the unit relative to the root location as a Rotation object.
     *
     * @param unitConfig the unit config to refer the unit.
     *
     * @return a future of a rotation relative to the root location
     */
    default Future<RotationType.Rotation> getUnitRotationGlobal(final UnitConfig unitConfig) {
        return FutureProcessor.postProcess(((quat, time, timeUnit)  -> RotationType.Rotation.newBuilder().setQw(quat.w).setQx(quat.x).setQy(quat.y).setQz(quat.z).build()), getUnitRotationGlobalQuat4d(unitConfig));
    }

    /**
     * Gets the center coordinates of the unit's BoundingBox in the coordinate system of the root location as a Point3d object.
     *
     * @param unitConfig the unit config to refer the unit.
     *
     * @return a future of a center coordinates of the unit's BoundingBox relative to root location
     */
    default Future<Point3d> getUnitBoundingBoxCenterGlobalPoint3d(final UnitConfig unitConfig) {
        return FutureProcessor.postProcess(((transformation, time, timeUnit)  -> {
            final Point3d center = getUnitBoundingBoxCenterPoint3d(unitConfig);
            transformation.transform(center);
            return center;
        }), getUnitToRootTransform3D(unitConfig));
    }

    /**
     * Gets the center coordinates of the unit's BoundingBox in the unit coordinate system as a Point3d object.
     *
     * @param unitConfig the unit config to refer the unit.
     *
     * @return center coordinates of the unit's BoundingBox relative to unit
     *
     * @throws NotAvailableException is thrown if the center can not be calculate.
     */
    default Point3d getUnitBoundingBoxCenterPoint3d(final UnitConfig unitConfig) throws NotAvailableException {
        final AxisAlignedBoundingBox3DFloatType.AxisAlignedBoundingBox3DFloat bb = getUnitShapeByUnitConfig(unitConfig).getBoundingBox();
        final TranslationType.Translation lfc = bb.getLeftFrontBottom();

        final Point3d center = new Point3d(bb.getWidth(), bb.getDepth(), bb.getHeight());
        center.scale(0.5);
        center.add(new Point3d(lfc.getX(), lfc.getY(), lfc.getZ()));
        return center;
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
    default Shape getUnitShapeByUnitId(final String unitId) throws NotAvailableException {
        try {
            return getUnitShapeByUnitConfig(getUnitConfigById(unitId));
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
    default Shape getUnitShapeByUnitConfig(final UnitConfig unitConfig) throws NotAvailableException {
        return UnitShapeGenerator.generateUnitShape(unitConfig, this, CachedClassRegistryRemote.getRegistry());
    }
}
