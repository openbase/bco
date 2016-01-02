/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.user.remote;

import org.dc.bco.registry.user.lib.jp.JPUserRegistryScope;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBRemoteService;
import org.dc.jul.storage.registry.RemoteRegistry;
import org.dc.bco.registry.user.lib.generator.UserGroupConfigIdGenerator;
import org.dc.bco.registry.user.lib.generator.UserConfigIdGenerator;
import org.dc.bco.registry.user.lib.UserRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.preset.JPReadOnly;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.UserGroupConfigType.UserGroupConfig;
import rst.authorization.UserConfigType.UserConfig;
import rst.authorization.UserRegistryType.UserRegistry;

/**
 *
 * @author mpohling
 */
public class UserRegistryRemote extends RSBRemoteService<UserRegistry> implements UserRegistry {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserGroupConfig.getDefaultInstance()));
    }

    private final RemoteRegistry<String, UserConfig, UserConfig.Builder, UserRegistry.Builder> userConfigRemoteRegistry;
    private final RemoteRegistry<String, UserGroupConfig, UserGroupConfig.Builder, UserRegistry.Builder> groupConfigRemoteRegistry;

    public UserRegistryRemote() throws InstantiationException {
        try {
            userConfigRemoteRegistry = new RemoteRegistry<>(new UserConfigIdGenerator());
            groupConfigRemoteRegistry = new RemoteRegistry<>(new UserGroupConfigIdGenerator());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        try {
            super.init(JPService.getProperty(JPUserRegistryScope.class).getValue());
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        try {
            notifyUpdated(requestStatus());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial registry sync failed!", ex), logger, LogLevel.WARN);
        }
    }

    @Override
    public void notifyUpdated(final UserRegistry data) throws CouldNotPerformException {
        userConfigRemoteRegistry.notifyRegistryUpdated(data.getUserConfigList());
        groupConfigRemoteRegistry.notifyRegistryUpdated(data.getUserGroupConfigList());
    }

    public RemoteRegistry<String, UserConfig, UserConfig.Builder, UserRegistry.Builder> getUserConfigRemoteRegistry() {
        return userConfigRemoteRegistry;
    }

    public RemoteRegistry<String, UserGroupConfig, UserGroupConfig.Builder, UserRegistry.Builder> getGroupConfigRemoteRegistry() {
        return groupConfigRemoteRegistry;
    }

    @Override
    public UserConfig registerUserConfig(final UserConfig userConfig) throws CouldNotPerformException {
        try {
            return (UserConfig) callMethod("registerUserConfig", userConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register user config!", ex);
        }
    }

    @Override
    public UserConfig getUserConfigById(String userConfigId) throws CouldNotPerformException, NotAvailableException {
        getData();
        return userConfigRemoteRegistry.getMessage(userConfigId);
    }

    @Override
    public Boolean containsUserConfig(final UserConfig userConfig) throws CouldNotPerformException {
        getData();
        return userConfigRemoteRegistry.contains(userConfig);
    }

    @Override
    public Boolean containsUserConfigById(final String userConfigId) throws CouldNotPerformException {
        getData();
        return userConfigRemoteRegistry.contains(userConfigId);
    }

    @Override
    public UserConfig updateUserConfig(final UserConfig userConfig) throws CouldNotPerformException {
        try {
            return (UserConfig) callMethod("updateUserConfig", userConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update user config[" + userConfig + "]!", ex);
        }
    }

    @Override
    public UserConfig removeUserConfig(final UserConfig userConfig) throws CouldNotPerformException {
        try {
            return (UserConfig) callMethod("removeUserConfig", userConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove user config[" + userConfig + "]!", ex);
        }
    }

    @Override
    public List<UserConfig> getUserConfigs() throws CouldNotPerformException, NotAvailableException {
        getData();
        List<UserConfig> messages = userConfigRemoteRegistry.getMessages();
        return messages;
    }

    @Override
    public Future<Boolean> isUserConfigRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return CompletableFuture.completedFuture(true);
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }
        try {
            return RPCHelper.callRemoteMethod(this, Boolean.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the user config registry!!", ex);
        }
    }

    @Override
    public List<UserConfig> getUserConfigsByUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException {
        getData();
        List<UserConfig> userConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, UserGroupConfig, UserGroupConfig.Builder> group : groupConfigRemoteRegistry.getEntries()) {
            if (group.getMessage().equals(groupConfig)) {
                for (String memeberId : group.getMessage().getMemberIdList()) {
                    userConfigs.add(getUserConfigById(memeberId));
                }
                return userConfigs;
            }
        }
        return userConfigs;
    }

    @Override
    public UserGroupConfig registerUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException {
        try {
            return (UserGroupConfig) callMethod("registerUserGroupConfig", groupConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register group config!", ex);
        }
    }

    @Override
    public Boolean containsUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException {
        getData();
        return groupConfigRemoteRegistry.contains(groupConfig);
    }

    @Override
    public Boolean containsUserGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        getData();
        return groupConfigRemoteRegistry.contains(groupConfigId);
    }

    @Override
    public UserGroupConfig updateUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException {
        try {
            return (UserGroupConfig) callMethod("updateUserGroupConfig", groupConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update group config[" + groupConfig + "]!", ex);
        }
    }

    @Override
    public UserGroupConfig removeUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException {
        try {
            return (UserGroupConfig) callMethod("removeUserGroupConfig", groupConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove group config[" + groupConfig + "]!", ex);
        }
    }

    @Override
    public UserGroupConfig getUserGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        getData();
        return groupConfigRemoteRegistry.getMessage(groupConfigId);
    }

    @Override
    public List<UserGroupConfig> getUserGroupConfigs() throws CouldNotPerformException {
        getData();
        return groupConfigRemoteRegistry.getMessages();
    }

    @Override
    public List<UserGroupConfig> getUserGroupConfigsbyUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        getData();
        List<UserGroupConfig> groupConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, UserGroupConfig, UserGroupConfig.Builder> group : groupConfigRemoteRegistry.getEntries()) {
            group.getMessage().getMemberIdList().stream().filter((memeberId) -> (userConfig.getId().equals(memeberId))).forEach((_item) -> {
                groupConfigs.add(group.getMessage());
            });
        }
        return groupConfigs;
    }

    @Override
    public Future<Boolean> isUserGroupConfigRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return CompletableFuture.completedFuture(true);
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }
        try {
            return RPCHelper.callRemoteMethod(this, Boolean.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the group config registry!!", ex);
        }
    }
}
