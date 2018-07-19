package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.iface.AuthenticatedSnapshotable;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.ServiceProvider;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ProtobufVariableProvider;
import org.openbase.jul.extension.rsb.com.TransactionIdProvider;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import org.openbase.jul.extension.rst.processing.MetaConfigPool;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.iface.Configurable;
import org.openbase.jul.iface.Identifiable;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.iface.provider.LabelProvider;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.processing.VariableProvider;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.slf4j.LoggerFactory;
import rct.Transform;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.app.AppClassType.AppClass;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.geometry.RotationType;
import rst.geometry.RotationType.Rotation;
import rst.geometry.TranslationType;
import rst.geometry.TranslationType.Translation;
import rst.spatial.ShapeType.Shape;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;

/**
 * @param <D> the data type of this unit used for the state synchronization.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface Unit<D> extends LabelProvider, ScopeProvider, Identifiable<String>, Configurable<String, UnitConfig>, DataProvider<D>, ServiceProvider, Service, AuthenticatedSnapshotable, TransactionIdProvider {

    /**
     * Returns the type of this unit.
     *
     * @return UnitType the unit type defining which unit template is provided by this unit.
     *
     * @throws NotAvailableException is thrown if the unit type is currently not available.
     */
    UnitType getUnitType() throws NotAvailableException;

    /**
     * Returns the related template for this unit.
     * <p>
     * Note: The unit template defines which services are provided by this unit.
     *
     * @return UnitTemplate the unit template of this unit.
     *
     * @throws NotAvailableException in case the unit template is not available.
     */
    UnitTemplate getUnitTemplate() throws NotAvailableException;

    /**
     * Returns the related template for this unit.
     * <p>
     * Note: The unit template defines which services are provided by this unit.
     *
     * @return UnitTemplate the unit template of this unit.
     *
     * @throws NotAvailableException in case the unit template is not available.
     * @deprecated please use {@code getUnitTemplate()} instead.
     */
    @Deprecated
    default UnitTemplate getTemplate() throws NotAvailableException {
        return getUnitTemplate();
    }

    /**
     * Method returns the unit shape of this unit.
     * <p>
     * If this unit configuration does not provide any shape information the shape of the unit host will be returned.
     * In case the unit host even does not provide any shape information and the unit is a device than the shape of the device class will be used.
     *
     * @return the shape representing the unit.
     *
     * @throws NotAvailableException is thrown if the unit shape is not available or the resolution has been failed.
     */
    default Shape getUnitShape() throws NotAvailableException {
        try {
            return Registries.getUnitRegistry().getUnitShape(getConfig());
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("UnitShape", ex);
        }
    }

    /**
     * @param serviceState
     *
     * @throws VerificationFailedException
     * @deprecated please use Services.verifyOperationServiceState(...) instead
     */
    @Deprecated
    default void verifyOperationServiceState(final Object serviceState) throws VerificationFailedException {

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

    /**
     * @param value
     *
     * @throws VerificationFailedException
     * @deprecated please use Services.verifyOperationServiceStateValue(...) instead
     */
    @Deprecated
    default void verifyOperationServiceStateValue(final Enum value) throws VerificationFailedException {

        if (value == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceStateValue"));
        }

        if (value.name().equals("UNKNOWN")) {
            throw new VerificationFailedException("UNKNOWN." + value.getClass().getSimpleName() + " is an invalid operation service state of " + this + "!");
        }
    }

    @RPCMethod
    @Override
    default Future<Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                MultiException.ExceptionStack exceptionStack = null;
                Snapshot.Builder snapshotBuilder = Snapshot.newBuilder();
                for (ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {
                    try {
                        ServiceStateDescription.Builder serviceStateDescription = ServiceStateDescription.newBuilder().setServiceType(serviceDescription.getServiceType()).setUnitId(getId());

                        // skip non operation services.
                        if (serviceDescription.getPattern() != ServiceTemplate.ServicePattern.OPERATION) {
                            continue;
                        }

                        // load operation service attribute by related provider service
                        Message serviceAttribute = (Message) Services.invokeServiceMethod(serviceDescription.getServiceType(), ServiceTemplate.ServicePattern.PROVIDER, this);

                        // verify operation service state (e.g. ignore UNKNOWN service states)
                        Services.verifyOperationServiceState(serviceAttribute);

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
                        serviceStateDescription.setServiceType(serviceDescription.getServiceType());
                        serviceStateDescription.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(serviceAttribute));

                        // add action config
                        snapshotBuilder.addServiceStateDescription(serviceStateDescription.build());
                    } catch (CouldNotPerformException | ClassCastException ex) {
                        exceptionStack = MultiException.push(this, ex, exceptionStack);
                    }
                }

                try {
                    MultiException.checkAndThrow("Could not snapshot all service provider!", exceptionStack);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(ex, LoggerFactory.getLogger(Unit.class), LogLevel.WARN);
                }

                return snapshotBuilder.build();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not record snapshot!", ex);
            }
        });
    }

    @RPCMethod
    @Override
    Future<Void> restoreSnapshot(final Snapshot snapshot) throws CouldNotPerformException, InterruptedException;

    @RPCMethod
    Future<AuthenticatedValue> applyActionAuthenticated(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException, InterruptedException;


    /**
     * Gets the position of the unit relative to its parent location.
     *
     * @return relative position
     *
     * @throws NotAvailableException is thrown if the config is not available.
     * @deprecated please use {@code getUnitPosition()} instead.
     */
    @Deprecated
    default TranslationType.Translation getLocalPosition() throws NotAvailableException {
        return getUnitPosition();
    }

    /**
     * Gets the rotation of the unit relative to its parent location.
     *
     * @return relative rotation
     *
     * @throws NotAvailableException is thrown if the config is not available.
     * @deprecated please use {@code getUnitRotation()} instead.
     */
    @Deprecated
    default RotationType.Rotation getLocalRotation() throws NotAvailableException {
        return getUnitRotation();
    }

    /**
     * Gets the local position of the unit relative to its parent location.
     *
     * @return relative position
     *
     * @throws NotAvailableException is thrown if the unit config or parts of it are not available.
     */
    default TranslationType.Translation getUnitPosition() throws NotAvailableException {
        try {
            if (!getConfig().hasPlacementConfig()) {
                throw new NotAvailableException("PlacementConfig");
            }
            //todo release : rename PlacementConfig position into pose.
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
     *
     * @throws NotAvailableException is thrown if the unit config or parts of it are not available.
     */
    default RotationType.Rotation getUnitRotation() throws NotAvailableException {
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
     *
     * @throws NotAvailableException is thrown if the transformation is not available.
     * @throws InterruptedException  is thrown if the thread was externally interrupted.
     * @deprecated please use {@code getRootToUnitTransform3D()} instead.
     */
    default Transform3D getTransform3D() throws NotAvailableException, InterruptedException {
        return getRootToUnitTransform3D();
    }

    /**
     * Gets the inverse Transform3D to getTransform3D().
     * This is basically rotation and translation of the object in the root coordinate system
     * and thereby the inverse transformation to the one returned by getTransform3D().
     *
     * @return transform relative to root location
     *
     * @throws NotAvailableException is thrown if the transformation is not available.
     * @throws InterruptedException  is thrown if the thread was externally interrupted.
     * @deprecated please use {@code getUnitToRootTransform3D()} instead.
     */
    @Deprecated
    default Transform3D getTransform3DInverse() throws NotAvailableException, InterruptedException {
        return getUnitToRootTransform3D();
    }

    /**
     * Gets the position of the unit relative to the root location as a Point3d object.
     *
     * @return position relative to the root location
     *
     * @throws NotAvailableException is thrown if the transformation is not available.
     * @throws InterruptedException  is thrown if the thread was externally interrupted.
     * @deprecated please use {@code getUnitPositionGlobalPoint3d()} instead.
     */
    @Deprecated
    default Point3d getGlobalPositionPoint3d() throws NotAvailableException, InterruptedException {
        return getUnitPositionGlobalPoint3d();
    }

    /**
     * Gets the position of the unit relative to the root location as a Translation object.
     *
     * @return position relative to the root location
     *
     * @throws NotAvailableException is thrown if the transformation is not available.
     * @throws InterruptedException  is thrown if the thread was externally interrupted.
     * @deprecated please use {@code getUnitPositionGlobal()} instead.
     */
    @Deprecated
    default TranslationType.Translation getGlobalPosition() throws NotAvailableException, InterruptedException {
        return getUnitPositionGlobal();
    }

    /**
     * Gets the rotation of the unit relative to the root location as a Quat4d object.
     *
     * @return rotation relative to the root location
     *
     * @throws NotAvailableException is thrown if the transformation is not available.
     * @throws InterruptedException  is thrown if the thread was externally interrupted.
     * @deprecated please use {@code getUnitRotationGlobalQuat4d()} instead.
     */
    @Deprecated
    default Quat4d getGlobalRotationQuat4d() throws NotAvailableException, InterruptedException {
        return getUnitRotationGlobalQuat4d();
    }

    /**
     * Gets the rotation of the unit relative to the root location as a Rotation object.
     *
     * @return rotation relative to the root location
     *
     * @throws NotAvailableException is thrown if the transformation is not available.
     * @throws InterruptedException  is thrown if the thread was externally interrupted.
     * @deprecated please use {@code getUnitRotationGlobal()} instead.
     */
    @Deprecated
    default Rotation getGlobalRotation() throws NotAvailableException, InterruptedException {
        return getUnitRotationGlobal();
    }

    /**
     * Gets the center coordinates of the unit's BoundingBox in the unit coordinate system as a Point3d object.
     *
     * @return center coordinates of the unit's BoundingBox relative to unit
     *
     * @throws NotAvailableException is thrown if the center can not be calculate.
     * @deprecated please use {@code getUnitBoundingBoxCenterPoint3d()} instead.
     */
    @Deprecated
    default Point3d getLocalBoundingBoxCenterPoint3d() throws NotAvailableException {
        return getUnitBoundingBoxCenterPoint3d();
    }

    /**
     * Gets the center coordinates of the unit's BoundingBox in the coordinate system of the root location as a Point3d object.
     *
     * @return center coordinates of the unit's BoundingBox relative to root location
     *
     * @throws NotAvailableException          is thrown if the center can not be calculate.
     * @throws java.lang.InterruptedException
     * @deprecated please use {@code getUnitBoundingBoxCenterGlobalPoint3d()} instead.
     */
    @Deprecated
    default Point3d getGlobalBoundingBoxCenterPoint3d() throws NotAvailableException, InterruptedException {
        return getUnitBoundingBoxCenterGlobalPoint3d();
    }

    /**
     * Method returns the transformation leading from the root location to this unit.
     *
     * @return a transformation future
     */
    default Future<Transform> getRootToUnitTransformationFuture() {
        try {
            return Registries.getUnitRegistry().getRootToUnitTransformationFuture(getConfig());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(new NotAvailableException("UnitTransformation", ex));
        }
    }

    /**
     * Method returns the transformation leading from the unit to the root location.
     *
     * @return a transformation future
     */
    default Future<Transform> getUnitToRootTransformationFuture() {
        try {
            return Registries.getUnitRegistry().getUnitToRootTransformationFuture(getConfig());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(new NotAvailableException("UnitTransformation", ex));
        }
    }

    /**
     * Method returns the transformation leading from the root location to this unit.
     *
     * @return the transformation
     *
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    default Transform getRootToUnitTransformation() throws NotAvailableException {
        try {
            return Registries.getUnitRegistry().getRootToUnitTransformation(getConfig());
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("UnitTransformation", ex);
        }
    }

    /**
     * Method returns the transformation leading from the unit to the root location.
     *
     * @return the transformation
     *
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    default Transform getUnitToRootTransformation() throws NotAvailableException {
        try {
            return Registries.getUnitRegistry().getUnitToRootTransformation(getConfig());
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("UnitTransformation", ex);
        }
    }

    /**
     * Gets the Transform3D of the transformation from root to unit coordinate system.
     *
     * @return transform relative to root location
     *
     * @throws NotAvailableException is thrown if the transformation is not available.
     */
    default Transform3D getRootToUnitTransform3D() throws NotAvailableException {
        try {
            return Registries.getUnitRegistry().getRootToUnitTransform3D(getConfig());
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
     *
     * @throws NotAvailableException is thrown if the transformation is not available.
     */
    default Transform3D getUnitToRootTransform3D() throws NotAvailableException {
        try {
            return Registries.getUnitRegistry().getUnitToRootTransform3D(getConfig());
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("Transform3Dinverse", ex);
        }
    }

    /**
     * Gets the position of the unit relative to the root location as a Point3d object.
     *
     * @return position relative to the root location
     *
     * @throws NotAvailableException is thrown if the transformation is not available.
     */
    default Point3d getUnitPositionGlobalPoint3d() throws NotAvailableException {
        try {
            return Registries.getUnitRegistry().getUnitPositionGlobalPoint3d(getConfig());
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("GlobalPositionVector", ex);
        }
    }

    /**
     * Gets the position of the unit relative to the root location as a Translation object.
     *
     * @return position relative to the root location
     *
     * @throws NotAvailableException is thrown if the transformation is not available.
     */
    default Translation getUnitPositionGlobal() throws NotAvailableException {
        try {
            return Registries.getUnitRegistry().getUnitPositionGlobal(getConfig());
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("GlobalPosition", ex);
        }
    }

    /**
     * Gets the rotation of the unit relative to the root location as a Quat4d object.
     *
     * @return rotation relative to the root location
     *
     * @throws NotAvailableException is thrown if the transformation is not available.
     */
    default Quat4d getUnitRotationGlobalQuat4d() throws NotAvailableException {
        try {
            return Registries.getUnitRegistry().getUnitRotationGlobalQuat4d(getConfig());
        } catch (final NotAvailableException ex) {
            throw new NotAvailableException("GlobalRotationQuat", ex);
        }
    }

    /**
     * Gets the rotation of the unit relative to the root location as a Rotation object.
     *
     * @return rotation relative to the root location
     *
     * @throws NotAvailableException is thrown if the transformation is not available.
     */
    default Rotation getUnitRotationGlobal() throws NotAvailableException {
        try {
            return Registries.getUnitRegistry().getUnitRotationGlobal(getConfig());
        } catch (final NotAvailableException ex) {
            throw new NotAvailableException("GlobalRotation", ex);
        }
    }

    /**
     * Gets the center coordinates of the unit's BoundingBox in the unit coordinate system as a Point3d object.
     *
     * @return center coordinates of the unit's BoundingBox relative to unit
     *
     * @throws NotAvailableException is thrown if the center can not be calculate.
     */
    default Point3d getUnitBoundingBoxCenterPoint3d() throws NotAvailableException {
        return Registries.getUnitRegistry().getUnitBoundingBoxCenterPoint3d(getConfig());
    }

    /**
     * Gets the center coordinates of this unit's BoundingBox in the coordinate system of the root location as a Point3d object.
     *
     * @return center coordinates of this unit's BoundingBox relative to root location
     *
     * @throws NotAvailableException is thrown if the center can not be calculate.
     */
    default Point3d getUnitBoundingBoxCenterGlobalPoint3d() throws NotAvailableException {
        try {
            return Registries.getUnitRegistry().getUnitBoundingBoxCenterGlobalPoint3d(getConfig());
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("GlobalBoundingBoxCenter", ex);
        }
    }

    /**
     * Add an observer which is only notified if the value for the current
     * value of the given service type changes.
     *
     * @param serviceType The service type on which the observer is added.
     * @param observer    The observer which is added.
     */
    @Override
    default void addServiceStateObserver(final ServiceType serviceType, final Observer observer) {
        addServiceStateObserver(ServiceTempus.CURRENT, serviceType, observer);
    }

    /**
     * Remove an observer which is only notified if the value for the current
     * value of the given service type changes.
     *
     * @param serviceType The service type on which the observer is removed.
     * @param observer    The observer which is removed.
     */
    @Override
    default void removeServiceStateObserver(final ServiceType serviceType, final Observer observer) {
        removeServiceStateObserver(ServiceTempus.CURRENT, serviceType, observer);
    }

    /**
     * Add an observer which is only notified if the desired service type for
     * the desired service tempus changes.
     * The service data notified can be empty. If you want to filter these updates
     * you can use the ServiceStateObserver. Empty updates are left in because when
     * observing requested states this can indicate that it is taken over as the current state.
     *
     * @param serviceTempus The service tempus on which the observer is added.
     * @param serviceType   The service type on which the observer is added.
     * @param observer      The observer which is added.
     */
    void addServiceStateObserver(final ServiceTempus serviceTempus, final ServiceType serviceType, final Observer observer);

    /**
     * Remove an observer which is only notified if the desired service type for
     * the desired service tempus changes.
     *
     * @param serviceTempus The service tempus on which the observer is removed.
     * @param serviceType   The service type on which the observer is removed.
     * @param observer      The observer which is removed.
     */
    void removeServiceStateObserver(final ServiceTempus serviceTempus, final ServiceType serviceType, final Observer observer);

    /**
     * Add a data observer which is only notified if data for the given
     * service tempus changes.
     * The value unknown is equivalent to listening on all changes.
     *
     * @param serviceTempus The service tempus on which the observer is added.
     * @param observer      The observer which is added.
     */
    void addDataObserver(ServiceTempus serviceTempus, final Observer<D> observer);

    /**
     * Remove a data observer which is only notified if data for the given
     * service tempus changes.
     * The value unknown is equivalent to listening on all changes.
     *
     * @param serviceTempus The service tempus on which the observer is removed.
     * @param observer      The observer which is removed.
     */
    void removeDataObserver(ServiceTempus serviceTempus, final Observer<D> observer);

    /**
     * Returns true if this unit is a dal unit.
     *
     * @return is true if this unit is a dal unit.
     *
     * @throws CouldNotPerformException is throw if the check could not be performed.
     */
    default boolean isDalUnit() throws CouldNotPerformException {
        return UnitConfigProcessor.isDalUnit(getUnitType());
    }

    /**
     * Returns true if this unit is a base unit.
     *
     * @return is true if this unit is a base unit.
     *
     * @throws CouldNotPerformException is throw if the check could not be performed.
     */
    default boolean isBaseUnit() throws CouldNotPerformException {
        return UnitConfigProcessor.isDalUnit(getUnitType());
    }

    /**
     * Returns the variable provider of this unit like {@code generateVariablePool()}. Additionally this provider also contains variables related to the given service of this unit.
     * For this it's needed that this unit supports the given service. Additional to the {@code generateVariablePool()} this method further provides:
     * * BindingServiceConfig (if available)
     * * ServiceMetaConfig (if available)
     * * ServiceConfig (protobuf fields)
     *
     * @return a key - value pair pool providing all related variable of this unit including the service variables.
     *
     * @throws NotAvailableException is thrown if the variable pool is not available e.g. because the unit is not compatible with the given service type..
     */
    default VariableProvider generateVariablePool(final ServiceType serviceType) throws NotAvailableException {
        for (ServiceConfig serviceConfig : getConfig().getServiceConfigList()) {
            if (serviceConfig.getServiceDescription().getServiceType() == serviceType) {
                return generateVariablePool(serviceConfig);
            }
        }
        throw new NotAvailableException("VariableProvider", new InvalidStateException("ServiceType[" + serviceType.name() + "] not supported by " + this));
    }

    /**
     * Returns the variable provider of this unit like {@code generateVariablePool()}. Additionally this provider also contains variables related to the given service of this unit.
     * For this it's needed that this unit supports the given service. Additional to the {@code generateVariablePool()} this method further provides:
     * * BindingServiceConfig (if available)
     * * ServiceMetaConfig (if available)
     * * ServiceConfig (protobuf fields)
     *
     * @return a key - value pair pool providing all related variable of this unit including the service variables.
     *
     * @throws NotAvailableException is thrown if the variable pool is not available.
     */
    default VariableProvider generateVariablePool(final ServiceConfig serviceConfig) throws NotAvailableException {
        final MetaConfigPool configPool = (MetaConfigPool) generateVariablePool();
        if (serviceConfig.hasBindingConfig()) {
            configPool.register(new MetaConfigVariableProvider("BindingServiceConfig", serviceConfig.getBindingConfig().getMetaConfig()));
        }
        configPool.register(new MetaConfigVariableProvider("ServiceMetaConfig", serviceConfig.getMetaConfig()));
        configPool.register(new ProtobufVariableProvider(serviceConfig));
        return configPool;
    }

    /**
     * Method return the host unit config of this config if available.
     * Mainly dal units are providing host units. A host unit (device/app) is the unit which introduces dal units (light, motiondetector) to the system.
     * Example: A light can be introduced by a physical device like a Philip Hue. In this case the Philip Hue Device config is returned as host unit config.
     *
     * @return the app or device config of this dal unit.
     *
     * @throws NotAvailableException is thrown if this unit does not provide a host unit or something else went wrong during resolution.
     */
    default UnitConfig getHostUnitConfig() throws NotAvailableException {
        try {
            return Registries.getUnitRegistry().getUnitConfigById(getConfig().getUnitHostId());
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("HostUnitConfig", ex);
        }
    }

    /**
     * Returns the variable provider of this unit. It contains variables of all meta configs related to this unit including:
     * * HostConfigMetaConfig (if available)
     * * DeviceBindingConfig (if available)
     * * DeviceClassMetaConfig (if available)
     * * AppBindingConfig  (if available)
     * * AppClassMetaConfig (if available)
     * * UnitLocationMetaConfig (if available)
     * * UnitMetaConfig
     * * LocationUnitConfig (protobuf fields)
     * *
     *
     * @return a key - value pair pool providing all related variable of this unit.
     *
     * @throws NotAvailableException is thrown if the variable pool is not available.
     */
    default VariableProvider generateVariablePool() throws NotAvailableException {
        try {
            final UnitConfig unitConfig = getConfig();
            final MetaConfigPool configPool = new MetaConfigPool();

            // resolve host unit meta configs if this unit is a dal unit.
            if (isDalUnit()) {

                // HostConfigMetaConfig
                UnitConfig hostUnitConfig = getHostUnitConfig();
                configPool.register(new MetaConfigVariableProvider("HostConfigMetaConfig", hostUnitConfig.getMetaConfig()));
                configPool.register(new ProtobufVariableProvider(hostUnitConfig));
                switch (hostUnitConfig.getUnitType()) {
                    case DEVICE:
                        final DeviceClass deviceClass = Registries.getClassRegistry().getDeviceClassById(hostUnitConfig.getDeviceConfig().getDeviceClassId());
                        configPool.register(new MetaConfigVariableProvider("DeviceBindingConfig", deviceClass.getBindingConfig().getMetaConfig()));
                        configPool.register(new MetaConfigVariableProvider("DeviceClassMetaConfig", deviceClass.getMetaConfig()));
                        configPool.register(new ProtobufVariableProvider(deviceClass));
                        break;
                    case APP:
                        final AppClass appClass = Registries.getClassRegistry().getAppClassById(hostUnitConfig.getAppConfig().getAppClassId());
                        configPool.register(new MetaConfigVariableProvider("AppBindingConfig", appClass.getBindingConfig().getMetaConfig()));
                        configPool.register(new MetaConfigVariableProvider("AppClassMetaConfig", appClass.getMetaConfig()));
                        configPool.register(new ProtobufVariableProvider(appClass));
                        break;
                }
            }

            // register location meta config if available
            try {
                UnitConfig locationUnitConfig = Registries.getUnitRegistry(true).getUnitConfigById(unitConfig.getPlacementConfig().getLocationId());
                configPool.register(new MetaConfigVariableProvider("UnitLocationMetaConfig", locationUnitConfig.getMetaConfig()));
                configPool.register(new ProtobufVariableProvider(locationUnitConfig));
            } catch (NullPointerException | CouldNotPerformException | InterruptedException ex) {
                // location not available so skip those
            }

            configPool.register(new MetaConfigVariableProvider("UnitMetaConfig", unitConfig.getMetaConfig()));
            configPool.register(new ProtobufVariableProvider(unitConfig));
            return configPool;
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("Variable Provider not available!", ex);
        }
    }

    default ServiceProvider getServiceProvider() {
        return this;
    }
}
