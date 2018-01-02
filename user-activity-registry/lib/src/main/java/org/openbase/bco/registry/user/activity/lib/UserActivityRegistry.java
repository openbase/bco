package org.openbase.bco.registry.user.activity.lib;

/*
 * #%L
 * BCO Registry User Activity Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.jul.pattern.provider.DataProvider;
import rst.domotic.activity.UserActivityClassType.UserActivityClass;
import rst.domotic.activity.UserActivityClassType.UserActivityClass.UserActivityType;
import rst.domotic.activity.UserActivityConfigType.UserActivityConfig;
import rst.domotic.registry.UserActivityRegistryDataType.UserActivityRegistryData;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface UserActivityRegistry extends DataProvider<UserActivityRegistryData>, Shutdownable {
    
    // ===================================== UserActivityClass Methods =========================================================================================
    
    @RPCMethod
    public Future<UserActivityClass> registerUserActivityClass(UserActivityClass userActivityClass) throws CouldNotPerformException;

    @RPCMethod
    public Future<UserActivityClass> updateUserActivityClass(UserActivityClass userActivityClass) throws CouldNotPerformException;

    @RPCMethod
    public Future<UserActivityClass> removeUserActivityClass(UserActivityClass userActivityClass) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsUserActivityClass(UserActivityClass userActivityClass) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsUserActivityClassById(String userActivityClassId) throws CouldNotPerformException;

    @RPCMethod
    public UserActivityClass getUserActivityClassById(final String userActivityClassId) throws CouldNotPerformException;

    public List<UserActivityClass> getUserActivityClasses() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isUserActivityClassRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the user activity class registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    public Boolean isUserActivityClassRegistryConsistent() throws CouldNotPerformException;
    
    public UserActivityClass getUserActivityClassByType(final UserActivityType userActivityType) throws CouldNotPerformException;
    
    // ===================================== UserActivityConfig Methods ==============================================================================================
    
    
    @RPCMethod
    public Future<UserActivityConfig> registerUserActivityConfig(UserActivityConfig userActivityConfig) throws CouldNotPerformException;

    @RPCMethod
    public Future<UserActivityConfig> updateUserActivityConfig(UserActivityConfig userActivityConfig) throws CouldNotPerformException;

    @RPCMethod
    public Future<UserActivityConfig> removeUserActivityConfig(UserActivityConfig userActivityConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsUserActivityConfig(UserActivityConfig userActivityConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsUserActivityConfigById(String userActivityConfigId) throws CouldNotPerformException;

    @RPCMethod
    public UserActivityConfig getUserActivityConfigById(final String userActivityConfigId) throws CouldNotPerformException;

    public List<UserActivityConfig> getUserActivityConfigs() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isUserActivityConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the user activity config registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    public Boolean isUserActivityConfigRegistryConsistent() throws CouldNotPerformException;
    
    public List<UserActivityConfig> getUserActivityConfigsByType(final UserActivityType userActivityType) throws CouldNotPerformException;
}
