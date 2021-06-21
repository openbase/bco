package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.iface.Configurable;
import org.openbase.jul.iface.Identifiable;
import org.openbase.jul.iface.provider.LabelProvider;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.processing.VariableProvider;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.rct.Transform;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionReferenceType.ActionReference;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.authentication.AuthenticatedValueType;
import org.openbase.type.domotic.database.QueryType;
import org.openbase.type.domotic.database.RecordCollectionType;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.AggregatedServiceStateType.AggregatedServiceState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.app.AppClassType.AppClass;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass;
import org.openbase.type.geometry.RotationType;
import org.openbase.type.geometry.RotationType.Rotation;
import org.openbase.type.geometry.TranslationType;
import org.openbase.type.geometry.TranslationType.Translation;
import org.openbase.type.spatial.ShapeType.Shape;
import org.slf4j.LoggerFactory;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import java.util.*;
import java.util.concurrent.Future;

/**
 * @param <D> the data type of this unit used for the state synchronization.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface Unit<D extends Message> extends LabelProvider, ScopeProvider, Identifiable<String>, Configurable<String, UnitConfig>, DataProvider<D>, ServiceProvider<Message>, Service, AuthenticatedSnapshotable, TransactionIdProvider {

    String META_CONFIG_UNIT_INFRASTRUCTURE_FLAG = "INFRASTRUCTURE";

    /**
     * Returns the type of this unit.
     *
     * @return UnitType the unit type defining which unit template is provided by this unit.
     *
     * @throws NotAvailableException is thrown if the unit type is currently not available.
     */
    UnitType getUnitType() throws NotAvailableException;

    /**
     * Method returns the unit template which refers to the most compatible unit template supported by this instance.
     * <p>
     * This needs not to be equal with the actual unit template of the connected unit because result because it can be a supertype.
     * For example a {@code LightRemote} can be connected to a {@code ColorableLight} unit.
     * In this case the {@code template} of the unit is actually of the type {@code COLORABLE_LIGHT_UNIT}. When calling {@code getUnitTemplate()} on remote site the method will refer to the {@code LIGHT_UNIT} template.
     *
     * @return the unit template..
     *
     * @throws NotAvailableException is thrown when the template is not available, for instance when not yet synchronized with the bco registry.
     */
    UnitTemplate getUnitTemplate() throws NotAvailableException;

    /**
     * Method returns the unit template of this unit containing all service templates of available units.
     * <p>
     * Note: The amount of supported and available services only varies for {@code MultiUnits} (e.g. {@code Location}, {@code UnitGroup}).
     *
     * @param onlyAvailableServices if the filter flag is set to true, only service templates are included which are available for the current instance.
     *
     * @return the {@code UnitTemplate} of this unit.
     *
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
     *
     * @throws NotAvailableException is thrown if the unit shape is not available or the resolution has been failed.
     */
    default Shape getUnitShape() throws NotAvailableException {
        try {
            return Registries.getUnitRegistry().getUnitShapeByUnitConfig(getConfig());
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
     *
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
     *
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
     *
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
                        serviceStateDescription.setServiceStateClassName(Services.getServiceStateClassName(serviceState));

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

    /**
     * @param snapshot
     *
     * @return
     *
     * @deprecated Deprecated because not yet fully compatible with unit allocation.
     */
    @RPCMethod(legacy = true)
    @Override
    @Deprecated
    Future<Void> restoreSnapshot(final Snapshot snapshot);
    // todo: method should return a ActionDescription including its impact.

    /**
     * Return a list of operation services representing this unit. The default implementation returns all available
     * operation services and prints a warning if there are more than one. The returned services are used to record
     * snapshots which should not contain all services because they can interact e.g. a colorable light is best
     * represented by its color state and not by its power state. If both are returned, restoring snapshots leads
     * to rejecting one of the actions.
     *
     * @return a list of operations service types best representing the unit.
     *
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
     *
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
     *
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
    default Future<Transform> getRootToUnitTransformation() {
        try {
            return Registries.getUnitRegistry().getRootToUnitTransformation(getConfig());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(Transform.class, new NotAvailableException("UnitTransformation", ex));
        }
    }

    /**
     * Method returns the transformation leading from the unit to the root location.
     *
     * @return a transformation future
     */
    default Future<Transform> getUnitToRootTransformation() {
        try {
            return Registries.getUnitRegistry().getUnitToRootTransformation(getConfig());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(Transform.class, new NotAvailableException("UnitTransformation", ex));
        }
    }

    /**
     * Gets the Transform3D of the transformation from root to unit coordinate system.
     *
     * @return transform relative to root location
     */
    default Future<Transform3D> getRootToUnitTransform3D() {
        try {
            return Registries.getUnitRegistry().getRootToUnitTransform3D(getConfig());
        } catch (final CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(Transform3D.class, new NotAvailableException("Transform3D", ex));
        }
    }

    /**
     * Gets the transformation leading from the unit to the root location.
     * This is basically rotation and translation of the object in the root coordinate system
     * and thereby the inverse transformation to the one returned by getTransform3D().
     *
     * @return transform relative to root location
     */
    default Future<Transform3D> getUnitToRootTransform3D() {
        try {
            return Registries.getUnitRegistry().getUnitToRootTransform3D(getConfig());
        } catch (NotAvailableException ex) {
            return FutureProcessor.canceledFuture(Transform3D.class, new NotAvailableException("Transform3Dinverse", ex));
        }
    }

    /**
     * Gets the position of the unit relative to the root location as a Point3d object.
     *
     * @return position relative to the root location
     */
    default Future<Point3d> getUnitPositionGlobalPoint3d() {
        try {
            return Registries.getUnitRegistry().getUnitPositionGlobalPoint3d(getConfig());
        } catch (NotAvailableException ex) {
            return FutureProcessor.canceledFuture(Point3d.class, new NotAvailableException("GlobalPositionVector", ex));
        }
    }

    /**
     * Gets the position of the unit relative to the root location as a Translation object.
     *
     * @return position relative to the root location
     */
    default Future<Translation> getUnitPositionGlobal() {
        try {
            return Registries.getUnitRegistry().getUnitPositionGlobal(getConfig());
        } catch (NotAvailableException ex) {
            return FutureProcessor.canceledFuture(Translation.class, new NotAvailableException("GlobalPosition", ex));
        }
    }

    /**
     * Gets the rotation of the unit relative to the root location as a Quat4d object.
     *
     * @return rotation relative to the root location
     */
    default Future<Quat4d> getUnitRotationGlobalQuat4d() {
        try {
            return Registries.getUnitRegistry().getUnitRotationGlobalQuat4d(getConfig());
        } catch (final NotAvailableException ex) {
            return FutureProcessor.canceledFuture(Quat4d.class, new NotAvailableException("GlobalRotationQuat", ex));
        }
    }

    /**
     * Gets the rotation of the unit relative to the root location as a Rotation object.
     *
     * @return rotation relative to the root location
     */
    default Future<Rotation> getUnitRotationGlobal() {
        try {
            return Registries.getUnitRegistry().getUnitRotationGlobal(getConfig());
        } catch (final NotAvailableException ex) {
            return FutureProcessor.canceledFuture(Rotation.class, new NotAvailableException("GlobalRotation", ex));
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
     */
    default Future<Point3d> getUnitBoundingBoxCenterGlobalPoint3d() {
        try {
            return Registries.getUnitRegistry().getUnitBoundingBoxCenterGlobalPoint3d(getConfig());
        } catch (NotAvailableException ex) {
            return FutureProcessor.canceledFuture(Point3d.class, new NotAvailableException("GlobalBoundingBoxCenter", ex));
        }
    }

    /**
     * Add an observer which is only notified if the value for the current
     * value of the given service type changes.
     *
     * @param serviceType The service type on which the observer is added.
     * @param observer    The observer which is added.
     *
     * @throws CouldNotPerformException method throws an InvalidStateException if the requested service type is not supported by this unit.
     */
    @Override
    default void addServiceStateObserver(final ServiceType serviceType, final Observer observer) throws CouldNotPerformException {
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
     *
     * @throws CouldNotPerformException method throws an InvalidStateException if the requested service type is not supported by this unit.
     */
    void addServiceStateObserver(final ServiceTempus serviceTempus, final ServiceType serviceType, final Observer<ServiceStateProvider<Message>, Message> observer) throws CouldNotPerformException;

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
     * <p>
     * Note: Use Tempus.UNKNOWN to get informed about any action state changes.
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
     * @throws NotAvailableException is thrown if the variable pool is not available e.g. because the unit is not compatible with the given service type.
     * @throws InterruptedException  is thrown if the thread is externally interrupted.
     */
    default VariableProvider generateVariablePool(final ServiceType serviceType) throws NotAvailableException, InterruptedException {
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
     * @throws InterruptedException  is thrown if the thread is externally interrupted.
     */
    default VariableProvider generateVariablePool(final ServiceConfig serviceConfig) throws NotAvailableException, InterruptedException {
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
     *
     * @return a key - value pair pool providing all related variable of this unit.
     *
     * @throws NotAvailableException is thrown if the variable pool is not available.
     * @throws InterruptedException  is thrown in case the thread was externally interrupted.
     */
    default VariableProvider generateVariablePool() throws NotAvailableException, InterruptedException {
        return registerVariables(getConfig(), new MetaConfigPool());
    }

    default MetaConfigPool registerVariables(final UnitConfig unitConfig, final MetaConfigPool variablePool) throws NotAvailableException, InterruptedException {
        try {
            final String key = LabelProcessor.getBestMatch(Locale.ENGLISH, unitConfig.getLabel(), getId()) + StringProcessor.transformUpperCaseToPascalCase(unitConfig.getUnitType().name());

            // direct meta config
            variablePool.register(new MetaConfigVariableProvider(key + "MetaConfig", unitConfig.getMetaConfig()));
            variablePool.register(new ProtobufVariableProvider(unitConfig));

            // register location meta config if available
            try {
                UnitConfig locationUnitConfig = Registries.getUnitRegistry(true).getUnitConfigById(unitConfig.getPlacementConfig().getLocationId());
                variablePool.register(new MetaConfigVariableProvider(key + "PlacementMetaConfig", locationUnitConfig.getMetaConfig()));
                variablePool.register(new ProtobufVariableProvider(locationUnitConfig));
            } catch (InterruptedException ex) {
                throw ex;
            } catch (NullPointerException | CouldNotPerformException ex) {
                // location not available so skip it
            }

            // type specific variables
            switch (unitConfig.getUnitType()) {
                case DEVICE:
                    final DeviceClass deviceClass = Registries.getClassRegistry().getDeviceClassById(unitConfig.getDeviceConfig().getDeviceClassId());
                    variablePool.register(new MetaConfigVariableProvider(key + "BindingConfig", deviceClass.getBindingConfig().getMetaConfig()));
                    variablePool.register(new MetaConfigVariableProvider(key + "ClassMetaConfig", deviceClass.getMetaConfig()));
                    variablePool.register(new ProtobufVariableProvider(deviceClass));
                    break;
                case APP:
                    final AppClass appClass = Registries.getClassRegistry().getAppClassById(unitConfig.getAppConfig().getAppClassId());
                    variablePool.register(new MetaConfigVariableProvider(key + "BindingConfig", appClass.getBindingConfig().getMetaConfig()));
                    variablePool.register(new MetaConfigVariableProvider(key + "ClassMetaConfig", appClass.getMetaConfig()));
                    variablePool.register(new ProtobufVariableProvider(appClass));
                    break;
                case GATEWAY:
                    final GatewayClass gatewayClass = Registries.getClassRegistry().getGatewayClassById(unitConfig.getGatewayConfig().getGatewayClassId());
                    variablePool.register(new MetaConfigVariableProvider(key + "ClassMetaConfig", gatewayClass.getMetaConfig()));
                    variablePool.register(new ProtobufVariableProvider(gatewayClass));
                    for (String id : gatewayClass.getNestedGatewayClassIdList()) {
                        final GatewayClass nestedGatewayClass = Registries.getClassRegistry(true).getGatewayClassById(id);
                        variablePool.register(new MetaConfigVariableProvider(key + "NestedGatewayClass[" + LabelProcessor.getBestMatch(nestedGatewayClass.getLabel(), id) + "]", nestedGatewayClass.getMetaConfig()));
                    }

                    for (String id : unitConfig.getGatewayConfig().getNestedGatewayIdList()) {
                        final UnitConfig nestedGatewayUnitConfig = Registries.getUnitRegistry(true).getUnitConfigById(id);
                        variablePool.register(new MetaConfigVariableProvider(key + "NestedGatewayClass[" + LabelProcessor.getBestMatch(nestedGatewayUnitConfig.getLabel(), id) + "]", nestedGatewayUnitConfig.getMetaConfig()));
                    }
                    variablePool.register(new ProtobufVariableProvider(gatewayClass));
                    break;
            }

            // HostConfigMetaConfig
            if (UnitConfigProcessor.isHostUnitAvailable(unitConfig)) {
                registerVariables(getHostUnitConfig(), variablePool);
            }

            return variablePool;
        } catch (final CouldNotPerformException ex) {
            throw new NotAvailableException("Variable Provider not available!", ex);
        }
    }

    /**
     * Returns if this unit belongs to the infrastructure.
     * Infrastructure units should not be included in actions for multiple units to prevent
     * accidentally switching of important units for the infrastructure.
     *
     * @return if the the infrastructure flag is set to true in a meta config for this unit
     */
    boolean isInfrastructure();

    /**
     * This method returns the parent location config of this unit.
     * If this unit is a location, than its parent location config is returned,
     * otherwise the parent location config is returned which refers the location where this unit is placed in.
     *
     * @return a unit config of the parent location.
     *
     * @throws NotAvailableException is thrown if the location config is currently not available.
     */
    default UnitConfig getParentLocationConfig() throws NotAvailableException {
        try {
            return Registries.getUnitRegistry().getUnitConfigByIdAndUnitType(getConfig().getPlacementConfig().getLocationId(), UnitType.LOCATION);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("LocationConfig", ex);
        }
    }

    /**
     * This method cancels the given action on remote controller.
     *
     * @param actionDescription the description which identifies the action.
     *
     * @return a future object representing the success of the cancellation.
     */
    Future<ActionDescription> cancelAction(final ActionDescription actionDescription);

    /**
     * This method extends the given action on remote controller.
     *
     * @param actionDescription the action to extend.
     *
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
    Future<AuthenticatedValueType.AuthenticatedValue> queryAggregatedServiceStateAuthenticated(final AuthenticatedValueType.AuthenticatedValue databaseQuery);

    Future<AggregatedServiceState> queryAggregatedServiceState(final QueryType.Query databaseQuery);

    @RPCMethod
    Future<AuthenticatedValueType.AuthenticatedValue> queryRecordAuthenticated(final AuthenticatedValueType.AuthenticatedValue databaseQuery);

    Future<RecordCollectionType.RecordCollection> queryRecord(final QueryType.Query databaseQuery);
}
