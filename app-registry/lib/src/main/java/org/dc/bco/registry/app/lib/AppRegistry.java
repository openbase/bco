/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.app.lib;

import org.dc.jul.exception.CouldNotPerformException;
import java.util.List;
import java.util.concurrent.Future;
import rst.homeautomation.control.app.AppConfigType.AppConfig;

/**
 *
 * @author mpohling
 */
public interface AppRegistry {

    public AppConfig registerAppConfig(AppConfig appConfig) throws CouldNotPerformException;

    public Boolean containsAppConfig(AppConfig appConfig) throws CouldNotPerformException;

    public Boolean containsAppConfigById(String appConfigId) throws CouldNotPerformException;

    public AppConfig updateAppConfig(AppConfig appConfig) throws CouldNotPerformException;

    public AppConfig removeAppConfig(AppConfig appConfig) throws CouldNotPerformException;

    public AppConfig getAppConfigById(final String appConfigId) throws CouldNotPerformException;
    
    public List<AppConfig> getAppConfigs() throws CouldNotPerformException;
    
     public Future<Boolean> isAppConfigRegistryReadOnly() throws CouldNotPerformException;
    
    public void shutdown();
}
