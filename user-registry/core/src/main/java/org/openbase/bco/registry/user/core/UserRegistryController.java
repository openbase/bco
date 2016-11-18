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
import org.openbase.bco.registry.lib.com.AbstractVirtualRegistryController;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.bco.registry.user.lib.UserRegistry;
import org.openbase.bco.registry.user.lib.jp.JPUserRegistryScope;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.DefaultInitializable;
import org.openbase.jul.iface.Launchable;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.unit.authorizationgroup.AuthorizationGroupConfigType.AuthorizationGroupConfig;
import rst.domotic.unit.user.UserConfigType.UserConfig;
import rst.domotic.registry.UserRegistryDataType.UserRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType.Scope;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserRegistryController extends AbstractVirtualRegistryController<UserRegistryData, UserRegistryData.Builder, UnitRegistryData> implements UserRegistry, Launchable<Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthorizationGroupConfig.getDefaultInstance()));
    }

    private final UnitRegistryRemote unitRegistryRemote;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> userUnitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> authorizationGroupUnitConfigRemoteRegistry;

    public UserRegistryController() throws InstantiationException {
        super(JPUserRegistryScope.class, UserRegistryData.newBuilder());
        this.unitRegistryRemote = new UnitRegistryRemote();
        this.userUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(unitRegistryRemote, UnitRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER);
        this.authorizationGroupUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(unitRegistryRemote, UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER);
    }

    @Override
    protected void syncVirtualRegistryFields(final UserRegistryData.Builder virtualDataBuilder, final UnitRegistryData realData) throws CouldNotPerformException {
        virtualDataBuilder.clearUserUnitConfig();
        virtualDataBuilder.addAllUserUnitConfig(realData.getUserUnitConfigList());

        virtualDataBuilder.clearAuthorizationGroupUnitConfig();
        virtualDataBuilder.addAllAuthorizationGroupUnitConfig(realData.getAuthorizationGroupUnitConfigList());

        virtualDataBuilder.setUserUnitConfigRegistryConsistent(realData.getUserUnitConfigRegistryConsistent());
        virtualDataBuilder.setUserUnitConfigRegistryReadOnly(realData.getUserUnitConfigRegistryReadOnly());

        virtualDataBuilder.setAuthorizationGroupUnitConfigRegistryConsistent(realData.getAuthorizationGroupUnitConfigRegistryConsistent());
        virtualDataBuilder.setAuthorizationGroupUnitConfigRegistryReadOnly(realData.getAuthorizationGroupUnitConfigRegistryReadOnly());
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRegistryRemotes() throws CouldNotPerformException {
        registerRegistryRemote(unitRegistryRemote);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRemoteRegistries() throws CouldNotPerformException {
        registerRemoteRegistry(userUnitConfigRemoteRegistry);
        registerRemoteRegistry(authorizationGroupUnitConfigRemoteRegistry);
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(UserRegistry.class, this, server);
    }

    private void verifyAuthorizationGroupUnitConfig(UnitConfig unitConfig) throws VerificationFailedException {
        UnitConfigProcessor.verifyUnitConfig(unitConfig, UnitType.AUTHORIZATION_GROUP);
    }

    private void verifyUserUnitConfig(UnitConfig unitConfig) throws VerificationFailedException {
        UnitConfigProcessor.verifyUnitConfig(unitConfig, UnitType.USER);
    }

    @Override
    public Future<UnitConfig> registerUserConfig(UnitConfig userConfig) throws CouldNotPerformException {
        verifyUserUnitConfig(userConfig);
        return unitRegistryRemote.registerUnitConfig(userConfig);
    }

    @Override
    public Boolean containsUserConfig(UnitConfig userConfig) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return userUnitConfigRemoteRegistry.contains(userConfig);
    }

    @Override
    public Boolean containsUserConfigById(String userConfigId) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return userUnitConfigRemoteRegistry.contains(userConfigId);
    }

    @Override
    public Future<UnitConfig> updateUserConfig(UnitConfig userConfig) throws CouldNotPerformException {
        verifyUserUnitConfig(userConfig);
        return unitRegistryRemote.updateUnitConfig(userConfig);
    }

    @Override
    public Future<UnitConfig> removeUserConfig(UnitConfig userConfig) throws CouldNotPerformException {
        verifyUserUnitConfig(userConfig);
        return unitRegistryRemote.removeUnitConfig(userConfig);
    }

    @Override
    public UnitConfig getUserConfigById(String userConfigId) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return userUnitConfigRemoteRegistry.getMessage(userConfigId);
    }

    @Override
    public List<UnitConfig> getUserConfigs() throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return userUnitConfigRemoteRegistry.getMessages();
    }

    @Override
    public Boolean isUserConfigRegistryReadOnly() throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return unitRegistryRemote.isUserUnitRegistryReadOnly();
    }

    @Override
    public List<UnitConfig> getUserConfigsByAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        verifyAuthorizationGroupUnitConfig(groupConfig);
        List<UnitConfig> userConfigs = new ArrayList<>();
        for (UnitConfig group : getAuthorizationGroupConfigs()) {
            if (group.equals(groupConfig)) {
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
        verifyAuthorizationGroupUnitConfig(groupConfig);
        return unitRegistryRemote.registerUnitConfig(groupConfig);
    }

    @Override
    public Boolean containsAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return authorizationGroupUnitConfigRemoteRegistry.contains(groupConfig);
    }

    @Override
    public Boolean containsAuthorizationGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return authorizationGroupUnitConfigRemoteRegistry.contains(groupConfigId);
    }

    @Override
    public Future<UnitConfig> updateAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        verifyAuthorizationGroupUnitConfig(groupConfig);
        return unitRegistryRemote.updateUnitConfig(groupConfig);
    }

    @Override
    public Future<UnitConfig> removeAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
        verifyAuthorizationGroupUnitConfig(groupConfig);
        return unitRegistryRemote.removeUnitConfig(groupConfig);
    }

    @Override
    public UnitConfig getAuthorizationGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return authorizationGroupUnitConfigRemoteRegistry.getMessage(groupConfigId);
    }

    @Override
    public List<UnitConfig> getAuthorizationGroupConfigs() throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return authorizationGroupUnitConfigRemoteRegistry.getMessages();
    }

    @Override
    public List<UnitConfig> getAuthorizationGroupConfigsbyUserConfig(UnitConfig userConfig) throws CouldNotPerformException {
        verifyUserUnitConfig(userConfig);
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
        unitRegistryRemote.validateData();
        return unitRegistryRemote.isAuthorizationGroupUnitRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isUserConfigRegistryConsistent() throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return unitRegistryRemote.isUserUnitRegistryConsistent();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isAuthorizationGroupConfigRegistryConsistent() throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return unitRegistryRemote.isAuthorizationGroupUnitRegistryConsistent();
    }
}
