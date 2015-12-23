/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.usr.lib.registry;

import de.citec.jul.exception.CouldNotPerformException;
import java.util.List;
import java.util.concurrent.Future;
import rst.authorization.UserConfigType.UserConfig;
import rst.authorization.UserGroupConfigType.UserGroupConfig;

/**
 *
 * @author mpohling
 */
public interface UserRegistryInterface {

    public UserConfig registerUserConfig(UserConfig userConfig) throws CouldNotPerformException;

    public Boolean containsUserConfig(UserConfig userConfig) throws CouldNotPerformException;

    public Boolean containsUserConfigById(String userConfigId) throws CouldNotPerformException;

    public UserConfig updateUserConfig(UserConfig userConfig) throws CouldNotPerformException;

    public UserConfig removeUserConfig(UserConfig userConfig) throws CouldNotPerformException;

    public UserConfig getUserConfigById(final String userConfigId) throws CouldNotPerformException;

    public List<UserConfig> getUserConfigs() throws CouldNotPerformException;

    public List<UserConfig> getUserConfigsByUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException;

    public Future<Boolean> isUserConfigRegistryReadOnly() throws CouldNotPerformException;

    public UserGroupConfig registerUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException;

    public Boolean containsUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException;

    public Boolean containsUserGroupConfigById(String groupConfigId) throws CouldNotPerformException;

    public UserGroupConfig updateUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException;

    public UserGroupConfig removeUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException;

    public UserGroupConfig getUserGroupConfigById(final String groupConfigId) throws CouldNotPerformException;

    public List<UserGroupConfig> getUserGroupConfigs() throws CouldNotPerformException;
    
    public List<UserGroupConfig> getUserGroupConfigsbyUserConfig(UserConfig userConfig) throws CouldNotPerformException;

    public Future<Boolean> isUserGroupConfigRegistryReadOnly() throws CouldNotPerformException;
    
    public void shutdown();
}
