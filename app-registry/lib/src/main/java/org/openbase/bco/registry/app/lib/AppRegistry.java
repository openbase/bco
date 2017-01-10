package org.openbase.bco.registry.app.lib;

/*
 * #%L
 * BCO Registry App Library
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
import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.unit.app.AppClassType.AppClass;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface AppRegistry {

    public Future<UnitConfig> registerAppConfig(UnitConfig appUnitConfig) throws CouldNotPerformException;

    public Boolean containsAppConfig(UnitConfig appUnitConfig) throws CouldNotPerformException, InterruptedException;

    public Boolean containsAppConfigById(String appConfigId) throws CouldNotPerformException, InterruptedException;

    public Future<UnitConfig> updateAppConfig(UnitConfig appUnitConfigId) throws CouldNotPerformException;

    public Future<UnitConfig> removeAppConfig(UnitConfig appUnitConfig) throws CouldNotPerformException;

    public UnitConfig getAppConfigById(final String appUnitConfigId) throws CouldNotPerformException, InterruptedException;

    public List<UnitConfig> getAppConfigs() throws CouldNotPerformException, InterruptedException;

    public List<UnitConfig> getAppConfigsByAppClass(AppClass appClass) throws CouldNotPerformException, InterruptedException;

    public List<UnitConfig> getAppConfigsByAppClassId(String appClassId) throws CouldNotPerformException, InterruptedException;

    public Boolean isAppConfigRegistryReadOnly() throws CouldNotPerformException, InterruptedException;

    public Future<AppClass> registerAppClass(AppClass appClass) throws CouldNotPerformException;

    public Boolean containsAppClass(AppClass appClass) throws CouldNotPerformException, InterruptedException;

    public Boolean containsAppClassById(String appClassId) throws CouldNotPerformException, InterruptedException;

    public Future<AppClass> updateAppClass(AppClass appClass) throws CouldNotPerformException;

    public Future<AppClass> removeAppClass(AppClass appClass) throws CouldNotPerformException;

    public AppClass getAppClassById(final String appClassId) throws CouldNotPerformException, InterruptedException;

    public List<AppClass> getAppClasses() throws CouldNotPerformException, InterruptedException;

    public Boolean isAppClassRegistryReadOnly() throws CouldNotPerformException, InterruptedException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the app class registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    public Boolean isAppClassRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the app config registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    public Boolean isAppConfigRegistryConsistent() throws CouldNotPerformException;

    public void shutdown();
}
