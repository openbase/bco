/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.lib;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import java.util.List;
import java.util.concurrent.Future;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.ConnectionConfigType.ConnectionConfig;

/**
 *
 * @author mpohling
 */
public interface LocationRegistry {

    /**
     * This method registered the given location config in the registry.
     *
     * @param locationConfig
     * @return
     * @throws CouldNotPerformException is thrown in case if the registered
     * entry already exists or is inconsistent.
     */
    public LocationConfig registerLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException;

    /**
     * Method returns the location config which is registered with the given
     * location id.
     *
     * @param locationId
     * @return
     * @throws CouldNotPerformException
     */
    public LocationConfig getLocationConfigById(final String locationId) throws CouldNotPerformException;

    /**
     * Method returns all location configs which are assigned to the given
     * label.
     *
     * @param locationLabel
     * @return
     * @throws CouldNotPerformException
     */
    public List<LocationConfig> getLocationConfigsByLabel(final String locationLabel) throws CouldNotPerformException;

    /**
     * Method returns true if the location config with the given id is
     * registered, otherwise false. The location config id field is used for the
     * comparison.
     *
     * @param locationConfig
     * @return
     * @throws CouldNotPerformException
     */
    public Boolean containsLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException;

    /**
     * Method returns true if the location config with the given id is
     * registered, otherwise false.
     *
     * @param locationId
     * @return
     * @throws CouldNotPerformException
     */
    public Boolean containsLocationConfigById(final String locationId) throws CouldNotPerformException;

    /**
     * Method updates the given location config.
     *
     * @param locationConfig
     * @return the updated location config.
     * @throws CouldNotPerformException
     */
    public LocationConfig updateLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException;

    /**
     * Method removes the given location config out of the global registry.
     *
     * @param locationConfig
     * @return The removed location config.
     * @throws CouldNotPerformException
     */
    public LocationConfig removeLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException;

    /**
     * Method returns all registered location configs.
     *
     * @return the location configs stored in this registry.
     * @throws CouldNotPerformException
     * @throws NotAvailableException
     */
    public List<LocationConfig> getLocationConfigs() throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are direct or recursive
     * related to the given location id.
     *
     * @param locationId
     * @return A collection of unit configs.
     * @throws CouldNotPerformException
     * @throws NotAvailableException
     */
    public List<UnitConfig> getUnitConfigsByLocation(final String locationId) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are direct or recursive
     * related to the given location label which can represent more than one
     * location.
     *
     * @param locationLabel
     * @return A collection of unit configs.
     * @throws CouldNotPerformException
     * @throws NotAvailableException
     */
    public List<UnitConfig> getUnitConfigsByLocationLabel(final String locationLabel) throws CouldNotPerformException;

    /**
     * Method returns a collection of unit configs which are located within the
     * defined location and match the given unit label.
     *
     * @param unitLabel
     * @param locationId
     * @return
     * @throws CouldNotPerformException
     */
    public List<UnitConfig> getUnitConfigsByLabelAndLocation(final String unitLabel, final String locationId) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are direct or recursive
     * related to the given location id and an instance of the given unit type.
     *
     * @param type
     * @param locationConfigId
     * @return A collection of unit configs.
     * @throws CouldNotPerformException
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
     * @throws CouldNotPerformException
     * @throws NotAvailableException
     */
    public List<UnitConfig> getUnitConfigsByLocation(final ServiceType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException;

    /**
     * Method returns all service configurations which are direct or recursive
     * related to the given location id.
     *
     * @param locationId
     * @return the list of service configurations.
     * @throws CouldNotPerformException
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
     * @throws CouldNotPerformException
     */
    public Future<Boolean> isLocationConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns the root location of the registered location hierarchy
     * tree.
     *
     * @return the root location
     * @throws CouldNotPerformException
     * @throws NotAvailableException
     */
    public LocationConfig getRootLocationConfig() throws CouldNotPerformException, NotAvailableException;

    // TODO mpohling: should be moved with init in a own interface.
    public void shutdown();

    /**
     * This method registers the given connection config in the registry.
     *
     * @param connectionConfig
     * @return
     * @throws CouldNotPerformException is thrown in case if the registered
     * entry already exists or is inconsistent.
     */
    public ConnectionConfig registerConnectionConfig(final ConnectionConfig connectionConfig) throws CouldNotPerformException;

    /**
     * Method returns the connection config which is registered with the given
     * connection id.
     *
     * @param connectionId
     * @return
     * @throws CouldNotPerformException
     */
    public ConnectionConfig getConnectionConfigById(final String connectionId) throws CouldNotPerformException;

    /**
     * Method returns all connection configs which are assigned to the given
     * label.
     *
     * @param connectionLabel
     * @return
     * @throws CouldNotPerformException
     */
    public List<ConnectionConfig> getConnectionConfigsByLabel(final String connectionLabel) throws CouldNotPerformException;

    /**
     * Method returns true if the connection config with the given id is
     * registered, otherwise false. The connection config id field is used for
     * the comparison.
     *
     * @param connectionConfig
     * @return
     * @throws CouldNotPerformException
     */
    public Boolean containsConnectionConfig(final ConnectionConfig connectionConfig) throws CouldNotPerformException;

    /**
     * Method returns true if the connection config with the given id is
     * registered, otherwise false.
     *
     * @param connectionId
     * @return
     * @throws CouldNotPerformException
     */
    public Boolean containsConnectionConfigById(final String connectionId) throws CouldNotPerformException;

    /**
     * Method updates the given connection config.
     *
     * @param connectionConfig
     * @return the updated connection config.
     * @throws CouldNotPerformException
     */
    public ConnectionConfig updateConnectionConfig(final ConnectionConfig connectionConfig) throws CouldNotPerformException;

    /**
     * Method removes the given connection config out of the global registry.
     *
     * @param connectionConfig
     * @return The removed connection config.
     * @throws CouldNotPerformException
     */
    public ConnectionConfig removeConnectionConfig(final ConnectionConfig connectionConfig) throws CouldNotPerformException;

    /**
     * Method returns all registered connection configs.
     *
     * @return the connection configs stored in this registry.
     * @throws CouldNotPerformException
     * @throws NotAvailableException
     */
    public List<ConnectionConfig> getConnectionConfigs() throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are related to the given
     * connection id.
     *
     * @param connectionConfigId
     * @return A collection of unit configs.
     * @throws CouldNotPerformException
     * @throws NotAvailableException
     */
    public List<UnitConfig> getUnitConfigsByConnection(final String connectionConfigId) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are related to the given
     * connection id and an instance of the given unit type.
     *
     * @param type
     * @param connectionConfigId
     * @return A collection of unit configs.
     * @throws CouldNotPerformException
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
     * @throws CouldNotPerformException
     * @throws NotAvailableException
     */
    public List<UnitConfig> getUnitConfigsByConnection(final ServiceType type, final String connectionConfigId) throws CouldNotPerformException, NotAvailableException;

    /**
     * Method returns all service configurations which are related to the given
     * connection id.
     *
     * @param connectionConfigId
     * @return the list of service configurations.
     * @throws CouldNotPerformException
     * @throws NotAvailableException is thrown if the given connection config id
     * is unknown.
     */
    public List<ServiceConfig> getServiceConfigsByConnection(final String connectionConfigId) throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     *
     * @return
     * @throws CouldNotPerformException
     */
    public Future<Boolean> isConnectionConfigRegistryReadOnly() throws CouldNotPerformException;

}
