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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.dc.bco.registry.device.lib.generator.DeviceClassIdGenerator;
import org.dc.bco.registry.device.lib.generator.DeviceConfigIdGenerator;
import org.dc.bco.registry.device.lib.generator.UnitGroupIdGenerator;
import org.dc.bco.registry.device.lib.generator.UnitTemplateIdGenerator;
import org.dc.bco.registry.device.lib.jp.JPDeviceRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.preset.JPReadOnly;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBRemoteService;
import org.dc.jul.storage.registry.RemoteRegistry;
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

/**
 *
 * @author mpohling
 */
public class DeviceRegistryRemote extends RSBRemoteService<DeviceRegistry> implements org.dc.bco.registry.device.lib.DeviceRegistry {

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
            unitTemplateRemoteRegistry = new RemoteRegistry<>(new UnitTemplateIdGenerator());
            deviceClassRemoteRegistry = new RemoteRegistry<>(new DeviceClassIdGenerator());
            deviceConfigRemoteRegistry = new RemoteRegistry<>(new DeviceConfigIdGenerator());
            unitGroupRemoteRegistry = new RemoteRegistry<>(new UnitGroupIdGenerator());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        try {
            super.init(JPService.getProperty(JPDeviceRegistryScope.class).getValue());
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        try {
            notifyUpdated(requestStatus());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial registry sync failed!", ex), logger);
        }
    }

    @Override
    public void notifyUpdated(final DeviceRegistry data) throws CouldNotPerformException {
        deviceClassRemoteRegistry.notifyRegistryUpdated(data.getDeviceClassList());
        deviceConfigRemoteRegistry.notifyRegistryUpdated(data.getDeviceConfigList());
        unitTemplateRemoteRegistry.notifyRegistryUpdated(data.getUnitTemplateList());
        unitGroupRemoteRegistry.notifyRegistryUpdated(data.getUnitGroupConfigList());
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

    @Override
    public DeviceConfig registerDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return (DeviceConfig) callMethod("registerDeviceConfig", deviceConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register device config!", ex);
        }
    }

    @Override
    public UnitTemplate getUnitTemplateById(String unitTemplateId) throws CouldNotPerformException, NotAvailableException {
        getData();
        return unitTemplateRemoteRegistry.getMessage(unitTemplateId);
    }

    @Override
    public DeviceClass getDeviceClassById(String deviceClassId) throws CouldNotPerformException, NotAvailableException {
        getData();
        return deviceClassRemoteRegistry.getMessage(deviceClassId);
    }

    @Override
    public DeviceConfig getDeviceConfigById(String deviceConfigId) throws CouldNotPerformException, NotAvailableException {
        getData();
        return deviceConfigRemoteRegistry.getMessage(deviceConfigId);
    }

    @Override
    public UnitConfig getUnitConfigById(String unitConfigId) throws CouldNotPerformException, NotAvailableException {
        getData();
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
        getData();

        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRemoteRegistry.getEntries()) {
            for (UnitConfig unitConfig : deviceConfig.getMessage().getUnitConfigList()) {
                if (unitConfig.getLabel().equalsIgnoreCase(unitConfigLabel)) {
                    unitConfigs.add(unitConfig);
                }
            }
        }
        return unitConfigs;
    }

    @Override
    public Boolean containsUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException {
        getData();
        return unitTemplateRemoteRegistry.contains(unitTemplate);
    }

    @Override
    public Boolean containsUnitTemplateById(String unitTemplateId) throws CouldNotPerformException {
        getData();
        return unitTemplateRemoteRegistry.contains(unitTemplateId);
    }

    @Override
    public Boolean containsDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException {
        getData();
        return deviceConfigRemoteRegistry.contains(deviceConfig);
    }

    @Override
    public Boolean containsDeviceConfigById(final String deviceConfigId) throws CouldNotPerformException {
        getData();
        return deviceConfigRemoteRegistry.contains(deviceConfigId);
    }

    @Override
    public DeviceConfig updateDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return (DeviceConfig) callMethod("updateDeviceConfig", deviceConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update device config!", ex);
        }
    }

    @Override
    public UnitTemplate updateUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException {
        try {
            return callMethod("updateUnitTemplate", unitTemplate);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update unit template!", ex);
        }
    }

    @Override
    public DeviceConfig removeDeviceConfig(final DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return (DeviceConfig) callMethod("removeDeviceConfig", deviceConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove device config!", ex);
        }
    }

    @Override
    public DeviceClass registerDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return (DeviceClass) callMethod("registerDeviceClass", deviceClass);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register device class!", ex);
        }
    }

    @Override
    public Boolean containsDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        getData();
        return deviceClassRemoteRegistry.contains(deviceClass);
    }

    @Override
    public Boolean containsDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        getData();
        return deviceClassRemoteRegistry.contains(deviceClassId);
    }

    @Override
    public DeviceClass updateDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return (DeviceClass) callMethod("updateDeviceClass", deviceClass);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update device class!", ex);
        }
    }

    @Override
    public DeviceClass removeDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return (DeviceClass) callMethod("removeDeviceClass", deviceClass);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove device class!", ex);
        }
    }

    @Override
    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException, NotAvailableException {
        getData();
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRemoteRegistry.getEntries()) {
            unitConfigs.addAll(deviceConfig.getMessage().getUnitConfigList());
        }
        return unitConfigs;
    }

    @Override
    public List<UnitConfig> getUnitConfigs(final UnitType type) throws CouldNotPerformException {
        getData();
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

    @Override
    public List<ServiceConfig> getServiceConfigs() throws CouldNotPerformException, NotAvailableException {
        List<ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs()) {
            serviceConfigs.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigs;
    }

    @Override
    public List<ServiceConfig> getServiceConfigs(final ServiceType serviceType) throws CouldNotPerformException {
        getData();
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

    @Override
    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException, NotAvailableException {
        getData();
        return unitTemplateRemoteRegistry.getMessages();
    }

    @Override
    public List<DeviceClass> getDeviceClasses() throws CouldNotPerformException, NotAvailableException {
        getData();
        return deviceClassRemoteRegistry.getMessages();
    }

    @Override
    public List<DeviceConfig> getDeviceConfigs() throws CouldNotPerformException, NotAvailableException {
        getData();
        return deviceConfigRemoteRegistry.getMessages();
    }

    @Override
    public UnitTemplate getUnitTemplateByType(final UnitType type) throws CouldNotPerformException {
        for (UnitTemplate unitTemplate : unitTemplateRemoteRegistry.getMessages()) {
            if (unitTemplate.getType() == type) {
                return unitTemplate;
            }
        }
        throw new NotAvailableException("unit template", "No UnitTemplate with given type registered!");
    }

    @Override
    public Future<Boolean> isUnitTemplateRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return CompletableFuture.completedFuture(true);
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        try {
            return RPCHelper.callRemoteMethod(this, Boolean.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the unit template registry!!", ex);
        }
    }

    @Override
    public Future<Boolean> isDeviceClassRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return CompletableFuture.completedFuture(true);
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        try {
            return RPCHelper.callRemoteMethod(this, Boolean.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the device class registry!!", ex);
        }
    }

    @Override
    public Future<Boolean> isDeviceConfigRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return CompletableFuture.completedFuture(true);
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        try {
            return RPCHelper.callRemoteMethod(this, Boolean.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the device config registry!", ex);
        }
    }

    @Override
    public UnitGroupConfig registerUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        try {
            return (UnitGroupConfig) callMethod("registerUnitGroupConfig", groupConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register unit group config!", ex);
        }
    }

    @Override
    public Boolean containsUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        getData();
        return unitGroupRemoteRegistry.contains(groupConfig);
    }

    @Override
    public Boolean containsUnitGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        getData();
        return unitGroupRemoteRegistry.contains(groupConfigId);
    }

    @Override
    public UnitGroupConfig updateUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        try {
            return (UnitGroupConfig) callMethod("updateUnitGroupConfig", groupConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update unit group config!", ex);
        }
    }

    @Override
    public UnitGroupConfig removeUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        try {
            return (UnitGroupConfig) callMethod("removeUnitGroupConfig", groupConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove unit group config!", ex);
        }
    }

    @Override
    public UnitGroupConfig getUnitGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        getData();
        return unitGroupRemoteRegistry.getMessage(groupConfigId);
    }

    @Override
    public List<UnitGroupConfig> getUnitGroupConfigs() throws CouldNotPerformException {
        getData();
        List<UnitGroupConfig> unitGroups = new ArrayList<>();
        for (IdentifiableMessage<String, UnitGroupConfig, UnitGroupConfig.Builder> unitGroup : unitGroupRemoteRegistry.getEntries()) {
            unitGroups.add(unitGroup.getMessage());
        }
        return unitGroups;
    }

    @Override
    public List<UnitGroupConfig> getUnitGroupConfigsbyUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException {
        getData();
        List<UnitGroupConfig> unitGroups = new ArrayList<>();
        for (UnitGroupConfig unitGroup : getUnitGroupConfigs()) {
            if (unitGroup.getMemberIdList().contains(unitConfig.getId())) {
                unitGroups.add(unitGroup);
            }
        }
        return unitGroups;
    }

    @Override
    public List<UnitGroupConfig> getUnitGroupConfigsByServiceTypes(List<ServiceType> serviceTypes) throws CouldNotPerformException {
        getData();
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

    @Override
    public List<UnitGroupConfig> getUnitGroupConfigsByUnitType(UnitType type) throws CouldNotPerformException {
        getData();
        List<UnitGroupConfig> unitGroups = new ArrayList<>();
        for (UnitGroupConfig unitGroup : getUnitGroupConfigs()) {
            if (unitGroup.getUnitType() == type || getSubUnitTypesOfUnitType(type).contains(unitGroup.getUnitType())) {
                unitGroups.add(unitGroup);
            }
        }
        return unitGroups;
    }

    @Override
    public List<UnitConfig> getUnitConfigsByUnitGroupConfig(UnitGroupConfig groupConfig) throws CouldNotPerformException {
        getData();
        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (String unitId : groupConfig.getMemberIdList()) {
            unitConfigs.add(getUnitConfigById(unitId));
        }
        return unitConfigs;
    }

    @Override
    public Future<Boolean> isUnitGroupConfigRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return CompletableFuture.completedFuture(true);
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        try {
            return RPCHelper.callRemoteMethod(this, Boolean.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the unit group config registry!", ex);
        }
    }

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
