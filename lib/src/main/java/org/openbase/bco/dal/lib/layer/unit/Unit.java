package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.dal.lib.layer.service.*;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ProtobufVariableProvider;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.type.iface.ScopeProvider;
import org.openbase.jul.extension.type.iface.TransactionIdProvider;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.iface.Configurable;
import org.openbase.jul.iface.Identifiable;
import org.openbase.jul.iface.provider.LabelProvider;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.processing.VariableProvider;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.rct.Transform;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.database.QueryType;
import org.openbase.type.domotic.database.RecordType;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.AggregatedServiceStateType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.app.AppClassType.AppClass;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.geometry.RotationType;
import org.openbase.type.geometry.RotationType.Rotation;
import org.openbase.type.geometry.TranslationType;
import org.openbase.type.geometry.TranslationType.Translation;
import org.openbase.type.spatial.ShapeType.Shape;
import org.slf4j.LoggerFactory;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * @param <D> the data type of this unit used for the state synchronization.
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface Unit<D extends Message> extends LabelProvider, ScopeProvider, Identifiable<String>, Configurable<String, UnitConfig>, DataProvider<D>, ServiceProvider, Service, AuthenticatedSnapshotable, TransactionIdProvider {

    /**
     * Returns the type of this unit.
     *
     * @return UnitType the unit type defining which unit template is provided by this unit.
     * @throws NotAvailableException is thrown if the unit type is currently not available.
     */
    UnitType getUnitType() throws NotAvailableException;

    /**
     * Returns the related template for this unit.
     * <p>
     * Note: The unit template defines which services are provided by this unit.
     *
     * @return UnitTemplate the unit template of this unit.
     * @throws NotAvailableException in case the unit template is not available.
     */
    UnitTemplate getUnitTemplate() throws NotAvailableException;

    /**
     * Method returns the unit template of this unit containing all service templates of available units.
     * <p>
     * Note: The amount of supported and available services only varies for {@code MultiUnits} (e.g. {@code Location}, {@code UnitGroup}).
     *
     * @param onlyAvailableServices if the filter flag is set to true, only service templates are included which are available for the current instance.
     * @return the {@code UnitTemplate} of this unit.
     * @throws NotAvailableException is thrown if the {@code UnitTemplate} is currently not available.
     */
    default UnitTemplate getUnitTemplate(final boolean onlyAvailableServices) throws NotAvailableException {
        // return the unfiltered unit template. Only MultiUnits must overwrite this method.
        return getUnitTemplate();
    }

    /**
     * Method returns the unit shape of this unit.
     * <p>
     * If this unit configuration does not provide any shape information the shape of the unit host will be returned.
     * In case the unit host even does not provide any shape information and the unit is a device than the shape of the device class will be used.
     *
     * @return the shape representing the unit.
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
     * Method returns a list of all supported services provided by this unit.
     * "Supported" in this context means that the unit can handle those services.
     * In most cases this means the supported services are also available and fully provided through the unit.
     * Only MultiUnits of the type Location and UnitGroup support mostly more units than actually available.
     * In this case you can use the method getAvailableServiceTypes() to check which services are aggregated by the unit.
     *
     * @return a set of supported service types.
     * @throws NotAvailableException
     */
    default Set<ServiceType> getSupportedServiceTypes() throws NotAvailableException {
        final Set<ServiceType> serviceTypeSet = new HashSet<>();
        try {
            for (final ServiceConfig serviceConfig : getConfig().getServiceConfigList()) {
                serviceTypeSet.add(serviceConfig.getServiceDescription().getServiceType());
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("SupportedServiceTypes", new CouldNotPerformException("Could not generate supported service type list!", ex));
        }
        return serviceTypeSet;
    }

    /**
     * Method returns a set of all currently available service types of this unit instance.
     *
     * @return a set of {@code ServiceTypes}.
     * @throws NotAvailableException is thrown if the service types can not be detected.
     */
    default Set<ServiceType> getAvailableServiceTypes() throws NotAvailableException {
        final Set<ServiceType> serviceTypeList = new HashSet<>();
        for (final ServiceDescription serviceDescription : getUnitTemplate(true).getServiceDescriptionList()) {
            serviceTypeList.add(serviceDescription.getServiceType());
        }
        return serviceTypeList;
    }

    /**
     * Method returns a set of all currently available service descriptions of this unit instance.
     *
     * @return a set of {@code ServiceDescription}.
     * @throws NotAvailableException is thrown if the service types can not be detected.
     */
    default Set<ServiceDescription> getAvailableServiceDescriptions() throws NotAvailableException {
        final Set<ServiceDescription> serviceDescriptionList = new HashSet<>();
        serviceDescriptionList.addAll(getUnitTemplate(true).getServiceDescriptionList());
        return serviceDescriptionList;
    }

    @RPCMethod(legacy = true)
    @Override
    default Future<Snapshot> recordSnapshot() {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                MultiException.ExceptionStack exceptionStack = null;
                Snapshot.Builder snapshotBuilder = Snapshot.newBuilder();
                for (final ServiceType serviceType : getRepresentingOperationServiceTypes()) {
                    try {
                        ServiceStateDescription.Builder serviceStateDescription = ServiceStateDescription.newBuilder().setServiceType(serviceType).setUnitId(getId());

                        // load operation service attribute by related provider service
                        Message serviceState = (Message) Services.invokeServiceMethod(serviceType, ServiceTemplate.ServicePattern.PROVIDER, this);

                        // verify operation service state (e.g. ignore UNKNOWN service states)
                        try {
                            Services.verifyAndRevalidateServiceState(serviceState);
                        } catch (VerificationFailedException ex) {
                            // skip invalid or not available services.
                            continue;
                        }

                        // fill action config
                        final ServiceJSonProcessor serviceJSonProcessor = new ServiceJSonProcessor();
                        try {
                            serviceStateDescription.setServiceState(serviceJSonProcessor.serialize(serviceState));
                        } catch (InvalidStateException ex) {
                            // skip if serviceState is empty.
                            continue;
                        }
                        serviceStateDescription.setUnitId(getId());
                        serviceStateDescription.setUnitType(getUnitTemplate().getUnitType());
                        serviceStateDescription.setServiceType(serviceType);
                        serviceStateDescription.setServiceStateClassName(serviceJSonProcessor.getServiceStateClassName(serviceState));

                        // add action config
                        snapshotBuilder.addServiceStateDescription(serviceStateDescription.build());
                    } catch (CouldNotPerformException | ClassCastException ex) {
                        exceptionStack = MultiException.push(this, ex, exceptionStack);
                    }
                }

                try {
                    MultiException.checkAndThrow(() -> "Could not snapshot all service provider!", exceptionStack);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(ex, LoggerFactory.getLogger(Unit.class), LogLevel.WARN);
                }

                return snapshotBuilder.build();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not record snapshot!", ex);
            }
        });
    }

    @RPCMethod(legacy = true)
    @Override
    Future<Void> restoreSnapshot(final Snapshot snapshot);

    /**
     * Return a list of operation services representing this unit. The default implementation returns all available
     * operation services and prints a warning if there are more than one. The returned services are used to record
     * snapshots which should not contain all services because they can interact e.g. a colorable light is best
     * represented by its color state and not by its power state. If both are returned, restoring snapshots leads
     * to rejecting one of the actions.
     *
     * @return a list of operations service types best representing the unit.
     * @throws NotAvailableException if the unit template is not available.
     */
    default List<ServiceType> getRepresentingOperationServiceTypes() throws NotAvailableException {
        final List<ServiceType> serviceTypeList = new ArrayList<>();
        for (final ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {
            if (serviceDescription.getPattern() != ServicePattern.OPERATION) {
                continue;
            }

            serviceTypeList.add(serviceDescription.getServiceType());
        }

        if (serviceTypeList.size() > 1) {
            LoggerFactory.getLogger(Unit.class).warn("Unit {} has more than one operation service which should be handled by a specialized implementation!", this);
        }
        return serviceTypeList;
    }

    /**
     * Gets the local position of the unit relative to its parent location.
     *
     * @return relative position
     * @throws NotAvailableException is thrown if the unit config or parts of it are not available.
     */
    default TranslationType.Translation getUnitPosition() throws NotAvailableException {
        try {
            if (!getConfig().hasPlacementConfig()) {
                throw new NotAvailableException("PlacementConfig");
            }
            if (!getConfig().getPlacementConfig().hasPose()) {
                throw new NotAvailableException("Pose");
            }
            return getConfig().getPlacementConfig().getPose().getTranslation();
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
    default RotationType.Rotation getUnitRotation() throws NotAvailableException {
        try {
            if (!getConfig().hasPlacementConfig()) {
                throw new NotAvailableException("PlacementConfig");
            }
            if (!getConfig().getPlacementConfig().hasPose()) {
                throw new NotAvailableException("Position");
            }
            return getConfig().getPlacementConfig().getPose().getRotation();
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("UnitRotation", ex);
        }
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
     * @throws NotAvailableException is thrown if the center can not be calculate.
     */
    default Point3d getUnitBoundingBoxCenterPoint3d() throws NotAvailableException {
        return Registries.getUnitRegistry().getUnitBoundingBoxCenterPoint3d(getConfig());
    }

    /**
     * Gets the center coordinates of this unit's BoundingBox in the coordinate system of the root location as a Point3d object.
     *
     * @return center coordinates of this unit's BoundingBox relative to root location
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
    void addServiceStateObserver(final ServiceTempus serviceTempus, final ServiceType serviceType, final Observer<ServiceStateProvider<Message>, Message> observer);

    /**
     * Remove an observer which is only notified if the desired service type for
     * the desired service tempus changes.
     *
     * @param serviceTempus The service tempus on which the observer is removed.
     * @param serviceType   The service type on which the observer is removed.
     * @param observer      The observer which is removed.
     */
    void removeServiceStateObserver(final ServiceTempus serviceTempus, final ServiceType serviceType, final Observer<ServiceStateProvider<Message>, Message> observer);

    /**
     * Add a data observer which is only notified if data for the given
     * service tempus changes.
     * The value unknown is equivalent to listening on all changes.
     *
     * @param serviceTempus The service tempus on which the observer is added.
     * @param observer      The observer which is added.
     */
    void addDataObserver(ServiceTempus serviceTempus, final Observer<DataProvider<D>, D> observer);

    /**
     * Remove a data observer which is only notified if data for the given
     * service tempus changes.
     * The value unknown is equivalent to listening on all changes.
     *
     * @param serviceTempus The service tempus on which the observer is removed.
     * @param observer      The observer which is removed.
     */
    void removeDataObserver(ServiceTempus serviceTempus, final Observer<DataProvider<D>, D> observer);

    /**
     * Returns true if this unit is a dal unit.
     *
     * @return is true if this unit is a dal unit.
     * @throws CouldNotPerformException is throw if the check could not be performed.
     */
    default boolean isDalUnit() throws CouldNotPerformException {
        return UnitConfigProcessor.isDalUnit(getUnitType());
    }

    /**
     * Returns true if this unit is a base unit.
     *
     * @return is true if this unit is a base unit.
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
     * @throws NotAvailableException is thrown if this unit does not provide a host unit or something else went wrong during resolution.
     */
    default UnitConfig getHostUnitConfig() throws NotAvailableException {
        try {
            final UnitConfig config = getConfig();
            if (!UnitConfigProcessor.isHostUnitAvailable(config)) {
                throw new InvalidStateException("Unit itself might me a host unit and therefore do not link to any host unit.");
            }
            return Registries.getUnitRegistry().getUnitConfigById(config.getUnitHostId());
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
     * @throws NotAvailableException is thrown if the variable pool is not available.
     */
    default VariableProvider generateVariablePool() throws NotAvailableException {
        try {
            final UnitConfig unitConfig = getConfig();
            final MetaConfigPool configPool = new MetaConfigPool();

            configPool.register(new MetaConfigVariableProvider("UnitMetaConfig", unitConfig.getMetaConfig()));
            configPool.register(new ProtobufVariableProvider(unitConfig));

            // register location meta config if available
            try {
                UnitConfig locationUnitConfig = Registries.getUnitRegistry(true).getUnitConfigById(unitConfig.getPlacementConfig().getLocationId());
                configPool.register(new MetaConfigVariableProvider("UnitLocationMetaConfig", locationUnitConfig.getMetaConfig()));
                configPool.register(new ProtobufVariableProvider(locationUnitConfig));
            } catch (NullPointerException | CouldNotPerformException | InterruptedException ex) {
                // location not available so skip those
            }

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
            return configPool;
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("Variable Provider not available!", ex);
        }
    }

    /**
     * This method returns the parent location config of this unit.
     * If this unit is a location, than its parent location config is returned,
     * otherwise the parent location config is returned which refers the location where this unit is placed in.
     *
     * @return a unit config of the parent location.
     * @throws NotAvailableException is thrown if the location config is currently not available.
     */
    default UnitConfig getParentLocationConfig() throws NotAvailableException {
        try {
            return Registries.getUnitRegistry().getUnitConfigById(getConfig().getPlacementConfig().getLocationId(), UnitType.LOCATION);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("LocationConfig", ex);
        }
    }

    /**
     * This method cancels the given action on remote controller.
     *
     * @param actionDescription the description which identifies the action.
     * @return a future object representing the success of the cancellation.
     */
    Future<ActionDescription> cancelAction(final ActionDescription actionDescription);

    /**
     * This method extends the given action on remote controller.
     *
     * @param actionDescription the action to extend.
     * @return a future of the extension request.
     */
    Future<ActionDescription> extendAction(final ActionDescription actionDescription);

    default ServiceProvider getServiceProvider() {
        return this;
    }

    default List<ActionDescription> getActionList() throws NotAvailableException {
        return ProtoBufFieldProcessor.getRepeatedFieldList(Action.TYPE_FIELD_NAME_ACTION, getData());
    }

    @RPCMethod
    Future<AggregatedServiceStateType.AggregatedServiceState> queryAggregatedServiceState(final QueryType.Query databaseQuery);

    @RPCMethod
    Future<RecordType.Record> queryRecord(final QueryType.Query databaseQuery);
}
