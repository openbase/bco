package org.openbase.bco.registry.location.lib;

/*
 * #%L
 * BCO Registry Location Library
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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rct.GlobalTransformReceiver;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.iface.annotations.RPCMethod;
import rct.Transform;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.FutureProcessor;
import rst.domotic.registry.LocationRegistryDataType.LocationRegistryData;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.EnablingStateType.EnablingState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitProbabilityCollectionType.UnitProbabilityCollection;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import rst.math.Vec3DDoubleType.Vec3DDouble;
import rst.tracking.PointingRay3DFloatCollectionType.PointingRay3DFloatCollection;
import rst.tracking.PointingRay3DFloatType.PointingRay3DFloat;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface LocationRegistry extends DataProvider<LocationRegistryData>, Shutdownable {

    /**
     * This method registers the given location config in the registry.
     *
     * @param locationConfig the location config registered
     * @return the registered location config
     * @throws CouldNotPerformException is thrown if the entry already exists or results in an inconsistent registry
     */
    @RPCMethod
    public Future<UnitConfig> registerLocationConfig(final UnitConfig locationConfig) throws CouldNotPerformException;

    /**
     * Method returns the location config which is registered with the given
     * location id.
     *
     * @param locationId
     * @return the requested unit config.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    public UnitConfig getLocationConfigById(final String locationId) throws CouldNotPerformException;

    /**
     * Method returns all location configs which are assigned to the given
     * label. Label resolving is done case insensitive!
     *
     * @param locationLabel
     * @return a list of the requested unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    public List<UnitConfig> getLocationConfigsByLabel(final String locationLabel) throws CouldNotPerformException;

    /**
     * Method returns all the locations which contain the given coordinate.
     *
     * @param coordinate
     * @return a list of the requested unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public default List<UnitConfig> getLocationConfigsByCoordinate(final Vec3DDouble coordinate) throws CouldNotPerformException, InterruptedException, ExecutionException {
        return getLocationConfigsByCoordinate(coordinate, LocationType.UNKNOWN);
    }

    /**
     * Method returns all the locations which contain the given coordinate and
     * belong to the given location type.
     *
     * @param coordinate
     * @param locationType
     * @return a list of the requested unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public List<UnitConfig> getLocationConfigsByCoordinate(final Vec3DDouble coordinate, LocationType locationType) throws CouldNotPerformException, InterruptedException, ExecutionException;

    /**
     * Method returns true if the location config with the given id is
     * registered, otherwise false. The location config id field is used for the
     * comparison.
     *
     * @param locationConfig
     * @return
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    public Boolean containsLocationConfig(final UnitConfig locationConfig) throws CouldNotPerformException;

    /**
     * Method returns true if the location config with the given id is
     * registered, otherwise false.
     *
     * @param locationId
     * @return
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    public Boolean containsLocationConfigById(final String locationId) throws CouldNotPerformException;

    /**
     * Method updates the given location config.
     *
     * @param locationConfig the updated location config.
     * @return the updated location config.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    public Future<UnitConfig> updateLocationConfig(final UnitConfig locationConfig) throws CouldNotPerformException;

    /**
     * Method removes the given location config out of the global registry.
     *
     * @param locationConfig the location unit config to remove
     * @return The removed location config.
     * @throws CouldNotPerformException is thrown if the removal fails.
     */
    @RPCMethod
    public Future<UnitConfig> removeLocationConfig(final UnitConfig locationConfig) throws CouldNotPerformException;

    /**
     * Method returns all registered location configs.
     *
     * @return the location configs stored in this registry.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    public List<UnitConfig> getLocationConfigs() throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are direct or recursive related to the given location id.
     *
     * @param locationId the id of the location which provides the units.
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    public List<UnitConfig> getUnitConfigsByLocation(final String locationId) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are direct related to the given location id.
     * In case the {@code recursive} flag is set to true than recursive related units are included as well.
     *
     * @param locationId the id of the location which provides the units.
     * @param recursive defines if recursive related unit should be included as well.
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    public List<UnitConfig> getUnitConfigsByLocation(final String locationId, final boolean recursive) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are direct or recursive
     * related to the given location label which can represent more than one
     * location. Label resolving is done case insensitive!
     *
     * @param locationLabel
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    public List<UnitConfig> getUnitConfigsByLocationLabel(final String locationLabel) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations with a given type which are direct
     * or recursive related to the given location label which can represent more
     * than one location. Label resolving is done case insensitive!
     *
     * @param unitType
     * @param locationLabel
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    public List<UnitConfig> getUnitConfigsByLocationLabel(final UnitType unitType, final String locationLabel) throws CouldNotPerformException;

    /**
     * Method returns a collection of unit configs which are located within the
     * defined location and match the given unit label. Label resolving is done
     * case insensitive!
     *
     * @param unitLabel
     * @param locationId
     * @return
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    public List<UnitConfig> getUnitConfigsByLabelAndLocation(final String unitLabel, final String locationId) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are direct or recursive
     * related to the given location id and an instance of the given unit type.
     * Label resolving is done case insensitive!
     *
     * @param type
     * @param locationConfigId
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    public List<UnitConfig> getUnitConfigsByLocation(final UnitType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException;

    /**
     * Method returns all unit configurations which are direct or recursive
     * related to the given location id and an implement the given service type.
     *
     * @param type service type filter.
     * @param locationConfigId related location.
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    public List<UnitConfig> getUnitConfigsByLocation(final ServiceType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException;

    /**
     * Method returns all service configurations which are direct or recursive
     * related to the given location id.
     *
     * @param locationId
     * @return the list of service configurations.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException is thrown if the given location config id
     * is unknown.
     */
    public List<ServiceConfig> getServiceConfigsByLocation(final String locationId) throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    public Boolean isLocationConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns the root location of the registered location hierarchy
     * tree.
     *
     * @return the root location
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException is thrown if no rood connection exists.
     */
    public UnitConfig getRootLocationConfig() throws CouldNotPerformException, NotAvailableException;

    /**
     * This method registers the given connection config in the registry.
     *
     * @param connectionConfig
     * @return
     * @throws CouldNotPerformException is thrown in case if the registered entry already exists or is inconsistent.
     */
    @RPCMethod
    public Future<UnitConfig> registerConnectionConfig(final UnitConfig connectionConfig) throws CouldNotPerformException;

    /**
     * Method returns the connection config which is registered with the given
     * connection id.
     *
     * @param connectionId
     * @return
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    public UnitConfig getConnectionConfigById(final String connectionId) throws CouldNotPerformException;

    /**
     * Method returns all connection configs which are assigned to the given
     * label.
     *
     * @param connectionLabel
     * @return a collection of unit configurations.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    public List<UnitConfig> getConnectionConfigsByLabel(final String connectionLabel) throws CouldNotPerformException;

    /**
     * Method returns true if the connection config with the given id is
     * registered, otherwise false. The connection config id field is used for
     * the comparison.
     *
     * @param connectionConfig
     * @return a collection of unit configurations.
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    public Boolean containsConnectionConfig(final UnitConfig connectionConfig) throws CouldNotPerformException;

    /**
     * Method returns true if the connection config with the given id is
     * registered, otherwise false.
     *
     * @param connectionId the connection id to check.
     * @return a collection of unit configurations.
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    public Boolean containsConnectionConfigById(final String connectionId) throws CouldNotPerformException;

    /**
     * Method updates the given connection config.
     *
     * @param connectionConfig the connection config to update.
     * @return the updated connection config.
     * @throws CouldNotPerformException is thrown if the update fails.
     */
    @RPCMethod
    public Future<UnitConfig> updateConnectionConfig(final UnitConfig connectionConfig) throws CouldNotPerformException;

    /**
     * Method removes the given connection config out of the global registry.
     *
     * @param connectionConfig the connection config to remote.
     * @return The removed connection config.
     * @throws CouldNotPerformException is thrown if the removal fails.
     */
    @RPCMethod
    public Future<UnitConfig> removeConnectionConfig(final UnitConfig connectionConfig) throws CouldNotPerformException;

    /**
     * Method returns all registered connection configs.
     *
     * @return the connection configs stored in this registry.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    public List<UnitConfig> getConnectionConfigs() throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are related to the given
     * connection id.
     *
     * @param connectionConfigId
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    public List<UnitConfig> getUnitConfigsByConnection(final String connectionConfigId) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are related to the given
     * connection id and an instance of the given unit type.
     *
     * @param type
     * @param connectionConfigId
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    public List<UnitConfig> getUnitConfigsByConnection(final UnitType type, final String connectionConfigId) throws CouldNotPerformException, NotAvailableException;

    /**
     * Method returns all unit configurations which are related to the given
     * connection id and an implement the given service type.
     *
     * @param type service type filter.
     * @param connectionConfigId related connection.
     * @return A collection of unit configs.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException
     */
    public List<UnitConfig> getUnitConfigsByConnection(final ServiceType type, final String connectionConfigId) throws CouldNotPerformException, NotAvailableException;

    /**
     * Method returns all service configurations which are related to the given
     * connection id.
     *
     * @param connectionConfigId
     * @return the list of service configurations.
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException is thrown if the given connection config id
     * is unknown.
     */
    public List<ServiceConfig> getServiceConfigsByConnection(final String connectionConfigId) throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return true if the registry is read only.
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    public Boolean isConnectionConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns all neighbor tiles for a tile. If the given locationId
     * does not belong to a tile, the could not perform exception is thrown.
     *
     * @param locationId the id of the location which neighbors you want to get
     * @return all neighbor tiles
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    public List<UnitConfig> getNeighborLocations(String locationId) throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the location config registry is consistent
     * @throws CouldNotPerformException is thrown if the check fails
     */
    @RPCMethod
    public Boolean isLocationConfigRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the connection config registry is consistent
     * @throws CouldNotPerformException is thrown if the check fails
     */
    @RPCMethod
    public Boolean isConnectionConfigRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns a list of probably intersected units by the given 3D ray.
     * This could for example be useful for selecting units by pointing gestures.
     *
     * @param pointingRay3DFloat ray which probably intersects with a specific unit priorized by a given certainty.
     * @return a collection of probably intersected units referred by there id.
     * @throws CouldNotPerformException is thrown in case the computation could not be performed.
     */
    @RPCMethod
    public Future<UnitProbabilityCollection> computeUnitIntersection(final PointingRay3DFloat pointingRay3DFloat) throws CouldNotPerformException;

    /**
     * Method returns a list of probably intersected units by the given 3D rays.
     * This could for example be useful for selecting units by pointing gestures.
     *
     * @param pointingRay3DFloatCollection a collection of rays which probably intersects with a specific unit priorized by a given certainty.
     * @return a collection of probably intersected units referred by there id.
     * @throws CouldNotPerformException is thrown in case the computation could not be performed.
     */
    @RPCMethod
    public Future<UnitProbabilityCollection> computeUnitIntersection(final PointingRay3DFloatCollection pointingRay3DFloatCollection) throws CouldNotPerformException;

    /**
     * Method returns the transformation from the root location to the given unit.
     *
     * @param unitConfigTarget the unit where the transformation leads to.
     * @return a transformation future
     */
    public default Future<Transform> getUnitTransformation(final UnitConfig unitConfigTarget) {
        try {
            return getUnitTransformation(getRootLocationConfig(), unitConfigTarget);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(new NotAvailableException("UnitTransformation", ex));
        }
    }

    /**
     * Method returns the transformation between the given unit A and the given unit B.
     *
     * @param unitConfigSource the unit used as transformation base.
     * @param unitConfigTarget the unit where the transformation leads to.
     * @return a transformation future
     */
    public default Future<Transform> getUnitTransformation(final UnitConfig unitConfigSource, final UnitConfig unitConfigTarget) {

        if (unitConfigSource.getEnablingState().getValue() != State.ENABLED) {
            return FutureProcessor.canceledFuture(new InvalidStateException("Source Unit[" + unitConfigSource.getLabel() + ":" + unitConfigSource.getId() + "] is disbled and does not provide any transformation!"));
        }

        if (unitConfigTarget.getEnablingState().getValue() != State.ENABLED) {
            return FutureProcessor.canceledFuture(new InvalidStateException("Target Unit[" + unitConfigTarget.getLabel() + ":" + unitConfigTarget.getId() + "] is disbled and does not provide any transformation!"));
        }
            
        if (!unitConfigSource.hasPlacementConfig() || !unitConfigSource.getPlacementConfig().hasPosition()) {
            return FutureProcessor.canceledFuture(new InvalidStateException("Source Unit[" + unitConfigSource.getLabel() + ":" + unitConfigSource.getId() + "] does not provide any position!"));
        }

        if (!unitConfigTarget.hasPlacementConfig() || !unitConfigTarget.getPlacementConfig().hasPosition()) {
            return FutureProcessor.canceledFuture(new InvalidStateException("Target Unit[" + unitConfigTarget.getLabel() + ":" + unitConfigTarget.getId() + "] does not provide any position!"));
        }
        
        if (!unitConfigSource.getPlacementConfig().hasTransformationFrameId() || unitConfigSource.getPlacementConfig().getTransformationFrameId().isEmpty()) {
            return FutureProcessor.canceledFuture(new InvalidStateException("Source Unit[" + unitConfigSource.getLabel() + ":" + unitConfigSource.getId() + "] does not provide yet a transformation frame id!"));
        }

        if (!unitConfigTarget.getPlacementConfig().hasTransformationFrameId() || unitConfigTarget.getPlacementConfig().getTransformationFrameId().isEmpty()) {
            return FutureProcessor.canceledFuture(new InvalidStateException("Target Unit[" + unitConfigTarget.getLabel() + ":" + unitConfigTarget.getId() + "] does not provide yet a transformation frame id!"));
        }
        
        Future<Transform> transformationFuture = GlobalTransformReceiver.getInstance().requestTransform(
                unitConfigTarget.getPlacementConfig().getTransformationFrameId(),
                unitConfigSource.getPlacementConfig().getTransformationFrameId(),
                System.currentTimeMillis());
        return GlobalCachedExecutorService.allOfInclusiveResultFuture(transformationFuture, getDataFuture());
    }
}
