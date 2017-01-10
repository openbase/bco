package org.openbase.bco.registry.unit.remote;

/*
 * #%L
 * BCO Registry Unit Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
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
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.storage.registry.RegistryRemote;
import org.openbase.jul.storage.registry.RemoteRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceConfigType;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitRegistryRemote extends AbstractRegistryRemote<UnitRegistryData> implements UnitRegistry, RegistryRemote<UnitRegistryData> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitTemplate.getDefaultInstance()));
    }

    private final SynchronizedRemoteRegistry<String, UnitTemplate, UnitTemplate.Builder> unitTemplateRemoteRegistry;
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

    ;

    public UnitRegistryRemote() throws InstantiationException {
        super(JPUnitRegistryScope.class, UnitRegistryData.class);
        try {
            this.unitTemplateRemoteRegistry = new SynchronizedRemoteRegistry<>(this, UnitRegistryData.UNIT_TEMPLATE_FIELD_NUMBER);
            this.dalUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry(this, UnitRegistryData.DAL_UNIT_CONFIG_FIELD_NUMBER);
            this.userUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this, UnitRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER);
            this.authorizationGroupUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this, UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER);
            this.deviceUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this, UnitRegistryData.DEVICE_UNIT_CONFIG_FIELD_NUMBER);
            this.unitGroupUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this, UnitRegistryData.UNIT_GROUP_UNIT_CONFIG_FIELD_NUMBER);
            this.locationUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this, UnitRegistryData.LOCATION_UNIT_CONFIG_FIELD_NUMBER);
            this.connectionUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this, UnitRegistryData.CONNECTION_UNIT_CONFIG_FIELD_NUMBER);
            this.agentUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this, UnitRegistryData.AGENT_UNIT_CONFIG_FIELD_NUMBER);
            this.sceneUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this, UnitRegistryData.SCENE_UNIT_CONFIG_FIELD_NUMBER);
            this.appUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this, UnitRegistryData.APP_UNIT_CONFIG_FIELD_NUMBER);
            this.unitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this,
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
            this.baseUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this,
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
        registerRemoteRegistry(unitTemplateRemoteRegistry);
        registerRemoteRegistry(dalUnitConfigRemoteRegistry);
        registerRemoteRegistry(userUnitConfigRemoteRegistry);
        registerRemoteRegistry(authorizationGroupUnitConfigRemoteRegistry);
        registerRemoteRegistry(deviceUnitConfigRemoteRegistry);
        registerRemoteRegistry(unitGroupUnitConfigRemoteRegistry);
        registerRemoteRegistry(locationUnitConfigRemoteRegistry);
        registerRemoteRegistry(connectionUnitConfigRemoteRegistry);
        registerRemoteRegistry(agentUnitConfigRemoteRegistry);
        registerRemoteRegistry(sceneUnitConfigRemoteRegistry);
        registerRemoteRegistry(appUnitConfigRemoteRegistry);
        registerRemoteRegistry(unitConfigRemoteRegistry);
        registerRemoteRegistry(baseUnitConfigRemoteRegistry);
    }

    @Override
    protected void notifyDataUpdate(UnitRegistryData data) throws CouldNotPerformException {
        super.notifyDataUpdate(data);
    }

    // todo: sync unitConfigRemoteRegistry
    public SynchronizedRemoteRegistry<String, UnitTemplate, UnitTemplate.Builder> getUnitTemplateRemoteRegistry() {
        return unitTemplateRemoteRegistry;
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
        try {
            return RPCHelper.callRemoteMethod(unitConfig, this, UnitConfig.class);
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
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public UnitConfig getUnitConfigById(final String unitConfigId) throws CouldNotPerformException, NotAvailableException {
        validateData();
        return unitConfigRemoteRegistry.getMessage(unitConfigId);
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
        try {
            return RPCHelper.callRemoteMethod(unitConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update unit config!", ex);
        }
    }

    @Override
    public Future<UnitConfig> removeUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(unitConfig, this, UnitConfig.class);
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
    public Future<UnitTemplate> updateUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(unitTemplate, this, UnitTemplate.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update unit template!", ex);
        }
    }

    @Override
    public Boolean containsUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException {
        validateData();
        return unitTemplateRemoteRegistry.contains(unitTemplate);
    }

    @Override
    public Boolean containsUnitTemplateById(final String unitTemplateId) throws CouldNotPerformException {
        validateData();
        return unitTemplateRemoteRegistry.contains(unitTemplateId);
    }

    @Override
    public UnitTemplate getUnitTemplateById(final String unitTemplateId) throws CouldNotPerformException {
        validateData();
        return unitTemplateRemoteRegistry.getMessage(unitTemplateId);
    }

    @Override
    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException {
        validateData();
        return unitTemplateRemoteRegistry.getMessages();
    }

    @Override
    public UnitTemplate getUnitTemplateByType(final UnitTemplate.UnitType type) throws CouldNotPerformException {
        validateData();
        for (final UnitTemplate unitTemplate : unitTemplateRemoteRegistry.getMessages()) {
            if (unitTemplate.getType() == type) {
                return unitTemplate;
            }
        }
        throw new NotAvailableException("unit template", "No UnitTemplate with given Type[" + type + "] registered!");
    }

    @Override
    public Boolean isUnitTemplateRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getUnitTemplateRegistryReadOnly();
    }

    @Override
    public Boolean isUnitTemplateRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getUnitTemplateRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    @Override
    public Future<UnitConfig> registerUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(groupConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register group unit config!", ex);
        }
    }

    @Override
    public Future<UnitConfig> updateUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(groupConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update group unit config!", ex);
        }
    }

    @Override
    public Future<UnitConfig> removeUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(groupConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove group unit config!", ex);
        }
    }

    @Override
    public Boolean containsUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException {
        validateData();
        return unitGroupUnitConfigRemoteRegistry.contains(groupConfig);
    }

    @Override
    public Boolean containsUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException {
        validateData();
        return unitGroupUnitConfigRemoteRegistry.contains(groupConfigId);
    }

    @Override
    public List<UnitConfig> getUnitConfigsByLabel(final String unitConfigLabel) throws CouldNotPerformException {
        validateData();
        List<UnitConfig> unitConfigs = Collections.synchronizedList(new ArrayList<>());
        getUnitConfigs().parallelStream().filter((unitConfig) -> (unitConfig.getLabel().equalsIgnoreCase(unitConfigLabel))).forEach((unitConfig) -> {
            unitConfigs.add(unitConfig);
        });
        return unitConfigs;
    }

    @Override
    public List<UnitConfig> getUnitConfigs(final UnitTemplate.UnitType type) throws CouldNotPerformException {
        validateData();
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            if (type == UnitTemplate.UnitType.UNKNOWN || unitConfig.getType() == type || getSubUnitTypesOfUnitType(type).contains(unitConfig.getType())) {
                unitConfigs.add(unitConfig);
            }
        }
        return unitConfigs;
    }

    @Override
    public List<ServiceConfigType.ServiceConfig> getServiceConfigs() throws CouldNotPerformException {
        validateData();
        List<ServiceConfigType.ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            serviceConfigs.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigs;
    }

    @Override
    public List<ServiceConfigType.ServiceConfig> getServiceConfigs(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType) throws CouldNotPerformException {
        validateData();
        List<ServiceConfigType.ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            for (ServiceConfigType.ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                if (serviceConfig.getServiceTemplate().getType() == serviceType) {
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

    @Override
    public UnitConfig getUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException {
        validateData();
        return unitGroupUnitConfigRemoteRegistry.get(groupConfigId).getMessage();
    }

    @Override
    public List<UnitConfig> getUnitGroupConfigs() throws CouldNotPerformException {
        validateData();
        return new ArrayList<>(unitGroupUnitConfigRemoteRegistry.getMessages());
    }

    @Override
    public List<UnitConfig> getUnitGroupConfigsByUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        validateData();
        List<UnitConfig> unitConfigList = new ArrayList<>();
        for (UnitConfig unitGroupUnitConfig : unitGroupUnitConfigRemoteRegistry.getMessages()) {
            if (unitGroupUnitConfig.getUnitGroupConfig().getMemberIdList().contains(unitConfig.getId())) {
                unitConfigList.add(unitGroupUnitConfig);
            }
        }
        return unitConfigList;
    }

    @Override
    public List<UnitConfig> getUnitGroupConfigsByUnitType(final UnitTemplate.UnitType type) throws CouldNotPerformException {
        validateData();
        List<UnitConfig> unitConfigList = new ArrayList<>();
        for (UnitConfig unitGroupUnitConfig : unitGroupUnitConfigRemoteRegistry.getMessages()) {
            if (unitGroupUnitConfig.getType() == type || getSubUnitTypesOfUnitType(type).contains(unitGroupUnitConfig.getType())) {
                unitConfigList.add(unitGroupUnitConfig);
            }
        }
        return unitConfigList;
    }

    @Override
    public List<UnitConfig> getUnitGroupConfigsByServiceTypes(final List<ServiceTemplateType.ServiceTemplate.ServiceType> serviceTypes) throws CouldNotPerformException {
        validateData();
        List<UnitConfig> unitGroups = new ArrayList<>();
        for (UnitConfig unitGroupUnitConfig : unitGroupUnitConfigRemoteRegistry.getMessages()) {
            boolean skipGroup = false;
            for (ServiceTemplateType.ServiceTemplate serviceTemplate : unitGroupUnitConfig.getUnitGroupConfig().getServiceTemplateList()) {
                if (!serviceTypes.contains(serviceTemplate.getType())) {
                    skipGroup = true;
                }
            }
            if (skipGroup) {
                continue;
            }
            unitGroups.add(unitGroupUnitConfig);
        }
        return unitGroups;
    }

    @Override
    public List<UnitConfig> getUnitConfigsByUnitGroupConfig(final UnitConfig unitGroupUnitConfig) throws CouldNotPerformException {
        validateData();
        verifyUnitGroupUnitConfig(unitGroupUnitConfig);
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (String unitId : unitGroupUnitConfig.getUnitGroupConfig().getMemberIdList()) {
            unitConfigs.add(getUnitConfigById(unitId));
        }
        return unitConfigs;
    }

    @Override
    public List<UnitConfig> getUnitConfigsByUnitTypeAndServiceTypes(final UnitTemplate.UnitType type, List<ServiceTemplateType.ServiceTemplate.ServiceType> serviceTypes) throws CouldNotPerformException {
        validateData();
        List<UnitConfig> unitConfigs = getUnitConfigs(type);
        boolean foundServiceType;

        for (UnitConfig unitConfig : new ArrayList<>(unitConfigs)) {
            foundServiceType = false;
            for (ServiceTemplateType.ServiceTemplate.ServiceType serviceType : serviceTypes) {
                for (ServiceConfigType.ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getServiceTemplate().getType() == serviceType) {
                        foundServiceType = true;
                    }
                }
                if (!foundServiceType) {
                    unitConfigs.remove(unitConfig);
                }
            }
        }
        return unitConfigs;
    }

    @Override
    public UnitConfig getUnitConfigByScope(ScopeType.Scope scope) throws CouldNotPerformException {
        validateData();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            if (unitConfig.getScope().equals(scope)) {
                return unitConfig;
            }
        }
        throw new NotAvailableException("No unit config available for given scope!");
    }

    @Override
    public List<UnitTemplate.UnitType> getSubUnitTypesOfUnitType(final UnitTemplate.UnitType type) throws CouldNotPerformException {
        validateData();
        List<UnitTemplate.UnitType> unitTypes = new ArrayList<>();
        for (UnitTemplate template : unitTemplateRemoteRegistry.getMessages()) {
            if (template.getIncludedTypeList().contains(type)) {
                unitTypes.add(template.getType());
            }
        }
        return unitTypes;
    }

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
}
