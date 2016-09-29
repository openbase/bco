package org.openbase.bco.registry.user.core;

/*
 * #%L
 * REM UserRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.concurrent.Future;
import org.openbase.bco.registry.lib.controller.AbstractVirtualRegistryController;
import org.openbase.bco.registry.user.lib.UserRegistry;
import org.openbase.bco.registry.user.lib.jp.JPUserRegistryScope;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.schedule.GlobalExecutionService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.AuthorizationGroupConfigType.AuthorizationGroupConfig;
import rst.authorization.UserConfigType.UserConfig;
import rst.authorization.UserRegistryDataType.UserRegistryData;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 */
public class UserRegistryController extends AbstractVirtualRegistryController<UserRegistryData, UserRegistryData.Builder> implements UserRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthorizationGroupConfig.getDefaultInstance()));
    }

    public UserRegistryController() throws InstantiationException, InterruptedException {
        super(JPUserRegistryScope.class, UserRegistryData.newBuilder());
//        try {
//
//        } catch (JPServiceException | CouldNotPerformException ex) {
//            throw new InstantiationException(this, ex);
//        }
    }
    
    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRegistryRemotes() throws CouldNotPerformException {
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(UserRegistry.class, this, server);
    }

    @Override
    public Future<UserConfig> registerUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> userRegistry.register(userConfig));
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
    public Future<UserConfig> updateUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> userRegistry.update(userConfig));
    }

    @Override
    public Future<UserConfig> removeUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> userRegistry.remove(userConfig));
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
    public Boolean isUserConfigRegistryReadOnly() throws CouldNotPerformException {
        return userRegistry.isReadOnly();
    }

    @Override
    public List<UserConfig> getUserConfigsByAuthorizationGroupConfig(AuthorizationGroupConfig groupConfig) throws CouldNotPerformException {
        List<UserConfig> userConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, AuthorizationGroupConfig, AuthorizationGroupConfig.Builder> group : authorizationGroupRegistry.getEntries()) {
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
    public Future<AuthorizationGroupConfig> registerAuthorizationGroupConfig(AuthorizationGroupConfig groupConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> authorizationGroupRegistry.register(groupConfig));
    }

    @Override
    public Boolean containsAuthorizationGroupConfig(AuthorizationGroupConfig groupConfig) throws CouldNotPerformException {
        return authorizationGroupRegistry.contains(groupConfig);
    }

    @Override
    public Boolean containsAuthorizationGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        return authorizationGroupRegistry.contains(groupConfigId);
    }

    @Override
    public Future<AuthorizationGroupConfig> updateAuthorizationGroupConfig(AuthorizationGroupConfig groupConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> authorizationGroupRegistry.update(groupConfig));
    }

    @Override
    public Future<AuthorizationGroupConfig> removeAuthorizationGroupConfig(AuthorizationGroupConfig groupConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> authorizationGroupRegistry.remove(groupConfig));
    }

    @Override
    public AuthorizationGroupConfig getAuthorizationGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        return authorizationGroupRegistry.get(groupConfigId).getMessage();
    }

    @Override
    public List<AuthorizationGroupConfig> getAuthorizationGroupConfigs() throws CouldNotPerformException {
        return authorizationGroupRegistry.getMessages();
    }

    @Override
    public List<AuthorizationGroupConfig> getAuthorizationGroupConfigsbyUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        List<AuthorizationGroupConfig> groupConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, AuthorizationGroupConfig, AuthorizationGroupConfig.Builder> group : authorizationGroupRegistry.getEntries()) {
            group.getMessage().getMemberIdList().stream().filter((memeberId) -> (userConfig.getId().equals(memeberId))).forEach((_item) -> {
                groupConfigs.add(group.getMessage());
            });
        }
        return groupConfigs;
    }

    @Override
    public Boolean isAuthorizationGroupConfigRegistryReadOnly() throws CouldNotPerformException {
        return authorizationGroupRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isUserConfigRegistryConsistent() throws CouldNotPerformException {
        return userRegistry.isConsistent();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isAuthorizationGroupConfigRegistryConsistent() throws CouldNotPerformException {
        return authorizationGroupRegistry.isConsistent();
    }
}
