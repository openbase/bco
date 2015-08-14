/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.remote;

import de.citec.dm.lib.generator.DeviceClassIdGenerator;
import de.citec.dm.lib.generator.DeviceConfigIdGenerator;
import de.citec.dm.lib.generator.UnitTemplateIdGenerator;
import de.citec.dm.lib.registry.DeviceRegistryInterface;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.rsb.com.RSBRemoteService;
import de.citec.jul.storage.registry.RemoteRegistry;
import java.util.ArrayList;
import java.util.List;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryRemote extends RSBRemoteService<DeviceRegistry> implements DeviceRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitTemplate.getDefaultInstance()));
    }

    private final RemoteRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistry.Builder> unitTemplateRemoteRegistry;
    private final RemoteRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistry.Builder> deviceClassRemoteRegistry;
    private final RemoteRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> deviceConfigRemoteRegistry;

    public DeviceRegistryRemote() throws InstantiationException {
        try {
            unitTemplateRemoteRegistry = new RemoteRegistry<>(new UnitTemplateIdGenerator());
            deviceClassRemoteRegistry = new RemoteRegistry<>(new DeviceClassIdGenerator());
            deviceConfigRemoteRegistry = new RemoteRegistry<>(new DeviceConfigIdGenerator());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        super.init(JPService.getProperty(JPDeviceRegistryScope.class).getValue());
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        try {
            notifyUpdated(requestStatus());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(logger, new CouldNotPerformException("Initial registry sync failed!", ex));
        }
    }

    @Override
    public void notifyUpdated(final DeviceRegistry data) throws CouldNotPerformException {
        deviceClassRemoteRegistry.notifyRegistryUpdated(data.getDeviceClassList());
        deviceConfigRemoteRegistry.notifyRegistryUpdated(data.getDeviceConfigList());
    }

    @Override
    public DeviceConfigType.DeviceConfig registerDeviceConfig(final DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return (DeviceConfigType.DeviceConfig) callMethod("registerDeviceConfig", deviceConfig);
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
    public DeviceClassType.DeviceClass registerDeviceClass(final DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return (DeviceClassType.DeviceClass) callMethod("registerDeviceClass", deviceClass);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register device class!", ex);
        }
    }

    @Override
    public Boolean containsDeviceClass(final DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        getData();
        return deviceClassRemoteRegistry.contains(deviceClass);
    }

    @Override
    public Boolean containsDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        getData();
        return deviceClassRemoteRegistry.contains(deviceClassId);
    }

    @Override
    public DeviceClassType.DeviceClass updateDeviceClass(final DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return (DeviceClassType.DeviceClass) callMethod("updateDeviceClass", deviceClass);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update device class!", ex);
        }
    }

    @Override
    public DeviceClassType.DeviceClass removeDeviceClass(final DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return (DeviceClassType.DeviceClass) callMethod("removeDeviceClass", deviceClass);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove device class!", ex);
        }
    }

    @Override
    public List<UnitConfigType.UnitConfig> getUnitConfigs() throws CouldNotPerformException, NotAvailableException {
        getData();
        List<UnitConfigType.UnitConfig> unitConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRemoteRegistry.getEntries()) {
            unitConfigs.addAll(deviceConfig.getMessage().getUnitConfigList());
        }
        return unitConfigs;
    }

    public List<UnitConfigType.UnitConfig> getUnitConfigs(final UnitType type) throws CouldNotPerformException, NotAvailableException {
        getData();
        List<UnitConfigType.UnitConfig> unitConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRemoteRegistry.getEntries()) {
            for (UnitConfig unitConfig : deviceConfig.getMessage().getUnitConfigList()) {
                if (unitConfig.getType() == type) {
                    unitConfigs.add(unitConfig);
                }
            }
        }
        return unitConfigs;
    }

    @Override
    public List<ServiceConfigType.ServiceConfig> getServiceConfigs() throws CouldNotPerformException, NotAvailableException {
        List<ServiceConfigType.ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfigType.UnitConfig unitConfig : getUnitConfigs()) {
            serviceConfigs.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigs;
    }

    public List<ServiceConfigType.ServiceConfig> getServiceConfigs(final ServiceType serviceType) throws CouldNotPerformException, NotAvailableException {
        getData();
        List<ServiceConfigType.ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfigType.UnitConfig unitConfig : getUnitConfigs()) {
            for (ServiceConfigType.ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
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
}
