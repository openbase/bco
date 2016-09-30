package org.openbase.bco.registry.unit.remote;

/*
 * #%L
 * REM UnitRegistry Remote
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
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.service.ServiceTemplateType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitRegistryDataType.UnitRegistryData;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitRegistryRemote extends AbstractRegistryRemote<UnitRegistryData> implements UnitRegistry, RegistryRemote<UnitRegistryData> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(final UnitRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(final UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(final UnitTemplate.getDefaultInstance()));
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
    private final RemoteRegistry<String, UnitConfig, UnitConfig.Builder> unitConfigRemoteRegistry;

    public UnitRegistryRemote() throws InstantiationException, InterruptedException {
        super(JPUnitRegistryScope.class, UnitRegistryData.class);
        try {
            this.unitTemplateRemoteRegistry = new SynchronizedRemoteRegistry<>(final UnitRegistryData.UNIT_TEMPLATE_FIELD_NUMBER, this);
            this.dalUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry(final UnitRegistryData.DAL_UNIT_CONFIG_FIELD_NUMBER, this);
            this.userUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(final UnitRegistryData.USER_UNIT_CONFIG_FIELD_NUMBER, this);
            this.authorizationGroupUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(final UnitRegistryData.AUTHORIZATION_GROUP_UNIT_CONFIG_FIELD_NUMBER, this);
            this.deviceUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(final UnitRegistryData.DEVICE_UNIT_CONFIG_FIELD_NUMBER, this);
            this.unitGroupUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(final UnitRegistryData.UNIT_GROUP_UNIT_CONFIG_FIELD_NUMBER, this);
            this.locationUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(final UnitRegistryData.LOCATION_UNIT_CONFIG_FIELD_NUMBER, this);
            this.connectionUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(final UnitRegistryData.CONNECTION_UNIT_CONFIG_FIELD_NUMBER, this);
            this.agentUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(final UnitRegistryData.AGENT_UNIT_CONFIG_FIELD_NUMBER, this);
            this.sceneUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(final UnitRegistryData.SCENE_UNIT_CONFIG_FIELD_NUMBER, this);
            this.appUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(final UnitRegistryData.APP_UNIT_CONFIG_FIELD_NUMBER, this);
            this.unitConfigRemoteRegistry = new RemoteRegistry<>();
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
            throw new CouldNotPerformException("Could not update unit config!", ex);
        }
    }

    @Override
    public Future<UnitConfig> removeUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException {
    }

    @Override
    public Boolean containsUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException {
    }

    @Override
    public Boolean containsUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException {
    }

    @Override
    public List<UnitConfig> getUnitConfigsByLabel(final String unitConfigLabel) throws CouldNotPerformException {
    }

    @Override
    public List<UnitConfig> getUnitConfigs(final UnitTemplate.UnitType type) throws CouldNotPerformException {
    }

    @Override
    public List<ServiceConfigType.ServiceConfig> getServiceConfigs() throws CouldNotPerformException {
    }

    @Override
    public List<ServiceConfigType.ServiceConfig> getServiceConfigs(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType) throws CouldNotPerformException {
    }

    @Override
    public Boolean isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException {
    }

    @Override
    public Boolean isUnitGroupConfigRegistryConsistent() throws CouldNotPerformException {
    }

    @Override
    public UnitConfig getUnitGroupConfigById(final String groupConfigId) throws CouldNotPerformException {
    }

    @Override
    public List<UnitConfig> getUnitGroupConfigs() throws CouldNotPerformException {
    }

    @Override
    public List<UnitConfig> getUnitGroupConfigsByUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
    }

    @Override
    public List<UnitConfig> getUnitGroupConfigsByUnitType(final UnitTemplate.UnitType type) throws CouldNotPerformException {
    }

    @Override
    public List<UnitConfig> getUnitGroupConfigsByServiceTypes(final List<ServiceTemplateType.ServiceTemplate.ServiceType> serviceTypes) throws CouldNotPerformException {
    }

    @Override
    public List<UnitConfig> getUnitConfigsByUnitGroupConfig(final UnitConfig groupConfig) throws CouldNotPerformException {
    }

    @Override
    public List<UnitConfig> getUnitConfigsByUnitTypeAndServiceTypes(final UnitTemplate.UnitType type, List<ServiceTemplateType.ServiceTemplate.ServiceType> serviceTypes) throws CouldNotPerformException {
    }

    @Override
    public UnitConfig getUnitConfigByScope(ScopeType.Scope scope) throws CouldNotPerformException {
    }

    @Override
    public List<UnitTemplate.UnitType> getSubUnitTypesOfUnitType(final UnitTemplate.UnitType type) throws CouldNotPerformException {
    }
}
