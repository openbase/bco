package org.openbase.bco.registry.unit.remote;

/*
 * #%L
 * BCO Registry Unit Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.authentication.lib.AuthenticatedServiceProcessor;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.registry.activity.remote.CachedActivityRegistryRemote;
import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.bco.registry.lib.com.AbstractRegistryRemote;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.lib.jp.JPUnitRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.communication.controller.RPCUtils;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.util.TransactionSynchronizationFuture;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.RegistryRemote;
import org.openbase.jul.storage.registry.RemoteRegistry;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import org.openbase.type.domotic.authentication.AuthorizationTokenType.AuthorizationToken;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.*;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitRegistryRemote extends AbstractRegistryRemote<UnitRegistryData> implements UnitRegistry, RegistryRemote<UnitRegistryData> {

    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> dalUnitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> userUnitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> authorizationGroupUnitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> deviceUnitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> unitGroupUnitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> locationUnitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> connectionUnitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> agentUnitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> sceneUnitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> appUnitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> objectUnitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> gatewayUnitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> unitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> baseUnitConfigRemoteRegistry;

    private final TreeMap<String, String> aliasIdMap;
    private final SyncObject aliasIdMapLock;
    private final Observer<DataProvider<Map<String, IdentifiableMessage<String, UnitConfig, Builder>>>, Map<String, IdentifiableMessage<String, UnitConfig, Builder>>> aliasMapUpdateObserver;

    public UnitRegistryRemote() throws InstantiationException {
        super(JPUnitRegistryScope.class, UnitRegistryData.class);
        try {
            this.aliasIdMap = new TreeMap<>();
            this.aliasIdMapLock = new SyncObject("AliasIdMapLock");

            this.dalUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, UnitRegistryData.DAL_UNIT_CONFIG_FIELD_NUMBER);
            this.userUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, UnitRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER);
            this.authorizationGroupUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER);
            this.deviceUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, UnitRegistryData.DEVICE_UNIT_CONFIG_FIELD_NUMBER);
            this.unitGroupUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, UnitRegistryData.UNIT_GROUP_UNIT_CONFIG_FIELD_NUMBER);
            this.locationUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, UnitRegistryData.LOCATION_UNIT_CONFIG_FIELD_NUMBER);
            this.connectionUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, UnitRegistryData.CONNECTION_UNIT_CONFIG_FIELD_NUMBER);
            this.agentUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, UnitRegistryData.AGENT_UNIT_CONFIG_FIELD_NUMBER);
            this.sceneUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, UnitRegistryData.SCENE_UNIT_CONFIG_FIELD_NUMBER);
            this.appUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, UnitRegistryData.APP_UNIT_CONFIG_FIELD_NUMBER);
            this.objectUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, UnitRegistryData.OBJECT_UNIT_CONFIG_FIELD_NUMBER);
            this.gatewayUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, UnitRegistryData.GATEWAY_UNIT_CONFIG_FIELD_NUMBER);
            this.unitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this,
                    UnitRegistryData.DAL_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.DEVICE_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.UNIT_GROUP_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.LOCATION_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.CONNECTION_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.AGENT_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.SCENE_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.APP_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.OBJECT_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.GATEWAY_UNIT_CONFIG_FIELD_NUMBER
            );
            this.baseUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this,
                    UnitRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.UNIT_GROUP_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.LOCATION_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.CONNECTION_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.AGENT_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.APP_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.SCENE_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.DEVICE_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.GATEWAY_UNIT_CONFIG_FIELD_NUMBER
            );

            aliasMapUpdateObserver = (source, data) -> {
                synchronized (aliasIdMapLock) {
                    aliasIdMap.clear();
                    for (IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage : data.values()) {
                        final UnitConfig unitConfig = identifiableMessage.getMessage();
                        for (String alias : unitConfig.getAliasList()) {
                            aliasIdMap.put(alias.toLowerCase(), unitConfig.getId());
                        }
                    }
                }
            };
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }


    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        CachedActivityRegistryRemote.getRegistry().waitForData();
        CachedClassRegistryRemote.getRegistry().waitForData();
        super.waitForData();
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        unitConfigRemoteRegistry.addDataObserver(aliasMapUpdateObserver);
        unitConfigRemoteRegistry.addDataObserver(clearUnitConfigsByTypeObserver);
        CachedUnitRegistryRemote.getRegistry().addDataObserver(clearUnitConfigsByTypeObserver);
        super.activate();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        unitConfigRemoteRegistry.removeDataObserver(aliasMapUpdateObserver);
        unitConfigRemoteRegistry.removeDataObserver(clearUnitConfigsByTypeObserver);
        CachedUnitRegistryRemote.getRegistry().removeDataObserver(clearUnitConfigsByTypeObserver);
        super.deactivate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerRemoteRegistries() {
        /* ATTENTION: the order here is important, if somebody registers an observer
         * on one of these remote registries and tries to get values from other remote registries
         * which are registered later than these are not synced yet
         */
        registerRemoteRegistry(locationUnitConfigRemoteRegistry);
        registerRemoteRegistry(authorizationGroupUnitConfigRemoteRegistry);
        registerRemoteRegistry(unitConfigRemoteRegistry);
        registerRemoteRegistry(dalUnitConfigRemoteRegistry);
        registerRemoteRegistry(userUnitConfigRemoteRegistry);
        registerRemoteRegistry(deviceUnitConfigRemoteRegistry);
        registerRemoteRegistry(unitGroupUnitConfigRemoteRegistry);
        registerRemoteRegistry(connectionUnitConfigRemoteRegistry);
        registerRemoteRegistry(agentUnitConfigRemoteRegistry);
        registerRemoteRegistry(sceneUnitConfigRemoteRegistry);
        registerRemoteRegistry(appUnitConfigRemoteRegistry);
        registerRemoteRegistry(baseUnitConfigRemoteRegistry);
        registerRemoteRegistry(objectUnitConfigRemoteRegistry);
        registerRemoteRegistry(gatewayUnitConfigRemoteRegistry);
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getDalUnitConfigRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (CouldNotPerformException ex) {
                throw new NotAvailableException("DalUnitConfigRemoteRegistry", ex);
            }
        }
        return dalUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getUserUnitConfigRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (InvalidStateException ex) {
                throw new NotAvailableException("UserUnitConfigRemoteRegistry", ex);
            }
        }
        return userUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getAuthorizationGroupUnitConfigRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (CouldNotPerformException ex) {
                throw new NotAvailableException("AuthorizationGroupUnitConfigRemoteRegistry", ex);
            }
        }
        return authorizationGroupUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getDeviceUnitConfigRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (InvalidStateException ex) {
                throw new NotAvailableException("DeviceUnitConfigRemoteRegistry", ex);
            }
        }
        return deviceUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getUnitGroupUnitConfigRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (InvalidStateException ex) {
                throw new NotAvailableException("UnitGroupUnitConfigRemoteRegistry", ex);
            }
        }
        return unitGroupUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getLocationUnitConfigRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (InvalidStateException ex) {
                throw new NotAvailableException("LocationUnitConfigRemoteRegistry", ex);
            }
        }
        return locationUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getConnectionUnitConfigRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (InvalidStateException ex) {
                throw new NotAvailableException("ConnectionUnitConfigRemoteRegistry", ex);
            }
        }
        return connectionUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getAgentUnitConfigRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (InvalidStateException ex) {
                throw new NotAvailableException("AgentUnitConfigRemoteRegistry", ex);
            }
        }
        return agentUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getSceneUnitConfigRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (InvalidStateException ex) {
                throw new NotAvailableException("SceneUnitConfigRemoteRegistry", ex);
            }
        }
        return sceneUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getAppUnitConfigRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (InvalidStateException ex) {
                throw new NotAvailableException("AppUnitConfigRemoteRegistry", ex);
            }
        }
        return appUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getGatewayUnitConfigRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (InvalidStateException ex) {
                throw new NotAvailableException("GatewayUnitConfigRemoteRegistry", ex);
            }
        }
        return gatewayUnitConfigRemoteRegistry;
    }

    public RemoteRegistry<String, UnitConfig, UnitConfig.Builder> getUnitConfigRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (InvalidStateException ex) {
                throw new NotAvailableException("UnitConfigRemoteRegistry", ex);
            }
        }
        return unitConfigRemoteRegistry;
    }

    public RemoteRegistry<String, UnitConfig, UnitConfig.Builder> getBaseUnitConfigRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (InvalidStateException ex) {
                throw new NotAvailableException("BaseUnitConfigRemoteRegistry", ex);
            }
        }
        return baseUnitConfigRemoteRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfig {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Future<UnitConfig> registerUnitConfig(final UnitConfig unitConfig) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(unitConfig, UnitConfig.class, SessionManager.getInstance(), authenticatedValue -> registerUnitConfigAuthenticated(authenticatedValue));
    }

    @Override
    public Future<AuthenticatedValue> registerUnitConfigAuthenticated(AuthenticatedValue authenticatedValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(authenticatedValue, this, AuthenticatedValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfigId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public UnitConfig getUnitConfigById(final String unitConfigId) throws NotAvailableException {
        try {
            validateData();
            return unitConfigRemoteRegistry.getMessage(unitConfigId);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("UnitConfigId", unitConfigId, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param unitAlias {@inheritDoc}
     * @return {@inheritDoc}
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public UnitConfig getUnitConfigByAlias(final String unitAlias) throws NotAvailableException {
        try {
            synchronized (aliasIdMapLock) {
                if (aliasIdMap.containsKey(unitAlias.toLowerCase())) {
                    return getUnitConfigById(aliasIdMap.get(unitAlias.toLowerCase()));
                }
            }
            throw new NotAvailableException("Alias", unitAlias);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("UnitConfig with Alias", unitAlias, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param unitAlias {@inheritDoc}
     * @param unitType  {@inheritDoc}
     * @return {@inheritDoc}
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public UnitConfig getUnitConfigByAliasAndUnitType(String unitAlias, final UnitType unitType) throws NotAvailableException {
        try {
            synchronized (aliasIdMapLock) {
                if (aliasIdMap.containsKey(unitAlias.toLowerCase())) {
                    return getUnitConfigByIdAndUnitType(aliasIdMap.get(unitAlias.toLowerCase()), unitType);
                }
            }
            throw new NotAvailableException("Alias", unitAlias);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("UnitConfig of UnitType[" + unitType.name() + "] with Alias", unitAlias, ex);
        }
    }

    @Override
    public Boolean containsUnitConfig(final UnitConfig unitConfig) {
        try {
            validateData();
            return unitConfigRemoteRegistry.contains(unitConfig);
        } catch (InvalidStateException ex) {
            return true;
        }
    }

    @Override
    public Boolean containsUnitConfigById(final String unitConfigId) {
        try {
            validateData();
            return unitConfigRemoteRegistry.contains(unitConfigId);
        } catch (InvalidStateException ex) {
            return true;
        }
    }

    @Override
    public Future<UnitConfig> updateUnitConfig(final UnitConfig unitConfig) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(unitConfig, UnitConfig.class, SessionManager.getInstance(), authenticatedValue -> updateUnitConfigAuthenticated(authenticatedValue));
    }

    @Override
    public Future<AuthenticatedValue> updateUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(authenticatedValue, this, AuthenticatedValue.class), this);
    }

    @Override
    public Future<UnitConfig> removeUnitConfig(final UnitConfig unitConfig) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(unitConfig, UnitConfig.class, SessionManager.getInstance(), authenticatedValue -> removeUnitConfigAuthenticated(authenticatedValue));
    }

    @Override
    public Future<AuthenticatedValue> removeUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(authenticatedValue, this, AuthenticatedValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param filterDisabledUnits {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException    {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsFiltered(boolean filterDisabledUnits) throws CouldNotPerformException, NotAvailableException {
        validateData();
        final List<UnitConfig> unitConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : unitConfigRemoteRegistry.getMessages()) {
            if (filterDisabledUnits && !UnitConfigProcessor.isEnabled(unitConfig)) {
                continue;
            }

            unitConfigs.add(unitConfig);
        }
        return unitConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getDalUnitConfigs() throws CouldNotPerformException {
        validateData();
        final List<UnitConfig> unitConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : dalUnitConfigRemoteRegistry.getMessages()) {
            if (!UnitConfigProcessor.isEnabled(unitConfig)) {
                continue;
            }

            unitConfigs.add(unitConfig);
        }
        return unitConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getBaseUnitConfigs() throws CouldNotPerformException {
        validateData();
        final List<UnitConfig> unitConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : baseUnitConfigRemoteRegistry.getMessages()) {
            if (!UnitConfigProcessor.isEnabled(unitConfig)) {
                continue;
            }

            unitConfigs.add(unitConfig);
        }
        return unitConfigs;
    }

    @Override
    public Boolean isUnitConfigRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getUnitConfigRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isUnitConfigRegistryConsistent() {
        try {
            validateData();
            return getData().getUnitConfigRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public List<UnitConfig> getUnitConfigsByLabel(final String unitConfigLabel) throws CouldNotPerformException {
        validateData();
        List<UnitConfig> unitConfigs = Collections.synchronizedList(new ArrayList<>());
        getUnitConfigs().parallelStream().filter((unitConfig) -> LabelProcessor.contains(unitConfig.getLabel(), unitConfigLabel)).forEach((unitConfig) -> {
            unitConfigs.add(unitConfig);
        });
        return unitConfigs;
    }

    @Override
    public List<ServiceConfig> getServiceConfigs() throws CouldNotPerformException {
        validateData();
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            serviceConfigs.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigs;
    }

    @Override
    public List<ServiceConfig> getServiceConfigsByServiceType(final ServiceType serviceType) throws CouldNotPerformException {
        validateData();
        return UnitRegistry.super.getServiceConfigsByServiceType(serviceType);
    }


    @Override
    public Boolean isUnitGroupConfigRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getUnitGroupUnitConfigRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isDalUnitConfigRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getDalUnitConfigRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isUserUnitRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getUserUnitConfigRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isAuthorizationGroupUnitRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getAuthorizationGroupUnitConfigRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isDeviceUnitRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getDeviceUnitConfigRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isUnitGroupUnitRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getUnitGroupUnitConfigRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isLocationUnitRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getLocationUnitConfigRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isConnectionUnitRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getConnectionUnitConfigRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isAgentUnitRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getAgentUnitConfigRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isAppUnitRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getAppUnitConfigRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isSceneUnitRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getSceneUnitConfigRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isObjectUnitRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getObjectUnitConfigRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isGatewayUnitRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getGatewayUnitConfigRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isUnitGroupConfigRegistryConsistent() {
        try {
            validateData();
            return getData().getUnitConfigRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }


    @Override
    public Boolean isDalUnitConfigRegistryConsistent() {
        try {
            validateData();
            return getData().getDalUnitConfigRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isUserUnitRegistryConsistent() {
        try {
            validateData();
            return getData().getUserUnitConfigRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isAuthorizationGroupUnitRegistryConsistent() {
        try {
            validateData();
            return getData().getAuthorizationGroupUnitConfigRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isDeviceUnitRegistryConsistent() {
        try {
            validateData();
            return getData().getDeviceUnitConfigRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isUnitGroupUnitRegistryConsistent() {
        try {
            validateData();
            return getData().getUnitGroupUnitConfigRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isLocationUnitRegistryConsistent() {
        try {
            validateData();
            return getData().getLocationUnitConfigRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isConnectionUnitRegistryConsistent() {
        try {
            validateData();
            return getData().getConnectionUnitConfigRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isAgentUnitRegistryConsistent() {
        try {
            validateData();
            return getData().getAgentUnitConfigRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isAppUnitRegistryConsistent() {
        try {
            validateData();
            return getData().getAppUnitConfigRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isSceneUnitRegistryConsistent() {
        try {
            validateData();
            return getData().getSceneUnitConfigRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isObjectUnitRegistryConsistent() {
        try {
            validateData();
            return getData().getObjectUnitConfigRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isGatewayUnitRegistryConsistent() {
        try {
            validateData();
            return getData().getGatewayUnitConfigRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param authorizationToken {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Future<String> requestAuthorizationToken(final AuthorizationToken authorizationToken) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(
                authorizationToken,
                String.class,
                SessionManager.getInstance(),
                authenticatedValue -> requestAuthorizationTokenAuthenticated(authenticatedValue)
        );
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> requestAuthorizationTokenAuthenticated(final AuthenticatedValue authenticatedValue) {
        return RPCUtils.callRemoteServerMethod(authenticatedValue, this, AuthenticatedValue.class);
    }

    @Override
    public Future<String> requestAuthenticationToken(final AuthenticationToken authenticationToken) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(
                authenticationToken,
                String.class,
                SessionManager.getInstance(),
                authenticatedValue -> requestAuthenticationTokenAuthenticated(authenticatedValue)
        );
    }

    @Override
    public Future<AuthenticatedValue> requestAuthenticationTokenAuthenticated(final AuthenticatedValue authenticatedValue) {
        return RPCUtils.callRemoteServerMethod(authenticatedValue, this, AuthenticatedValue.class);
    }

    @Override
    public Map<String, IdentifiableMessage<String, UnitConfig, Builder>> getAuthorizationGroupMap() {
        return authorizationGroupUnitConfigRemoteRegistry.getEntryMap();
    }

    @Override
    public Map<String, IdentifiableMessage<String, UnitConfig, Builder>> getLocationMap() {
        return locationUnitConfigRemoteRegistry.getEntryMap();
    }

    @Override
    public Boolean isConsistent() {
        return isAgentUnitRegistryConsistent() &&
                isAppUnitRegistryConsistent() &&
                isAuthorizationGroupUnitRegistryConsistent() &&
                isConnectionUnitRegistryConsistent() &&
                isDalUnitConfigRegistryConsistent() &&
                isDeviceUnitRegistryConsistent() &&
                isLocationUnitRegistryConsistent() &&
                isSceneUnitRegistryConsistent() &&
                isUserUnitRegistryConsistent() &&
                isUnitConfigRegistryConsistent() &&
                isUnitGroupConfigRegistryConsistent() &&
                isObjectUnitRegistryConsistent() &&
                isGatewayUnitRegistryConsistent();
    }
}
