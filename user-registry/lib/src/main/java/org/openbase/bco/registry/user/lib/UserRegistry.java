package org.openbase.bco.registry.user.lib;

/*
 * #%L
 * REM UserRegistry Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.jul.iface.Shutdownable;
import rst.authorization.UserConfigType.UserConfig;
import rst.authorization.UserGroupConfigType.UserGroupConfig;

/**
 *
 * @author mpohling
 */
public interface UserRegistry extends Shutdownable {

    public Future<UserConfig> registerUserConfig(UserConfig userConfig) throws CouldNotPerformException;

    public Boolean containsUserConfig(UserConfig userConfig) throws CouldNotPerformException;

    public Boolean containsUserConfigById(String userConfigId) throws CouldNotPerformException;

    public Future<UserConfig> updateUserConfig(UserConfig userConfig) throws CouldNotPerformException;

    public Future<UserConfig> removeUserConfig(UserConfig userConfig) throws CouldNotPerformException;

    public UserConfig getUserConfigById(final String userConfigId) throws CouldNotPerformException;

    public List<UserConfig> getUserConfigs() throws CouldNotPerformException;

    public List<UserConfig> getUserConfigsByUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException;

    public Boolean isUserConfigRegistryReadOnly() throws CouldNotPerformException;

    public Future<UserGroupConfig> registerUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException;

    public Boolean containsUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException;

    public Boolean containsUserGroupConfigById(String groupConfigId) throws CouldNotPerformException;

    public Future<UserGroupConfig> updateUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException;

    public Future<UserGroupConfig> removeUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException;

    public UserGroupConfig getUserGroupConfigById(final String groupConfigId) throws CouldNotPerformException;

    public List<UserGroupConfig> getUserGroupConfigs() throws CouldNotPerformException;

    public List<UserGroupConfig> getUserGroupConfigsbyUserConfig(UserConfig userConfig) throws CouldNotPerformException;

    public Boolean isUserGroupConfigRegistryReadOnly() throws CouldNotPerformException;
}
