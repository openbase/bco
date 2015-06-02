/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.lib.registry;

import de.citec.jul.exception.CouldNotPerformException;
import java.util.List;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.unit.UnitConfigType;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public interface LocationRegistryInterface {

    public LocationConfigType.LocationConfig registerLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException;
    
    public LocationConfigType.LocationConfig getLocationConfigById(final String locationConfigId) throws CouldNotPerformException;

    public Boolean containsLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException;

    public Boolean containsLocationConfigById(final String locationConfigId) throws CouldNotPerformException;

    public LocationConfigType.LocationConfig updateLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException;

    public LocationConfigType.LocationConfig removeLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException;
    
    public List<LocationConfigType.LocationConfig> getLocationConfigs() throws CouldNotPerformException;
    
    public List<UnitConfigType.UnitConfig> getUnitConfigs(final String locationConfigId) throws CouldNotPerformException;

    public List<ServiceConfigType.ServiceConfig> getServiceConfigs(final String locationConfigId) throws CouldNotPerformException;
    
    public void shutdown();

}
