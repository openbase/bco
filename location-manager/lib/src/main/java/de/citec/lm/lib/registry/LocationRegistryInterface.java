/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.lib.registry;

import de.citec.jul.exception.CouldNotPerformException;
import java.util.List;
import java.util.concurrent.Future;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public interface LocationRegistryInterface {

    public LocationConfig registerLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException;
    
    public LocationConfig getLocationConfigById(final String locationId) throws CouldNotPerformException;

    public List<LocationConfig> getLocationConfigsByLabel(final String locationId) throws CouldNotPerformException;

    public Boolean containsLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException;

    public Boolean containsLocationConfigById(final String locationId) throws CouldNotPerformException;

    public LocationConfig updateLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException;

    public LocationConfig removeLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException;
    
    public List<LocationConfig> getLocationConfigs() throws CouldNotPerformException;
    
    public List<UnitConfig> getUnitConfigs(final String locationId) throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigsByLabel(final String unitLabel, final String locationId) throws CouldNotPerformException;

    public List<ServiceConfig> getServiceConfigs(final String locationId) throws CouldNotPerformException;
    
    public Future<Boolean> isLocationConfigRegistryReadOnly() throws CouldNotPerformException;
    
    public void shutdown();

}
