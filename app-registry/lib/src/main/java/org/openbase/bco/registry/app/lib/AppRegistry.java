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
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.domotic.unit.app.AppClassType.AppClass;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface AppRegistry extends Shutdownable {

    @RPCMethod
    public Future<UnitConfig> registerAppConfig(UnitConfig appUnitConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsAppConfig(UnitConfig appUnitConfig) throws CouldNotPerformException, InterruptedException;

    @RPCMethod
    public Boolean containsAppConfigById(String appConfigId) throws CouldNotPerformException, InterruptedException;

    @RPCMethod
    public Future<UnitConfig> updateAppConfig(UnitConfig appUnitConfigId) throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> removeAppConfig(UnitConfig appUnitConfig) throws CouldNotPerformException;

    @RPCMethod
    public UnitConfig getAppConfigById(final String appUnitConfigId) throws CouldNotPerformException, InterruptedException;

    public List<UnitConfig> getAppConfigs() throws CouldNotPerformException, InterruptedException;

    public List<UnitConfig> getAppConfigsByAppClass(AppClass appClass) throws CouldNotPerformException, InterruptedException;

    public List<UnitConfig> getAppConfigsByAppClassId(String appClassId) throws CouldNotPerformException, InterruptedException;

    @RPCMethod
    public Boolean isAppConfigRegistryReadOnly() throws CouldNotPerformException, InterruptedException;

    @RPCMethod
    public Future<AppClass> registerAppClass(AppClass appClass) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsAppClass(AppClass appClass) throws CouldNotPerformException, InterruptedException;

    @RPCMethod
    public Boolean containsAppClassById(String appClassId) throws CouldNotPerformException, InterruptedException;

    @RPCMethod
    public Future<AppClass> updateAppClass(AppClass appClass) throws CouldNotPerformException;

    @RPCMethod
    public Future<AppClass> removeAppClass(AppClass appClass) throws CouldNotPerformException;

    @RPCMethod
    public AppClass getAppClassById(final String appClassId) throws CouldNotPerformException, InterruptedException;

    public List<AppClass> getAppClasses() throws CouldNotPerformException, InterruptedException;

    @RPCMethod
    public Boolean isAppClassRegistryReadOnly() throws CouldNotPerformException, InterruptedException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the app class registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    public Boolean isAppClassRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the app config registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    public Boolean isAppConfigRegistryConsistent() throws CouldNotPerformException;
}
