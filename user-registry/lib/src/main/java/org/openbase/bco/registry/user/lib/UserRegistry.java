package org.openbase.bco.registry.user.lib;

/*
 * #%L
 * BCO Registry User Library
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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.pattern.provider.DataProvider;
import rst.domotic.registry.UserRegistryDataType.UserRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface UserRegistry extends DataProvider<UserRegistryData>, Shutdownable {

    @RPCMethod
    public Future<UnitConfig> registerUserConfig(UnitConfig userConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsUserConfig(UnitConfig userConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsUserConfigById(String userConfigId) throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> updateUserConfig(UnitConfig userConfig) throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> removeUserConfig(UnitConfig userConfig) throws CouldNotPerformException;

    @RPCMethod
    public UnitConfig getUserConfigById(final String userConfigId) throws CouldNotPerformException;

    public List<UnitConfig> getUserConfigs() throws CouldNotPerformException;

    public List<UnitConfig> getUserConfigsByAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean isUserConfigRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> registerAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsAuthorizationGroupConfigById(String groupConfigId) throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> updateAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> removeAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException;

    @RPCMethod
    public UnitConfig getAuthorizationGroupConfigById(final String groupConfigId) throws CouldNotPerformException;

    public List<UnitConfig> getAuthorizationGroupConfigs() throws CouldNotPerformException;

    public List<UnitConfig> getAuthorizationGroupConfigsbyUserConfig(UnitConfig userConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean isAuthorizationGroupConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the user config registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    public Boolean isUserConfigRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the authorization group config registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    public Boolean isAuthorizationGroupConfigRegistryConsistent() throws CouldNotPerformException;

    /**
     * Retrieves a user config according to a given user name.
     * If multiple users happen to have the same user name, the first one is returned.
     *
     * @param userName
     * @return
     * @throws CouldNotPerformException
     * @throws NotAvailableException If no user with the given user name could be found.
     */
    @RPCMethod
    public UnitConfig getUserConfigByUserName(final String userName) throws CouldNotPerformException, NotAvailableException;
}
