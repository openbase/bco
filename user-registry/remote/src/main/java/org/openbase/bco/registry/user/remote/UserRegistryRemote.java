package org.openbase.bco.registry.user.remote;

/*
 * #%L
 * BCO Registry User Remote
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

import org.openbase.bco.authentication.lib.AuthorizationFilter;
import org.openbase.bco.registry.lib.com.AbstractVirtualRegistryRemote;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.lib.com.future.RegistrationFuture;
import org.openbase.bco.registry.lib.com.future.RemovalFuture;
import org.openbase.bco.registry.lib.com.future.UpdateFuture;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.bco.registry.user.lib.UserRegistry;
import org.openbase.bco.registry.user.lib.jp.JPUserRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.pattern.MockUpFilter;
import org.openbase.jul.pattern.Remote;
import org.openbase.jul.storage.registry.RemoteRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.registry.UserRegistryDataType.UserRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.authorizationgroup.AuthorizationGroupConfigType.AuthorizationGroupConfig;
import rst.domotic.unit.user.UserConfigType.UserConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserRegistryRemote extends AbstractVirtualRegistryRemote<UserRegistryData> implements UserRegistry, Remote<UserRegistryData> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthorizationGroupConfig.getDefaultInstance()));
    }

    private final AuthorizationFilter authorizationFilter;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> userConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> authorizationGroupConfigRemoteRegistry;

    private UnitRegistryRemote unitRegistry;

    public UserRegistryRemote() throws InstantiationException, InterruptedException {
        super(JPUserRegistryScope.class, UserRegistryData.class);
        try {
            authorizationFilter = new AuthorizationFilter();

            userConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, authorizationFilter, UserRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER);
            authorizationGroupConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, new MockUpFilter(), UserRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * {@inheritDoc }
     *
     * @throws InterruptedException     {@inheritDoc }
     * @throws CouldNotPerformException {@inheritDoc }
     */
    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        if (!CachedUserRegistryRemote.getRegistry().equals(this)) {
            logger.warn("You are using a " + getClass().getSimpleName() + " which is not maintained by the global registry singelton! This is extremely inefficient! Please use \"Registries.get" + getClass().getSimpleName().replace("Remote", "") + "()\" instead creating your own instances!");
        }
        authorizationFilter.setAuthorizationGroups(unitRegistry.getAuthorizationGroupUnitConfigRemoteRegistry().getEntryMap());
        authorizationFilter.setLocations(unitRegistry.getLocationUnitConfigRemoteRegistry().getEntryMap());
        super.activate();
    }

    @Override
    protected void registerRemoteRegistries() throws CouldNotPerformException {
        registerRemoteRegistry(authorizationGroupConfigRemoteRegistry);
        registerRemoteRegistry(userConfigRemoteRegistry);
    }

    @Override
    protected void registerRegistryRemotes() throws InitializationException, InterruptedException {
        try {
            unitRegistry = CachedUnitRegistryRemote.getRegistry();
            registerRegistryRemote(unitRegistry);
        } catch (NotAvailableException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void bindRegistryRemoteToRemoteRegistries() {
        try {
            bindRegistryRemoteToRemoteRegistry(userConfigRemoteRegistry, unitRegistry, UnitRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER);
            bindRegistryRemoteToRemoteRegistry(authorizationGroupConfigRemoteRegistry, unitRegistry, UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER);
        } catch (CouldNotPerformException ex) {
            new FatalImplementationErrorException("Could not bind registries", this, ex);
        }
    }

    public RemoteRegistry<String, UnitConfig, UnitConfig.Builder> getUserConfigRemoteRegistry() {
        return userConfigRemoteRegistry;
    }

    public RemoteRegistry<String, UnitConfig, UnitConfig.Builder> getGroupConfigRemoteRegistry() {
        return authorizationGroupConfigRemoteRegistry;
    }

    @Override
    public Future<UnitConfig> registerUserConfig(final UnitConfig userConfig) throws CouldNotPerformException {
        try {
            return new RegistrationFuture<>(RPCHelper.callRemoteMethod(userConfig, this, UnitConfig.class), userConfigRemoteRegistry, this);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register user config!", ex);
        }
    }

    @Override
    public UnitConfig getUserConfigById(String userConfigId) throws CouldNotPerformException, NotAvailableException {
        validateData();
        return userConfigRemoteRegistry.getMessage(userConfigId);
    }

    /**
     * Retrieves a user ID according to a given user name.
     * If multiple users happen to have the same user name, the first one is returned.
     *
     * @param userName
     * @return User ID
     * @throws CouldNotPerformException
     * @throws NotAvailableException    If no user with the given user name could be found.
     * @deprecated removed out of security reasons.
     */
    @Deprecated
    public UnitConfig getUserConfigByUserName(final String userName) throws CouldNotPerformException, NotAvailableException {
        validateData();
        List<UnitConfig> messages = userConfigRemoteRegistry.getMessages();

        for (UnitConfig message : messages) {
            if (message.getUserConfig().getUserName().equals(userName)) {
                return message;
            }
        }

        throw new NotAvailableException(userName);
    }

    @Override
    public String getUserIdByUserName(final String userName) throws CouldNotPerformException, NotAvailableException {
        validateData();
        List<UnitConfig> messages = userConfigRemoteRegistry.getMessages();

        for (UnitConfig message : messages) {
            if (message.getUserConfig().getUserName().equals(userName)) {
                return message.getId();
            }
        }

        throw new NotAvailableException(userName);
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
            return new UpdateFuture<>(RPCHelper.callRemoteMethod(userConfig, this, UnitConfig.class), userConfigRemoteRegistry, this);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update user config[" + userConfig + "]!", ex);
        }
    }

    @Override
    public Future<UnitConfig> removeUserConfig(final UnitConfig userConfig) throws CouldNotPerformException {
        try {
            return new RemovalFuture<>(RPCHelper.callRemoteMethod(userConfig, this, UnitConfig.class), userConfigRemoteRegistry, this);
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
            return new RegistrationFuture<>(RPCHelper.callRemoteMethod(groupConfig, this, UnitConfig.class), authorizationGroupConfigRemoteRegistry, this);
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
            return new UpdateFuture<>(RPCHelper.callRemoteMethod(groupConfig, this, UnitConfig.class), authorizationGroupConfigRemoteRegistry, this);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update group config[" + groupConfig + "]!", ex);
        }
    }

    @Override
    public Future<UnitConfig> removeAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        try {
            return new RemovalFuture<>(RPCHelper.callRemoteMethod(groupConfig, this, UnitConfig.class), authorizationGroupConfigRemoteRegistry, this);
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

    @Override
    public Boolean isConsistent() throws CouldNotPerformException {
        return isAuthorizationGroupConfigRegistryConsistent() && isUserConfigRegistryConsistent();
    }
}
