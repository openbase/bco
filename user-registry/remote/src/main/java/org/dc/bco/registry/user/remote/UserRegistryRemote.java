/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.user.remote;

/*
 * #%L
 * REM UserRegistry Remote
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.dc.bco.registry.user.lib.jp.JPUserRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.preset.JPReadOnly;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBRemoteService;
import org.dc.jul.extension.rsb.scope.ScopeTransformer;
import org.dc.jul.pattern.Remote;
import org.dc.jul.storage.registry.RemoteRegistry;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.UserConfigType.UserConfig;
import rst.authorization.UserGroupConfigType.UserGroupConfig;
import rst.authorization.UserRegistryType.UserRegistry;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 */
public class UserRegistryRemote extends RSBRemoteService<UserRegistry> implements org.dc.bco.registry.user.lib.UserRegistry, Remote<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserGroupConfig.getDefaultInstance()));
    }

    private final RemoteRegistry<String, UserConfig, UserConfig.Builder, UserRegistry.Builder> userConfigRemoteRegistry;
    private final RemoteRegistry<String, UserGroupConfig, UserGroupConfig.Builder, UserRegistry.Builder> groupConfigRemoteRegistry;

    public UserRegistryRemote() throws InstantiationException, InterruptedException {
        try {
            userConfigRemoteRegistry = new RemoteRegistry<>();
            groupConfigRemoteRegistry = new RemoteRegistry<>();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Method initializes the remote with the given scope for the server
     * registry connection.
     *
     * @param scope
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException
     */
    @Override
    public  void init(final Scope scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeTransformer.transform(scope));
        } catch (CouldNotTransformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * Method initializes the remote with the given scope for the server
     * registry connection.
     *
     * @param scope
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException
     */
    @Override
    public synchronized void init(final ScopeType.Scope scope) throws InitializationException, InterruptedException {
        super.init(scope);
    }


    /**
     * Method initializes the remote with the default registry connection scope.
     *
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    public void init() throws InitializationException, InterruptedException {
        try {
            this.init(JPService.getProperty(JPUserRegistryScope.class).getValue());
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
