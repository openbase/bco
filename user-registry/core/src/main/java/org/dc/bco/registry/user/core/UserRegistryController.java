/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.user.core;

/*
 * #%L
 * REM UserRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.dc.bco.registry.user.core.consistency.UserConfigScopeConsistencyHandler;
import org.dc.bco.registry.user.core.consistency.UserConfigUserNameConsistencyHandler;
import org.dc.bco.registry.user.core.consistency.UserGroupConfigLabelConsistencyHandler;
import org.dc.bco.registry.user.core.consistency.UserGroupConfigScopeConsistencyHandler;
import org.dc.bco.registry.user.lib.generator.UserConfigIdGenerator;
import org.dc.bco.registry.user.lib.generator.UserGroupConfigIdGenerator;
import org.dc.bco.registry.user.lib.jp.JPUserConfigDatabaseDirectory;
import org.dc.bco.registry.user.lib.jp.JPUserGroupConfigDatabaseDirectory;
import org.dc.bco.registry.user.lib.jp.JPUserRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBCommunicationService;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.dc.jul.iface.Manageable;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.dc.jul.storage.file.ProtoBufJSonFileProvider;
import org.dc.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
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
public class UserRegistryController extends RSBCommunicationService<UserRegistry, UserRegistry.Builder> implements org.dc.bco.registry.user.lib.UserRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserGroupConfig.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, UserConfig, UserConfig.Builder, UserRegistry.Builder> userRegistry;
    private ProtoBufFileSynchronizedRegistry<String, UserGroupConfig, UserGroupConfig.Builder, UserRegistry.Builder> userGroupRegistry;

    public UserRegistryController() throws InstantiationException, InterruptedException {
        super(UserRegistry.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            userRegistry = new ProtoBufFileSynchronizedRegistry<>(UserConfig.class, getBuilderSetup(), getFieldDescriptor(UserRegistry.USER_CONFIG_FIELD_NUMBER), new UserConfigIdGenerator(), JPService.getProperty(JPUserConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            userGroupRegistry = new ProtoBufFileSynchronizedRegistry<>(UserGroupConfig.class, getBuilderSetup(), getFieldDescriptor(UserRegistry.USER_GROUP_CONFIG_FIELD_NUMBER), new UserGroupConfigIdGenerator(), JPService.getProperty(JPUserGroupConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            userRegistry.loadRegistry();

            userRegistry.registerConsistencyHandler(new UserConfigUserNameConsistencyHandler());
            userRegistry.registerConsistencyHandler(new UserConfigScopeConsistencyHandler());

            userGroupRegistry.loadRegistry();

            userGroupRegistry.registerConsistencyHandler(new UserGroupConfigLabelConsistencyHandler());
            userGroupRegistry.registerConsistencyHandler(new UserGroupConfigScopeConsistencyHandler());

            userRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, UserConfig, UserConfig.Builder>>>() {

                @Override
                public void update(final Observable<Map<String, IdentifiableMessage<String, UserConfig, UserConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UserConfig, UserConfig.Builder>> data) throws Exception {
                    notifyChange();
                }
            });

            userGroupRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, UserGroupConfig, UserGroupConfig.Builder>>>() {

                @Override
                public void update(final Observable<Map<String, IdentifiableMessage<String, UserGroupConfig, UserGroupConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UserGroupConfig, UserGroupConfig.Builder>> data) throws Exception {
                    notifyChange();
                }
            });

        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            super.init(JPService.getProperty(JPUserRegistryScope.class).getValue());
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            super.activate();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate location registry!", ex);
        }

        try {
            userRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
        }

        try {
            userGroupRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
    }

    @Override
    public void shutdown() {
        if (userRegistry != null) {
            userRegistry.shutdown();
        }

        if (userGroupRegistry != null) {
            userGroupRegistry.shutdown();
        }

        try {
            deactivate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }

    @Override
    public final void notifyChange() throws CouldNotPerformException {
        // sync read only flags
        setField(UserRegistry.USER_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, userRegistry.isReadOnly());
        setField(UserRegistry.GROUP_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, userGroupRegistry.isReadOnly());
        super.notifyChange();
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(org.dc.bco.registry.user.lib.UserRegistry.class, this, server);
    }

    public ProtoBufFileSynchronizedRegistry<String, UserConfig, UserConfig.Builder, UserRegistry.Builder> getUserRegistry() {
        return userRegistry;
    }

    public ProtoBufFileSynchronizedRegistry<String, UserGroupConfig, UserGroupConfig.Builder, UserRegistry.Builder> getGroupRegistry() {
        return userGroupRegistry;
    }

    @Override
    public UserConfig registerUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return userRegistry.register(userConfig);
    }

    @Override
    public Boolean containsUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return userRegistry.contains(userConfig);
    }

    @Override
    public Boolean containsUserConfigById(String userConfigId) throws CouldNotPerformException {
        return userRegistry.contains(userConfigId);
    }

    @Override
    public UserConfig updateUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return userRegistry.update(userConfig);
    }

    @Override
    public UserConfig removeUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return userRegistry.remove(userConfig);
    }

    @Override
    public UserConfig getUserConfigById(String userConfigId) throws CouldNotPerformException {
        return userRegistry.get(userConfigId).getMessage();
    }

    @Override
    public List<UserConfig> getUserConfigs() throws CouldNotPerformException {
        return userRegistry.getMessages();
    }

    @Override
    public Future<Boolean> isUserConfigRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(userRegistry.isReadOnly());
    }

    @Override
    public List<UserConfig> getUserConfigsByUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException {
        List<UserConfig> userConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, UserGroupConfig, UserGroupConfig.Builder> group : userGroupRegistry.getEntries()) {
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
        return userGroupRegistry.register(groupConfig);
    }

    @Override
    public Boolean containsUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException {
        return userGroupRegistry.contains(groupConfig);
    }

    @Override
    public Boolean containsUserGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        return userGroupRegistry.contains(groupConfigId);
    }

    @Override
    public UserGroupConfig updateUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException {
        return userGroupRegistry.update(groupConfig);
    }

    @Override
    public UserGroupConfig removeUserGroupConfig(UserGroupConfig groupConfig) throws CouldNotPerformException {
        return userGroupRegistry.remove(groupConfig);
    }

    @Override
    public UserGroupConfig getUserGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        return userGroupRegistry.get(groupConfigId).getMessage();
    }

    @Override
    public List<UserGroupConfig> getUserGroupConfigs() throws CouldNotPerformException {
        return userGroupRegistry.getMessages();
    }

    @Override
    public List<UserGroupConfig> getUserGroupConfigsbyUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        List<UserGroupConfig> groupConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, UserGroupConfig, UserGroupConfig.Builder> group : userGroupRegistry.getEntries()) {
            group.getMessage().getMemberIdList().stream().filter((memeberId) -> (userConfig.getId().equals(memeberId))).forEach((_item) -> {
                groupConfigs.add(group.getMessage());
            });
        }
        return groupConfigs;
    }

    @Override
    public Future<Boolean> isUserGroupConfigRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(userGroupRegistry.isReadOnly());
    }
}
