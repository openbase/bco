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
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.bco.registry.user.lib.UserRegistry;
import org.openbase.bco.registry.user.lib.jp.JPUserRegistryScope;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.storage.registry.RemoteRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.AuthorizationGroupConfigType.AuthorizationGroupConfig;
import rst.authorization.UserConfigType.UserConfig;
import rst.authorization.UserRegistryDataType.UserRegistryData;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitRegistryDataType.UnitRegistryData;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserRegistryController extends AbstractVirtualRegistryController<UserRegistryData, UserRegistryData.Builder> implements UserRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthorizationGroupConfig.getDefaultInstance()));
    }

    private final UnitRegistryRemote unitRegistryRemote;
    private final RemoteRegistry<String, UnitConfig, UnitConfig.Builder, UserRegistryData.Builder> userUnitConfigRemoteRegistry;
    private final RemoteRegistry<String, UnitConfig, UnitConfig.Builder, UserRegistryData.Builder> authorizationGroupUnitConfigRemoteRegistry;

    public UserRegistryController() throws InstantiationException, InterruptedException {
        super(JPUserRegistryScope.class, UserRegistryData.newBuilder());
        this.unitRegistryRemote = new UnitRegistryRemote();
        this.userUnitConfigRemoteRegistry = new RemoteRegistry<>();
        this.authorizationGroupUnitConfigRemoteRegistry = new RemoteRegistry<>();
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        super.init();
        unitRegistryRemote.addDataObserver(new Observer<UnitRegistryData>() {

            @Override
            public void update(Observable<UnitRegistryData> source, UnitRegistryData data) throws Exception {
                userUnitConfigRemoteRegistry.notifyRegistryUpdate(data.getUserUnitConfigList());
                setDataField(UserRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER, data.getUserUnitConfigList());
                setDataField(UserRegistryData.USER_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, data.getUserUnitConfigRegistryConsistent());
                setDataField(UserRegistryData.USER_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, data.getUserUnitConfigRegistryReadOnly());

                authorizationGroupUnitConfigRemoteRegistry.notifyRegistryUpdate(data.getAuthorizationGroupUnitConfigList());
                setDataField(UserRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER, data.getAuthorizationGroupUnitConfigList());
                setDataField(UserRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, data.getAuthorizationGroupUnitConfigRegistryConsistent());
                setDataField(UserRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, data.getAuthorizationGroupUnitConfigRegistryReadOnly());
                notifyChange();
            }
        });
    }

    @Override
    public void shutdown() {
        super.shutdown();
        userUnitConfigRemoteRegistry.shutdown();
        authorizationGroupUnitConfigRemoteRegistry.shutdown();
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
    public Future<UnitConfig> registerUserConfig(UnitConfig userConfig) throws CouldNotPerformException {
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
        return unitRegistryRemote.updateUnitConfig(userConfig);
    }

    @Override
    public Future<UnitConfig> removeUserConfig(UnitConfig userConfig) throws CouldNotPerformException {
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
        return getData().getUserUnitConfigRegistryReadOnly();
    }

    @Override
    public List<UnitConfig> getUserConfigsByAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
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
        return unitRegistryRemote.updateUnitConfig(groupConfig);
    }

    @Override
    public Future<UnitConfig> removeAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException {
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
        unitRegistryRemote.validateData();
        return getData().getUserUnitConfigRegistryConsistent();
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
        return getData().getAuthorizationGroupUnitConfigRegistryConsistent();
    }
}
