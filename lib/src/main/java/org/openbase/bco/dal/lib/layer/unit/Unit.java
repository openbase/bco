package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.ServiceProvider;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import org.openbase.jul.iface.Configurable;
import org.openbase.jul.iface.Identifiable;
import org.openbase.jul.iface.Snapshotable;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.iface.provider.LabelProvider;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.registry.location.lib.LocationRegistry;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.schedule.FutureProcessor;
import rct.Transform;
import rst.geometry.RotationType;
import rst.geometry.RotationType.Rotation;
import rst.geometry.TranslationType;
import rst.geometry.TranslationType.Translation;
import rst.spatial.ShapeType.Shape;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * @param <D> the data type of this unit used for the state synchronization.
 */
public interface Unit<D> extends LabelProvider, ScopeProvider, Identifiable<String>, Configurable<String, UnitConfig>, DataProvider<D>, ServiceProvider, Service, Snapshotable<Snapshot> {

    /**
     * Returns the type of this unit.
     *
     * @return UnitType the unit type defining which unit template is provided by this unit.
     * @throws NotAvailableException is thrown if the unit type is currently not available.
     */
    public UnitType getUnitType() throws NotAvailableException;

    /**
     *
     * @return
     * @throws NotAvailableException
     * @deprecated please use {@code getUnitType()} instead.
     */
    @Deprecated
    default public UnitType getType() throws NotAvailableException {
        return getUnitType();
    }

    /**
     * Returns the related template for this unit.
     *
     * Note: The unit template defines which services are provided by this unit.
     *
     * @return UnitTemplate the unit template of this unit.
     * @throws NotAvailableException in case the unit template is not available.
     */
    public UnitTemplate getUnitTemplate() throws NotAvailableException;

    /**
     * Returns the related template for this unit.
     *
     * Note: The unit template defines which services are provided by this unit.
     *
     * @return UnitTemplate the unit template of this unit.
     * @throws NotAvailableException in case the unit template is not available.
     * @deprecated please use {@code getUnitTemplate()} instead.
     */
    @Deprecated
    default public UnitTemplate getTemplate() throws NotAvailableException {
        return getUnitTemplate();
    }

    /**
     * Method returns the unit shape of this unit.
     *
     * If this unit configuration does not provide any shape information the shape of the unit host will be returned.
     * In case the unit host even does not provide any shape information and the unit is a device than the shape of the device class will be used.
     *
     * @return the shape representing the unit.
     * @throws NotAvailableException is thrown if the unit shape is not available or the resolution has been failed.
     */
    default public Shape getUnitShape() throws NotAvailableException {
        try {
            try {
                return Registries.getLocationRegistry().getUnitShape(getConfig());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new FatalImplementationErrorException("getLocationRegistry should not throw InterruptedExceptions anymore!", Unit.class, ex);
            }
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("UnitShape", ex);
        }
    }

    public default void verifyOperationServiceState(final Object serviceState) throws VerificationFailedException {

        if (serviceState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        final Method valueMethod;
        try {
            valueMethod = serviceState.getClass().getMethod("getValue");
        } catch (NoSuchMethodException ex) {
            // service state does contain any value so verification is not possible.
            return;
        }

        try {
            verifyOperationServiceStateValue((Enum) valueMethod.invoke(serviceState));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassCastException ex) {
            ExceptionPrinter.printHistory("Operation service verification phase failed!", ex, LoggerFactory.getLogger(getClass()));
        }
    }

    public default void verifyOperationServiceStateValue(final Enum value) throws VerificationFailedException {

        if (value == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceStateValue"));
        }

        if (value.name().equals("UNKNOWN")) {
            throw new VerificationFailedException("UNKNOWN." + value.getClass().getSimpleName() + " is an invalid operation service state of " + this + "!");
        }
    }

    @RPCMethod
    @Override
    public default Future<Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        MultiException.ExceptionStack exceptionStack = null;
        Snapshot.Builder snapshotBuilder = Snapshot.newBuilder();
        for (ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {
            try {
                ServiceStateDescription.Builder serviceStateDescription = ServiceStateDescription.newBuilder().setServiceType(serviceDescription.getType()).setUnitId(getId());

                // skip non operation services.
                if (serviceDescription.getPattern() != ServiceTemplate.ServicePattern.OPERATION) {
                    continue;
                }

                // load operation service attribute by related provider service
                Object serviceAttribute = Services.invokeServiceMethod(serviceDescription.getType(), ServiceTemplate.ServicePattern.PROVIDER, this);
                //System.out.println("load[" + serviceAttribute + "] type: " + serviceAttribute.getClass().getSimpleName());

                // verify operation service state (e.g. ignore UNKNOWN service states)
                verifyOperationServiceState(serviceAttribute);

                // fill action config
                final ServiceJSonProcessor serviceJSonProcessor = new ServiceJSonProcessor();
                try {
                    serviceStateDescription.setServiceAttribute(serviceJSonProcessor.serialize(serviceAttribute));
                } catch (InvalidStateException ex) {
                    // skip if serviceAttribute is empty.
                    continue;
                }
                serviceStateDescription.setUnitId(getId());
                serviceStateDescription.setUnitType(getUnitTemplate().getType());
                serviceStateDescription.setServiceType(serviceDescription.getType());
                serviceStateDescription.setServiceType(serviceDescription.getType());
                serviceStateDescription.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(serviceAttribute));

                // add action config
                snapshotBuilder.addServiceStateDescription(serviceStateDescription.build());
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
        }
        MultiException.checkAndThrow("Could not record snapshot!", exceptionStack);
        return CompletableFuture.completedFuture(snapshotBuilder.build());
    }

    @RPCMethod
    @Override
    public default Future<Void> restoreSnapshot(final Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
        try {
            Collection<Future> futureCollection = new ArrayList<>();
            for (final ServiceStateDescription serviceStateDescription : snapshot.getServiceStateDescriptionList()) {
                ActionDescription actionDescription = ActionDescription.newBuilder().setServiceStateDescription(serviceStateDescription).build();
                futureCollection.add(applyAction(actionDescription));
            }
            return GlobalCachedExecutorService.allOf(futureCollection);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not record snapshot!", ex);
        }
    }

    /**
     * Gets the position of the unit relative to its parent location.
     *
     * @return relative position
     * @throws NotAvailableException is thrown if the config is not available.
     * @deprecated please use {@code getUnitPosition()} instead.
     */
    @Deprecated
    default public TranslationType.Translation getLocalPosition() throws NotAvailableException {
        return getUnitPosition();
    }

    /**
     * Gets the rotation of the unit relative to its parent location.
     *
     * @return relative rotation
     * @throws NotAvailableException is thrown if the config is not available.
     * @deprecated please use {@code getUnitRotation()} instead.
     */
    @Deprecated
    default public RotationType.Rotation getLocalRotation() throws NotAvailableException {
        return getUnitRotation();
    }

    /**
     * Gets the local position of the unit relative to its parent location.
     *
     * @return relative position
     * @throws NotAvailableException is thrown if the unit config or parts of it are not available.
     */
    default public TranslationType.Translation getUnitPosition() throws NotAvailableException {
        try {
            if (!getConfig().hasPlacementConfig()) {
                throw new NotAvailableException("PlacementConfig");
            }
            //release todo: rename PlacementConfig position into pose.
            if (!getConfig().getPlacementConfig().hasPosition()) {
                throw new NotAvailableException("Position");
            }
            return getConfig().getPlacementConfig().getPosition().getTranslation();
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("UnitPosition", ex);
        }
    }

    /**
     * Gets the local rotation of the unit relative to its parent location.
     *
     * @return relative rotation
     * @throws NotAvailableException is thrown if the unit config or parts of it are not available.
     */
    default public RotationType.Rotation getUnitRotation() throws NotAvailableException {
        try {
            if (!getConfig().hasPlacementConfig()) {
                throw new NotAvailableException("PlacementConfig");
            }
            if (!getConfig().getPlacementConfig().hasPosition()) {
                throw new NotAvailableException("Position");
            }
            return getConfig().getPlacementConfig().getPosition().getRotation();
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("UnitRotation", ex);
        }
    }

    /**
     * Gets the Transform3D of the transformation from root to unit coordinate system.
     *
     * @return transform relative to root location
     * @throws NotAvailableException is thrown if the transformation is not available.
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     * @deprecated please use {@code getRootToUnitTransform3D()} instead.
     */
    default public Transform3D getTransform3D() throws NotAvailableException, InterruptedException {
        return getRootToUnitTransform3D();
    }

    /**
     * Gets the inverse Transform3D to getTransform3D().
     * This is basically rotation and translation of the object in the root coordinate system
     * and thereby the inverse transformation to the one returned by getTransform3D().
     *
     * @return transform relative to root location
     * @throws NotAvailableException is thrown if the transformation is not available.
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     * @deprecated please use {@code getUnitToRootTransform3D()} instead.
     */
    @Deprecated
    default public Transform3D getTransform3DInverse() throws NotAvailableException, InterruptedException {
        return getUnitToRootTransform3D();
    }

    /**
     * Gets the position of the unit relative to the root location as a Point3d object.
     *
     * @return position relative to the root location
     * @throws NotAvailableException is thrown if the transformation is not available.
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     * @deprecated please use {@code getUnitPositionGlobalPoint3d()} instead.
     */
    @Deprecated
    default public Point3d getGlobalPositionPoint3d() throws NotAvailableException, InterruptedException {
        return getUnitPositionGlobalPoint3d();
    }

    /**
     * Gets the position of the unit relative to the root location as a Translation object.
     *
     * @return position relative to the root location
     * @throws NotAvailableException is thrown if the transformation is not available.
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     * @deprecated please use {@code getUnitPositionGlobal()} instead.
     */
    @Deprecated
    default public TranslationType.Translation getGlobalPosition() throws NotAvailableException, InterruptedException {
        return getUnitPositionGlobal();
    }

    /**
     * Gets the rotation of the unit relative to the root location as a Quat4d object.
     *
     * @return rotation relative to the root location
     * @throws NotAvailableException is thrown if the transformation is not available.
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     * @deprecated please use {@code getUnitRotationGlobalQuat4d()} instead.
     */
    @Deprecated
    default public Quat4d getGlobalRotationQuat4d() throws NotAvailableException, InterruptedException {
        return getUnitRotationGlobalQuat4d();
    }

    /**
     * Gets the rotation of the unit relative to the root location as a Rotation object.
     *
     * @return rotation relative to the root location
     * @throws NotAvailableException is thrown if the transformation is not available.
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     * @deprecated please use {@code getUnitRotationGlobal()} instead.
     */
    @Deprecated
    default public Rotation getGlobalRotation() throws NotAvailableException, InterruptedException {
        return getUnitRotationGlobal();
    }

    /**
     * Gets the center coordinates of the unit's BoundingBox in the unit coordinate system as a Point3d object.
     *
     * @return center coordinates of the unit's BoundingBox relative to unit
     * @throws NotAvailableException is thrown if the center can not be calculate.
     * @deprecated please use {@code getUnitBoundingBoxCenterPoint3d()} instead.
     */
    @Deprecated
    default public Point3d getLocalBoundingBoxCenterPoint3d() throws NotAvailableException {
        return getUnitBoundingBoxCenterPoint3d();
    }

    /**
     * Gets the center coordinates of the unit's BoundingBox in the coordinate system of the root location as a Point3d object.
     *
     * @return center coordinates of the unit's BoundingBox relative to root location
     * @throws NotAvailableException is thrown if the center can not be calculate.
     * @throws java.lang.InterruptedException
     * @deprecated please use {@code getUnitBoundingBoxCenterGlobalPoint3d()} instead.
     */
    @Deprecated
    default public Point3d getGlobalBoundingBoxCenterPoint3d() throws NotAvailableException, InterruptedException {
        return getUnitBoundingBoxCenterGlobalPoint3d();
    }

    /**
     * Method returns the transformation leading from the root location to this unit.
     *
     * @return a transformation future
     */
    public default Future<Transform> getRootToUnitTransformationFuture() {
        try {
            return getLocationRegistry().getRootToUnitTransformationFuture(getConfig());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(new NotAvailableException("UnitTransformation", ex));
        }
    }

    /**
     * Method returns the transformation leading from the unit to the root location.
     *
     * @return a transformation future
     */
    public default Future<Transform> getUnitToRootTransformationFuture() {
        try {
            return getLocationRegistry().getUnitToRootTransformationFuture(getConfig());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(new NotAvailableException("UnitTransformation", ex));
        }
    }

    /**
     * Method returns the transformation leading from the root location to this unit.
     *
     * @return the transformation
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    public default Transform getRootToUnitTransformation() throws NotAvailableException {
        try {
            return getLocationRegistry().getRootToUnitTransformation(getConfig());
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("UnitTransformation", ex);
        }
    }

    /**
     * Method returns the transformation leading from the unit to the root location.
     *
     * @return the transformation
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    public default Transform getUnitToRootTransformation() throws NotAvailableException {
        try {
            return getLocationRegistry().getUnitToRootTransformation(getConfig());
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("UnitTransformation", ex);
        }
    }

    /**
     * Gets the Transform3D of the transformation from root to unit coordinate system.
     *
     * @return transform relative to root location
     * @throws NotAvailableException is thrown if the transformation is not available.
     */
    default public Transform3D getRootToUnitTransform3D() throws NotAvailableException {
        try {
            return getLocationRegistry().getRootToUnitTransform3D(getConfig());
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("Transform3D", ex);
        }
    }

    /**
     * Gets the transformation leading from the unit to the root location.
     * This is basically rotation and translation of the object in the root coordinate system
     * and thereby the inverse transformation to the one returned by getTransform3D().
     *
     * @return transform relative to root location
     * @throws NotAvailableException is thrown if the transformation is not available.
     */
    default public Transform3D getUnitToRootTransform3D() throws NotAvailableException {
        try {
            return getLocationRegistry().getUnitToRootTransform3D(getConfig());
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("Transform3Dinverse", ex);
        }
    }

    /**
     * Gets the position of the unit relative to the root location as a Point3d object.
     *
     * @return position relative to the root location
     * @throws NotAvailableException is thrown if the transformation is not available.
     */
    default public Point3d getUnitPositionGlobalPoint3d() throws NotAvailableException {
        try {
            return getLocationRegistry().getUnitPositionGlobalPoint3d(getConfig());
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("GlobalPositionVector", ex);
        }
    }

    /**
     * Gets the position of the unit relative to the root location as a Translation object.
     *
     * @return position relative to the root location
     * @throws NotAvailableException is thrown if the transformation is not available.
     */
    default public Translation getUnitPositionGlobal() throws NotAvailableException {
        try {
            return getLocationRegistry().getUnitPositionGlobal(getConfig());
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("GlobalPosition", ex);
        }
    }

    /**
     * Gets the rotation of the unit relative to the root location as a Quat4d object.
     *
     * @return rotation relative to the root location
     * @throws NotAvailableException is thrown if the transformation is not available.
     */
    default public Quat4d getUnitRotationGlobalQuat4d() throws NotAvailableException {
        try {
            return getLocationRegistry().getUnitRotationGlobalQuat4d(getConfig());
        } catch (final NotAvailableException ex) {
            throw new NotAvailableException("GlobalRotationQuat", ex);
        }
    }

    /**
     * Gets the rotation of the unit relative to the root location as a Rotation object.
     *
     * @return rotation relative to the root location
     * @throws NotAvailableException is thrown if the transformation is not available.
     */
    default public Rotation getUnitRotationGlobal() throws NotAvailableException {
        try {
            return getLocationRegistry().getUnitRotationGlobal(getConfig());
        } catch (final NotAvailableException ex) {
            throw new NotAvailableException("GlobalRotation", ex);
        }
    }

    /**
     * Gets the center coordinates of the unit's BoundingBox in the unit coordinate system as a Point3d object.
     *
     * @return center coordinates of the unit's BoundingBox relative to unit
     * @throws NotAvailableException is thrown if the center can not be calculate.
     */
    default public Point3d getUnitBoundingBoxCenterPoint3d() throws NotAvailableException {
        return getLocationRegistry().getUnitBoundingBoxCenterPoint3d(getConfig());
    }

    /**
     * Gets the center coordinates of this unit's BoundingBox in the coordinate system of the root location as a Point3d object.
     *
     * @return center coordinates of this unit's BoundingBox relative to root location
     * @throws NotAvailableException is thrown if the center can not be calculate.
     */
    default public Point3d getUnitBoundingBoxCenterGlobalPoint3d() throws NotAvailableException {
        try {
            return getLocationRegistry().getUnitBoundingBoxCenterGlobalPoint3d(getConfig());
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("GlobalBoundingBoxCenter", ex);
        }
    }

    /**
     * Do not use this method! Use Registries.getLocationRegistry() instead!
     *
     * @return
     * @throws org.openbase.jul.exception.NotAvailableException
     * @deprecated Do not use this method! Use Registries.getLocationRegistry() instead!
     */
    @Deprecated
    default public LocationRegistry getLocationRegistry() throws NotAvailableException {
        // method is only needed because the registry is still throwing a InterruptedException which will removed in a future release.
        // release todo: can be removed later on
        try {
            try {
                return CachedLocationRegistryRemote.getRegistry();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new FatalImplementationErrorException("", this, ex);
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException(LocationRegistry.class);
        }
    }
}
