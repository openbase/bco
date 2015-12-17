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
import rst.authorization.GroupConfigType.GroupConfig;

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

    public List<UserConfig> getUserConfigsByGroupConfig(GroupConfig groupConfig) throws CouldNotPerformException;

    public Future<Boolean> isUserConfigRegistryReadOnly() throws CouldNotPerformException;

    public GroupConfig registerGroupConfig(GroupConfig groupConfig) throws CouldNotPerformException;

    public Boolean containsGroupConfig(GroupConfig groupConfig) throws CouldNotPerformException;

    public Boolean containsGroupConfigById(String groupConfigId) throws CouldNotPerformException;

    public GroupConfig updateGroupConfig(GroupConfig groupConfig) throws CouldNotPerformException;

    public GroupConfig removeGroupConfig(GroupConfig groupConfig) throws CouldNotPerformException;

    public GroupConfig getGroupConfigById(final String groupConfigId) throws CouldNotPerformException;

    public List<GroupConfig> getGroupConfigs() throws CouldNotPerformException;

    public List<GroupConfig> getGroupConfigsbyUserConfig(UserConfig userConfig) throws CouldNotPerformException;

    public Future<Boolean> isGroupConfigRegistryReadOnly() throws CouldNotPerformException;

    public void shutdown();
}
