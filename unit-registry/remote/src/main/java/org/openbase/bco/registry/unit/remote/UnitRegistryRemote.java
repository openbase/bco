package org.openbase.bco.registry.unit.remote;

/*
 * #%L
 * BCO Registry Unit Remote
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

import com.google.protobuf.ByteString;
import org.openbase.bco.authentication.lib.AuthenticatedServiceProcessor;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.registry.lib.com.AbstractRegistryRemote;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.lib.jp.JPUnitRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.extension.rst.util.TransactionSynchronizationFuture;
import org.openbase.jul.pattern.MockUpFilter;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.RegistryRemote;
import org.openbase.jul.storage.registry.RemoteRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import rst.domotic.authentication.AuthorizationTokenType.AuthorizationToken;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitRegistryRemote extends AbstractRegistryRemote<UnitRegistryData> implements UnitRegistry, RegistryRemote<UnitRegistryData> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitTemplate.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ServiceTemplate.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthenticatedValue.getDefaultInstance()));
    }

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
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> unitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> baseUnitConfigRemoteRegistry;

    private final TreeMap<String, String> aliasIdMap;
    private final SyncObject aliasIdMapLock;

    public UnitRegistryRemote() throws InstantiationException {
        super(JPUnitRegistryScope.class, UnitRegistryData.class);
        try {
            this.aliasIdMap = new TreeMap<>();
            this.aliasIdMapLock = new SyncObject("AliasIdMapLock");

            this.dalUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry(this.getIntenalPriorizedDataObservable(), this, UnitRegistryData.DAL_UNIT_CONFIG_FIELD_NUMBER);
            this.userUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, UnitRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER);
            this.authorizationGroupUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, new MockUpFilter(), UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER);
            this.deviceUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, UnitRegistryData.DEVICE_UNIT_CONFIG_FIELD_NUMBER);
            this.unitGroupUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, UnitRegistryData.UNIT_GROUP_UNIT_CONFIG_FIELD_NUMBER);
            this.locationUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, new MockUpFilter(), UnitRegistryData.LOCATION_UNIT_CONFIG_FIELD_NUMBER);
            this.connectionUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, UnitRegistryData.CONNECTION_UNIT_CONFIG_FIELD_NUMBER);
            this.agentUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, UnitRegistryData.AGENT_UNIT_CONFIG_FIELD_NUMBER);
            this.sceneUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, UnitRegistryData.SCENE_UNIT_CONFIG_FIELD_NUMBER);
            this.appUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, UnitRegistryData.APP_UNIT_CONFIG_FIELD_NUMBER);
            this.unitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this,
                    UnitRegistryData.DAL_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.DEVICE_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.UNIT_GROUP_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.LOCATION_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.CONNECTION_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.AGENT_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.SCENE_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.APP_UNIT_CONFIG_FIELD_NUMBER
            );
            this.baseUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this,
                    UnitRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.UNIT_GROUP_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.LOCATION_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.CONNECTION_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.AGENT_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.APP_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.SCENE_UNIT_CONFIG_FIELD_NUMBER,
                    UnitRegistryData.DEVICE_UNIT_CONFIG_FIELD_NUMBER
            );

            unitConfigRemoteRegistry.addDataObserver((source, data) -> {
                synchronized (aliasIdMapLock) {
                    aliasIdMap.clear();
                    for (IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage : data.values()) {
                        final UnitConfig unitConfig = identifiableMessage.getMessage();
                        unitConfig.getAliasList().forEach(alias -> aliasIdMap.put(alias, unitConfig.getId()));
                    }
                }
            });
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.InitializationException {@inheritDoc}
     */
    @Override
    protected void registerRemoteRegistries() throws CouldNotPerformException {
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
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getDalUnitConfigRemoteRegistry() {
        return dalUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getUserUnitConfigRemoteRegistry() {
        return userUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getAuthorizationGroupUnitConfigRemoteRegistry() {
        return authorizationGroupUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getDeviceUnitConfigRemoteRegistry() {
        return deviceUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getUnitGroupUnitConfigRemoteRegistry() {
        return unitGroupUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getLocationUnitConfigRemoteRegistry() {
        return locationUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getConnectionUnitConfigRemoteRegistry() {
        return connectionUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getAgentUnitConfigRemoteRegistry() {
        return agentUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getSceneUnitConfigRemoteRegistry() {
        return sceneUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getAppUnitConfigRemoteRegistry() {
        return appUnitConfigRemoteRegistry;
    }

    public RemoteRegistry<String, UnitConfig, UnitConfig.Builder> getUnitConfigRemoteRegistry() {
        return unitConfigRemoteRegistry;
    }

    public RemoteRegistry<String, UnitConfig, UnitConfig.Builder> getBaseUnitConfigRemoteRegistry() {
        return baseUnitConfigRemoteRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfig
     * @return
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    @Override
    public Future<UnitConfig> registerUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(unitConfig, UnitConfig.class, SessionManager.getInstance(), this::registerUnitConfigAuthenticated);
    }

    @Override
    public Future<AuthenticatedValue> registerUnitConfigAuthenticated(AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        try {
            return new TransactionSynchronizationFuture<>(RPCHelper.callRemoteMethod(authenticatedValue, this, AuthenticatedValue.class), this);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register unit config!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfigId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException    {@inheritDoc}
     */
    @Override
    public UnitConfig getUnitConfigById(final String unitConfigId) throws CouldNotPerformException, NotAvailableException {
        validateData();
        return unitConfigRemoteRegistry.getMessage(unitConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitAlias {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public UnitConfig getUnitConfigByAlias(String unitAlias) throws CouldNotPerformException {
        synchronized (aliasIdMapLock) {
            if (aliasIdMap.containsKey(unitAlias)) {
                return getUnitConfigById(aliasIdMap.get(unitAlias));
            }
        }
        throw new NotAvailableException("UnitConfig with alias[" + unitAlias + "]");
    }

    @Override
    public Boolean containsUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        validateData();
        return unitConfigRemoteRegistry.contains(unitConfig);
    }

    @Override
    public Boolean containsUnitConfigById(final String unitConfigId) throws CouldNotPerformException {
        validateData();
        return unitConfigRemoteRegistry.contains(unitConfigId);
    }

    @Override
    public Future<UnitConfig> updateUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(unitConfig, UnitConfig.class, SessionManager.getInstance(), this::updateUnitConfigAuthenticated);
    }

    @Override
    public Future<AuthenticatedValue> updateUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        try {
            return new TransactionSynchronizationFuture<>(RPCHelper.callRemoteMethod(authenticatedValue, this, AuthenticatedValue.class), this);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update unit config!", ex);
        }
    }

    @Override
    public Future<UnitConfig> removeUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(unitConfig, UnitConfig.class, SessionManager.getInstance(), this::removeUnitConfigAuthenticated);
    }

    @Override
    public Future<AuthenticatedValue> removeUnitConfigAuthenticated(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        try {
            return new TransactionSynchronizationFuture<>(RPCHelper.callRemoteMethod(authenticatedValue, this, AuthenticatedValue.class), this);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove unit config!", ex);
        }
    }

    @Override
    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException, NotAvailableException {
        validateData();
        return unitConfigRemoteRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getDalUnitConfigs() throws CouldNotPerformException {
        return dalUnitConfigRemoteRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getBaseUnitConfigs() throws CouldNotPerformException {
        return baseUnitConfigRemoteRegistry.getMessages();
    }

    @Override
    public Boolean isUnitConfigRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getUnitConfigRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isUnitConfigRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
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
    public List<ServiceConfig> getServiceConfigs(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType) throws CouldNotPerformException {
        validateData();
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                if (serviceConfig.getServiceDescription().getServiceType() == serviceType) {
                    serviceConfigs.add(serviceConfig);
                }
            }
        }
        return serviceConfigs;
    }

    @Override
    public Boolean isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getUnitGroupUnitConfigRegistryReadOnly();
    }

    @Override
    public Boolean isUnitGroupConfigRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }
//
//    @Override
//    public List<UnitConfig> getUnitGroupConfigsByUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
//        validateData();
//        List<UnitConfig> unitConfigList = new ArrayList<>();
//        for (UnitConfig unitGroupUnitConfig : unitGroupUnitConfigRemoteRegistry.getMessages()) {
//            if (unitGroupUnitConfig.getUnitGroupConfig().getMemberIdList().contains(unitConfig.getId())) {
//                unitConfigList.add(unitGroupUnitConfig);
//            }
//        }
//        return unitConfigList;
//    }
//
//    @Override
//    public List<UnitConfig> getUnitGroupConfigsByUnitType(final UnitTemplate.UnitType type) throws CouldNotPerformException {
//        validateData();
//        List<UnitConfig> unitConfigList = new ArrayList<>();
//        for (UnitConfig unitGroupUnitConfig : unitGroupUnitConfigRemoteRegistry.getMessages()) {
//            if (unitGroupunitConfig.getUnitType() == type || getSubUnitTypesOfUnitType(type).contains(unitGroupunitConfig.getUnitType())) {
//                unitConfigList.add(unitGroupUnitConfig);
//            }
//        }
//        return unitConfigList;
//    }
//
//    @Override
//    public List<UnitConfig> getUnitGroupConfigsByServiceTypes(final List<ServiceTemplateType.ServiceTemplate.ServiceType> serviceTypes) throws CouldNotPerformException {
//        validateData();
//        List<UnitConfig> unitGroups = new ArrayList<>();
//        for (UnitConfig unitGroupUnitConfig : unitGroupUnitConfigRemoteRegistry.getMessages()) {
//            boolean skipGroup = false;
//            for (ServiceDescription serviceDescription : unitGroupUnitConfig.getUnitGroupConfig().getServiceDescriptionList()) {
//                if (!serviceTypes.contains(serviceDescription.getServiceType())) {
//                    skipGroup = true;
//                }
//            }
//            if (skipGroup) {
//                continue;
//            }
//            unitGroups.add(unitGroupUnitConfig);
//        }
//        return unitGroups;
//    }
//
//    @Override
//    public List<UnitConfig> getUnitConfigsByUnitGroupConfig(final UnitConfig unitGroupUnitConfig) throws CouldNotPerformException {
//        validateData();
//        verifyUnitGroupUnitConfig(unitGroupUnitConfig);
//        List<UnitConfig> unitConfigs = new ArrayList<>();
//        for (String unitId : unitGroupUnitConfig.getUnitGroupConfig().getMemberIdList()) {
//            unitConfigs.add(getUnitConfigById(unitId));
//        }
//        return unitConfigs;
//    }
//
//    @Override
//    public List<UnitConfig> getUnitConfigsByUnitTypeAndServiceTypes(final UnitType type, final List<ServiceType> serviceTypes) throws CouldNotPerformException {
//        validateData();
//        final List<UnitConfig> unitConfigs = getUnitConfigs(type);
//        boolean foundServiceType;
//
//        for (UnitConfig unitConfig : new ArrayList<>(unitConfigs)) {
//            foundServiceType = false;
//            for (ServiceTemplateType.ServiceTemplate.ServiceType serviceType : serviceTypes) {
//                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
//                    if (serviceConfig.getServiceDescription().getServiceType() == serviceType) {
//                        foundServiceType = true;
//                    }
//                }
//                if (!foundServiceType) {
//                    unitConfigs.remove(unitConfig);
//                }
//            }
//        }
//        return unitConfigs;
//    }
//
//    @Override
//    public UnitConfig getUnitConfigByScope(ScopeType.Scope scope) throws CouldNotPerformException {
//        if (scope == null) {
//            throw new NotAvailableException("scope");
//        }
//        validateData();
//        for (UnitConfig unitConfig : getUnitConfigs()) {
//            if (unitConfig.getScope().equals(scope)) {
//                return unitConfig;
//            }
//        }
//        throw new NotAvailableException("No unit config available for given Scope[" + ScopeGenerator.generateStringRep(scope) + "]!");
//    }

    @Override
    public Boolean isDalUnitConfigRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getDalUnitConfigRegistryReadOnly();
    }

    @Override
    public Boolean isUserUnitRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getUserUnitConfigRegistryReadOnly();
    }

    @Override
    public Boolean isAuthorizationGroupUnitRegistryReadOnly() throws CouldNotPerformException {
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

    @Override
    public Boolean isDeviceUnitRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getDeviceUnitConfigRegistryReadOnly();
    }

    @Override
    public Boolean isUnitGroupUnitRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getUnitGroupUnitConfigRegistryReadOnly();
    }

    @Override
    public Boolean isLocationUnitRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getLocationUnitConfigRegistryReadOnly();
    }

    @Override
    public Boolean isConnectionUnitRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getConnectionUnitConfigRegistryReadOnly();
    }

    @Override
    public Boolean isAgentUnitRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getAgentUnitConfigRegistryReadOnly();
    }

    @Override
    public Boolean isAppUnitRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getAppUnitConfigRegistryReadOnly();
    }

    @Override
    public Boolean isSceneUnitRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getSceneUnitConfigRegistryReadOnly();
    }

    @Override
    public Boolean isDalUnitConfigRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getDalUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    @Override
    public Boolean isUserUnitRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getUserUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    @Override
    public Boolean isAuthorizationGroupUnitRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getAuthorizationGroupUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    @Override
    public Boolean isDeviceUnitRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getDeviceUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    @Override
    public Boolean isUnitGroupUnitRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getUnitGroupUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    @Override
    public Boolean isLocationUnitRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getLocationUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    @Override
    public Boolean isConnectionUnitRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getConnectionUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    @Override
    public Boolean isAgentUnitRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getAgentUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    @Override
    public Boolean isAppUnitRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getAppUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    @Override
    public Boolean isSceneUnitRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getSceneUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param authorizationToken {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<String> requestAuthorizationToken(final AuthorizationToken authorizationToken) throws CouldNotPerformException {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(
                authorizationToken,
                String.class,
                SessionManager.getInstance(),
                this::requestAuthorizationTokenAuthenticated
        );
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> requestAuthorizationTokenAuthenticated(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(authenticatedValue, this, AuthenticatedValue.class);
    }

    @Override
    public Future<ByteString> requestAuthenticationToken(final AuthenticationToken authenticationToken) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> {
            AuthenticatedValueFuture<String> internalFuture = null;
            try {
                internalFuture = AuthenticatedServiceProcessor.requestAuthenticatedAction(authenticationToken, String.class, SessionManager.getInstance(), this::requestAuthenticationTokenAuthenticated);
                return ByteString.copyFrom(Base64.getDecoder().decode(internalFuture.get()));
            } catch (CouldNotPerformException | ExecutionException ex) {
                throw new CouldNotPerformException("Could not request authentication token", ex);
            } catch (InterruptedException ex) {
                if (!internalFuture.isDone()) {
                    internalFuture.cancel(true);
                }
                Thread.currentThread().interrupt();
                throw new CouldNotPerformException("Could not request authentication token", ex);
            }
        });
    }

    @Override
    public Future<AuthenticatedValue> requestAuthenticationTokenAuthenticated(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(authenticatedValue, this, AuthenticatedValue.class);
    }

    @Override
    public Map<String, IdentifiableMessage<String, UnitConfig, Builder>> getAuthorizationGroupMap() throws CouldNotPerformException {
        return authorizationGroupUnitConfigRemoteRegistry.getEntryMap();
    }

    @Override
    public Map<String, IdentifiableMessage<String, UnitConfig, Builder>> getLocationMap() throws CouldNotPerformException {
        return locationUnitConfigRemoteRegistry.getEntryMap();
    }

    @Override
    public Boolean isConsistent() throws CouldNotPerformException {
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
                isUnitGroupConfigRegistryConsistent();
    }
}
