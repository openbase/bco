package org.openbase.bco.registry.user.remote;

/*
 * #%L
 * REM UserRegistry Remote
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.registry.user.lib.UserRegistry;
import org.openbase.bco.registry.user.lib.jp.JPUserRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBRemoteService;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.pattern.Remote;
import org.openbase.jul.storage.registry.RemoteRegistry;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.AuthorizationGroupConfigType.AuthorizationGroupConfig;
import rst.authorization.UserConfigType.UserConfig;
import rst.authorization.UserRegistryDataType.UserRegistryData;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserRegistryRemote extends RSBRemoteService<UserRegistryData> implements UserRegistry, Remote<UserRegistryData> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthorizationGroupConfig.getDefaultInstance()));
    }

    private final RemoteRegistry<String, UnitConfig, UnitConfig.Builder, UserRegistryData.Builder> userConfigRemoteRegistry;
    private final RemoteRegistry<String, UnitConfig, UnitConfig.Builder, UserRegistryData.Builder> authorizationGroupConfigRemoteRegistry;

    public UserRegistryRemote() throws InstantiationException, InterruptedException {
        super(UserRegistryData.class);
        try {
            userConfigRemoteRegistry = new RemoteRegistry<>();
            authorizationGroupConfigRemoteRegistry = new RemoteRegistry<>();
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
    public void init(final Scope scope) throws InitializationException, InterruptedException {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        try {
            userConfigRemoteRegistry.shutdown();
            authorizationGroupConfigRemoteRegistry.shutdown();
        } finally {
            super.shutdown();
        }
    }

    @Override
    public void notifyDataUpdate(final UserRegistryData data) throws CouldNotPerformException {
        userConfigRemoteRegistry.notifyRegistryUpdate(data.getUserUnitConfigList());
        authorizationGroupConfigRemoteRegistry.notifyRegistryUpdate(data.getAuthorizationGroupUnitConfigList());
    }

    public RemoteRegistry<String, UnitConfig, UnitConfig.Builder, UserRegistryData.Builder> getUserConfigRemoteRegistry() {
        return userConfigRemoteRegistry;
    }

    public RemoteRegistry<String, UnitConfig, UnitConfig.Builder, UserRegistryData.Builder> getGroupConfigRemoteRegistry() {
        return authorizationGroupConfigRemoteRegistry;
    }

    @Override
    public Future<UnitConfig> registerUserConfig(final UnitConfig userConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(userConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register user config!", ex);
        }
    }

    @Override
    public UnitConfig getUserConfigById(String userConfigId) throws CouldNotPerformException, NotAvailableException {
        validateData();
        return userConfigRemoteRegistry.getMessage(userConfigId);
    }

    @Override
    public Boolean containsUserConfig(final UnitConfig userConfig) throws CouldNotPerformException {
        validateData();
        return userConfigRemoteRegistry.contains(userConfig);
    }

    @Override
    public Boolean containsUserConfigById(final String userConfigId) throws CouldNotPerformException {
        validateData();
        return userConfigRemoteRegistry.contains(userConfigId);
    }

    @Override
    public Future<UnitConfig> updateUserConfig(final UnitConfig userConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(userConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update user config[" + userConfig + "]!", ex);
        }
    }

    @Override
    public Future<UnitConfig> removeUserConfig(final UnitConfig userConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(userConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove user config[" + userConfig + "]!", ex);
        }
    }

    @Override
    public List<UnitConfig> getUserConfigs() throws CouldNotPerformException, NotAvailableException {
        validateData();
        List<UnitConfig> messages = userConfigRemoteRegistry.getMessages();
        return messages;
    }

    @Override
    public Boolean isUserConfigRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        validateData();
        return getData().getUserUnitConfigRegistryReadOnly();
    }

    @Override
    public List<UnitConfig> getUserConfigsByAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        List<UnitConfig> userConfigs = new ArrayList<>();
        for (UnitConfig group : getAuthorizationGroupConfigs()) {
            if (group.getAuthorizationGroupConfig().equals(groupConfig)) {
                for (String memeberId : group.getAuthorizationGroupConfig().getMemberIdList()) {
                    userConfigs.add(getUserConfigById(memeberId));
                }
                return userConfigs;
            }
        }
        return userConfigs;
    }

    @Override
    public Future<UnitConfig> registerAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(groupConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register group config!", ex);
        }
    }

    @Override
    public Boolean containsAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        validateData();
        return authorizationGroupConfigRemoteRegistry.contains(groupConfig);
    }

    @Override
    public Boolean containsAuthorizationGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        validateData();
        return authorizationGroupConfigRemoteRegistry.contains(groupConfigId);
    }

    @Override
    public Future<UnitConfig> updateAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(groupConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update group config[" + groupConfig + "]!", ex);
        }
    }

    @Override
    public Future<UnitConfig> removeAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(groupConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove group config[" + groupConfig + "]!", ex);
        }
    }

    @Override
    public UnitConfig getAuthorizationGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        validateData();
        return authorizationGroupConfigRemoteRegistry.getMessage(groupConfigId);
    }

    @Override
    public List<UnitConfig> getAuthorizationGroupConfigs() throws CouldNotPerformException {
        validateData();
        return authorizationGroupConfigRemoteRegistry.getMessages();
    }

    @Override
    public List<UnitConfig> getAuthorizationGroupConfigsbyUserConfig(UnitConfig userConfig) throws CouldNotPerformException {
        validateData();
        List<UnitConfig> groupConfigs = new ArrayList<>();
        for (UnitConfig group : getAuthorizationGroupConfigs()) {
            group.getAuthorizationGroupConfig().getMemberIdList().stream().filter((memeberId) -> (userConfig.getId().equals(memeberId))).forEach((_item) -> {
                groupConfigs.add(group);
            });
        }
        return groupConfigs;
    }

    @Override
    public Boolean isAuthorizationGroupConfigRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getAuthorizationGroupUnitConfigRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isUserConfigRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getUserUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isAuthorizationGroupConfigRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getAuthorizationGroupUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }
}
