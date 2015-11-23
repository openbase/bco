/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.lib.registry;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import java.util.List;
import java.util.concurrent.Future;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public interface LocationRegistryInterface {

    /**
     * This method registered the given location config in the registry.
     *
     * @param locationConfig
     * @return
     * @throws CouldNotPerformException is thrown in case if the registered entry already exists or is inconsistent.
     */
    public LocationConfig registerLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException;

    /**
     * Method returns the location config which is registered with the given location id.
     *
     * @param locationId
     * @return
     * @throws CouldNotPerformException
     */
    public LocationConfig getLocationConfigById(final String locationId) throws CouldNotPerformException;

    /**
     * Method returns all location configs which are assigned to the given label.
     *
     * @param locationLabel
     * @return
     * @throws CouldNotPerformException
     */
    public List<LocationConfig> getLocationConfigsByLabel(final String locationLabel) throws CouldNotPerformException;

    /**
     * Method returns true if the location config with the given id is registered, otherwise false. The location config id field is used for the comparison.
     *
     * @param locationConfig
     * @return
     * @throws CouldNotPerformException
     */
    public Boolean containsLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException;

    /**
     * Method returns true if the location config with the given id is registered, otherwise false.
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
     * Method returns all unit configurations which are direct or recursive related to the given location id.
     *
     * @param locationId
     * @return A collection of unit configs.
     * @throws CouldNotPerformException
     * @throws NotAvailableException
     */
    public List<UnitConfig> getUnitConfigs(final String locationId) throws CouldNotPerformException;

    /**
     * Method returns a collection of unit configs which are located within the defined location and match the given unit label.
     *
     * @param unitLabel
     * @param locationId
     * @return
     * @throws CouldNotPerformException
     */
    public List<UnitConfig> getUnitConfigsByLabel(final String unitLabel, final String locationId) throws CouldNotPerformException;

    /**
     * Method returns all unit configurations which are direct or recursive related to the given location id and an instance of the given unit type.
     *
     * @param type
     * @param locationConfigId
     * @return A collection of unit configs.
     * @throws CouldNotPerformException
     * @throws NotAvailableException
     */
    public List<UnitConfigType.UnitConfig> getUnitConfigs(final UnitTemplateType.UnitTemplate.UnitType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException;

    /**
     * Method returns all unit configurations which are direct or recursive related to the given location id and an implement the given service type.
     *
     * @param type service type filter.
     * @param locationConfigId related location.
     * @return A collection of unit configs.
     * @throws CouldNotPerformException
     * @throws NotAvailableException
     */
    public List<UnitConfigType.UnitConfig> getUnitConfigs(final ServiceTemplateType.ServiceTemplate.ServiceType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException;

    /**
     * Method returns all service configurations which are direct or recursive related to the given location id.
     *
     * @param locationId
     * @return the list of service configurations.
     * @throws CouldNotPerformException
     * @throws NotAvailableException is thrown if the given location config id is unknown.
     */
    public List<ServiceConfig> getServiceConfigs(final String locationId) throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as read only. A registry is marked as read only in case of inconsistently data entries or if the underling database is loaded out of a
     * version tag.
     *
     * @return
     * @throws CouldNotPerformException
     */
    public Future<Boolean> isLocationConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns the root location of the registered location hierarchy tree.
     *
     * @return the root location
     * @throws CouldNotPerformException
     * @throws NotAvailableException
     */
    public LocationConfig getRootLocationConfig() throws CouldNotPerformException, NotAvailableException;

    // TODO mpohling: should be moved with init in a own interface.
    public void shutdown();

}
