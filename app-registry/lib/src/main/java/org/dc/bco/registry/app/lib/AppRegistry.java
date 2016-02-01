/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.app.lib;

/*
 * #%L
 * REM AppRegistry Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
