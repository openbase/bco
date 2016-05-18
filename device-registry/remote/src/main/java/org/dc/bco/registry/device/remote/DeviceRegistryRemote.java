package org.dc.bco.registry.device.remote;

/*
 * #%L
 * REM DeviceRegistry Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import java.util.concurrent.TimeUnit;
import org.dc.bco.registry.device.lib.jp.JPDeviceRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.preset.JPReadOnly;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBRemoteService;
import org.dc.jul.extension.rsb.scope.ScopeTransformer;
import org.dc.jul.pattern.Remote;
import org.dc.jul.storage.registry.RemoteRegistry;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryRemote extends RSBRemoteService<DeviceRegistry> implements org.dc.bco.registry.device.lib.DeviceRegistry, Remote<DeviceRegistry> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitTemplate.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitGroupConfig.getDefaultInstance()));
    }

    private final RemoteRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistry.Builder> unitTemplateRemoteRegistry;
    private final RemoteRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistry.Builder> deviceClassRemoteRegistry;
    private final RemoteRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> deviceConfigRemoteRegistry;
    private final RemoteRegistry<String, UnitGroupConfig, UnitGroupConfig.Builder, DeviceRegistry.Builder> unitGroupRemoteRegistry;

    public DeviceRegistryRemote() throws InstantiationException {
        try {
            unitTemplateRemoteRegistry = new RemoteRegistry<>();
            deviceClassRemoteRegistry = new RemoteRegistry<>();
            deviceConfigRemoteRegistry = new RemoteRegistry<>();
            unitGroupRemoteRegistry = new RemoteRegistry<>();
            localId = id;
            id++;
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Method initializes the remote with the given scope for the server
     * registry connection.
     *
     * @param scope {@inheritDoc}
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
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
     * @param scope {@inheritDoc}
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
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
            this.init(JPService.getProperty(JPDeviceRegistryScope.class).getValue());
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        System.out.println("Activating DeviceRegistryRemote[" + localId + "]!");
        super.activate();
        System.out.println("Returned from activation super.activate in DeviceRegistryRemote[" + localId + "]");
    }

    @Override
    public void shutdown() {
        try {
            unitTemplateRemoteRegistry.shutdown();
            deviceClassRemoteRegistry.shutdown();
            deviceConfigRemoteRegistry.shutdown();
            unitGroupRemoteRegistry.shutdown();
        } finally {
            super.shutdown();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param data {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void notifyDataUpdate(final DeviceRegistry data) throws CouldNotPerformException {
//        System.out.println("DeviceRegistryRemote[" + localId + "] Notify data update ...");
        unitTemplateRemoteRegistry.notifyRegistryUpdate(data.getUnitTemplateList());
        deviceClassRemoteRegistry.notifyRegistryUpdate(data.getDeviceClassList());
        deviceConfigRemoteRegistry.notifyRegistryUpdate(data.getDeviceConfigList());
        unitGroupRemoteRegistry.notifyRegistryUpdate(data.getUnitGroupConfigList());
//        System.out.println("DeviceRegistryRemote[" + localId + "] Notify data update finished");
    }

    public RemoteRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistry.Builder> getUnitTemplateRemoteRegistry() {
        return unitTemplateRemoteRegistry;
    }

    public RemoteRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistry.Builder> getDeviceClassRemoteRegistry() {
        return deviceClassRemoteRegistry;
    }

    public RemoteRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> getDeviceConfigRemoteRegistry() {
        return deviceConfigRemoteRegistry;
    }

    public RemoteRegistry<String, UnitGroupConfig, UnitGroupConfig.Builder, DeviceRegistry.Builder> getUnitGroupConfigRemoteRegistry() {
        return unitGroupRemoteRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<DeviceConfig> registerDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(deviceConfig, this, DeviceConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register device config!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplateId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public UnitTemplate getUnitTemplateById(String unitTemplateId) throws CouldNotPerformException, NotAvailableException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return unitTemplateRemoteRegistry.getMessage(unitTemplateId);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClassId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public DeviceClass getDeviceClassById(String deviceClassId) throws CouldNotPerformException, NotAvailableException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return deviceClassRemoteRegistry.getMessage(deviceClassId);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfigId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public DeviceConfig getDeviceConfigById(String deviceConfigId) throws CouldNotPerformException, NotAvailableException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return deviceConfigRemoteRegistry.getMessage(deviceConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfigId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public UnitConfig getUnitConfigById(String unitConfigId) throws CouldNotPerformException, NotAvailableException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRemoteRegistry.getEntries()) {
            for (UnitConfig unitConfig : deviceConfig.getMessage().getUnitConfigList()) {
                if (unitConfig.getId().equals(unitConfigId)) {
                    return unitConfig;
                }
            }
        }
        throw new NotAvailableException(unitConfigId);
    }

    @Override
    public List<UnitConfig> getUnitConfigsByLabel(String unitConfigLabel) throws CouldNotPerformException, NotAvailableException {
        ArrayList<UnitConfig> unitConfigs = new ArrayList<>();
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRemoteRegistry.getEntries()) {
            for (UnitConfig unitConfig : deviceConfig.getMessage().getUnitConfigList()) {
                if (unitConfig.getLabel().equalsIgnoreCase(unitConfigLabel)) {
                    unitConfigs.add(unitConfig);
                }
            }
        }
        return unitConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplate {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return unitTemplateRemoteRegistry.contains(unitTemplate);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplateId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsUnitTemplateById(String unitTemplateId) throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return unitTemplateRemoteRegistry.contains(unitTemplateId);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return deviceConfigRemoteRegistry.contains(deviceConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfigId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsDeviceConfigById(final String deviceConfigId) throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return deviceConfigRemoteRegistry.contains(deviceConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<DeviceConfig> updateDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(deviceConfig, this, DeviceConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update device config!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplate {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitTemplate> updateUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(unitTemplate, this, UnitTemplate.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update unit template!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<DeviceConfig> removeDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(deviceConfig, this, DeviceConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove device config!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<DeviceClass> registerDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(deviceClass, this, DeviceClass.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register device class!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return deviceClassRemoteRegistry.contains(deviceClass);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClassId
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean containsDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return deviceClassRemoteRegistry.contains(deviceClassId);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<DeviceClass> updateDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(deviceClass, this, DeviceClass.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update device class!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<DeviceClass> removeDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(deviceClass, this, DeviceClass.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove device class!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException, NotAvailableException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRemoteRegistry.getEntries()) {
            unitConfigs.addAll(deviceConfig.getMessage().getUnitConfigList());
        }
        return unitConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @param type {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigs(final UnitType type) throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRemoteRegistry.getEntries()) {
            for (UnitConfig unitConfig : deviceConfig.getMessage().getUnitConfigList()) {
                if (type == UnitType.UNKNOWN || unitConfig.getType() == type || getSubUnitTypesOfUnitType(type).contains(unitConfig.getType())) {
                    unitConfigs.add(unitConfig);
                }
            }
        }
        return unitConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public List<ServiceConfig> getServiceConfigs() throws CouldNotPerformException, NotAvailableException {
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            serviceConfigs.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @param serviceType {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public List<ServiceConfig> getServiceConfigs(final ServiceType serviceType) throws CouldNotPerformException, NotAvailableException {
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                if (serviceConfig.getType() == serviceType) {
                    serviceConfigs.add(serviceConfig);
                }
            }
        }
        return serviceConfigs;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException, NotAvailableException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return unitTemplateRemoteRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public List<DeviceClass> getDeviceClasses() throws CouldNotPerformException, NotAvailableException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return deviceClassRemoteRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public List<DeviceConfig> getDeviceConfigs() throws CouldNotPerformException, NotAvailableException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return deviceConfigRemoteRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @param type {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public UnitTemplate getUnitTemplateByType(final UnitType type) throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        for (UnitTemplate unitTemplate : unitTemplateRemoteRegistry.getMessages()) {
            if (unitTemplate.getType() == type) {
                return unitTemplate;
            }
        }
        throw new NotAvailableException("unit template", "No UnitTemplate with given Type[" + type + "] registered!");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isUnitTemplateRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return getData().getUnitTemplateRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isDeviceClassRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return getData().getDeviceClassRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isDeviceConfigRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return getData().getDeviceConfigRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitGroupConfig> registerUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(groupConfig, this, UnitGroupConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register unit group config!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return unitGroupRemoteRegistry.contains(groupConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfigId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsUnitGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return unitGroupRemoteRegistry.contains(groupConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitGroupConfig> updateUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(groupConfig, this, UnitGroupConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update unit group config!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitGroupConfig> removeUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(groupConfig, this, UnitGroupConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove unit group config!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfigId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public UnitGroupConfig getUnitGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return unitGroupRemoteRegistry.getMessage(groupConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitGroupConfig> getUnitGroupConfigs() throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        List<UnitGroupConfig> unitGroups = new ArrayList<>();
        for (IdentifiableMessage<String, UnitGroupConfig, UnitGroupConfig.Builder> unitGroup : unitGroupRemoteRegistry.getEntries()) {
            unitGroups.add(unitGroup.getMessage());
        }
        return unitGroups;
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitGroupConfig> getUnitGroupConfigsbyUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        List<UnitGroupConfig> unitGroups = new ArrayList<>();
        for (UnitGroupConfig unitGroup : getUnitGroupConfigs()) {
            if (unitGroup.getMemberIdList().contains(unitConfig.getId())) {
                unitGroups.add(unitGroup);
            }
        }
        return unitGroups;
    }

    /**
     * {@inheritDoc}
     *
     * @param serviceTypes {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitGroupConfig> getUnitGroupConfigsByServiceTypes(List<ServiceType> serviceTypes) throws CouldNotPerformException {
        List<UnitGroupConfig> unitGroups = new ArrayList<>();
        for (UnitGroupConfig unitGroup : getUnitGroupConfigs()) {
            boolean skipGroup = false;
            for (ServiceType serviceType : unitGroup.getServiceTypeList()) {
                if (!serviceTypes.contains(serviceType)) {
                    skipGroup = true;
                }
            }
            if (skipGroup) {
                continue;
            }
            unitGroups.add(unitGroup);
        }
        return unitGroups;
    }

    /**
     * {@inheritDoc}
     *
     * @param type {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitGroupConfig> getUnitGroupConfigsByUnitType(UnitType type) throws CouldNotPerformException {
        List<UnitGroupConfig> unitGroups = new ArrayList<>();
        for (UnitGroupConfig unitGroup : getUnitGroupConfigs()) {
            if (unitGroup.getUnitType() == type || getSubUnitTypesOfUnitType(type).contains(unitGroup.getUnitType())) {
                unitGroups.add(unitGroup);
            }
        }
        return unitGroups;
    }

    /**
     * {@inheritDoc}
     *
     * @param groupConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (String unitId : groupConfig.getMemberIdList()) {
            unitConfigs.add(getUnitConfigById(unitId));
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
    public Boolean isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return getData().getUnitGroupRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @param type {@inheritDoc}
     * @param serviceTypes {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByUnitTypeAndServiceTypes(final UnitType type, final List<ServiceType> serviceTypes) throws CouldNotPerformException {

        List<UnitConfig> unitConfigs = getUnitConfigs(type);

        boolean foundServiceType;

        for (UnitConfig unitConfig : new ArrayList<>(unitConfigs)) {
            foundServiceType = false;
            for (ServiceType serviceType : serviceTypes) {
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getType() == serviceType) {
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

    /**
     * {@inheritDoc}
     *
     * @param scope {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public UnitConfig getUnitConfigByScope(final ScopeType.Scope scope) throws CouldNotPerformException {
        for (UnitConfig unitConfig : getUnitConfigs()) {
            if (unitConfig.getScope().equals(scope)) {
                return unitConfig;
            }
        }
        throw new NotAvailableException("No unit config available for given Scope[" + scope + "]!");
    }

    /**
     * {@inheritDoc}
     *
     * @param type {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitType> getSubUnitTypesOfUnitType(UnitType type) throws CouldNotPerformException {
        List<UnitType> unitTypes = new ArrayList<>();
        for (UnitTemplate template : getUnitTemplates()) {
            if (template.getIncludedTypeList().contains(type)) {
                unitTypes.add(template.getType());
            }
        }
        return unitTypes;
    }
}
