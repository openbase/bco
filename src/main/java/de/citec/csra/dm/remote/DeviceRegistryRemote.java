/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.remote;

import de.citec.csra.dm.generator.DeviceClassIdGenerator;
import de.citec.csra.dm.generator.DeviceConfigIdGenerator;
import de.citec.csra.dm.registry.DeviceRegistryInterface;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.rsb.container.IdentifiableMessage;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.rsb.com.RSBRemoteService;
import de.citec.jul.storage.registry.RemoteRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryRemote extends RSBRemoteService<DeviceRegistry> implements DeviceRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceClassType.DeviceClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceConfigType.DeviceConfig.getDefaultInstance()));
    }

    private final RemoteRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistry.Builder> deviceClassRemoteRegistry;
    private final RemoteRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> deviceConfigRemoteRegistry;

    public DeviceRegistryRemote() throws InstantiationException {
        try {
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
    public void notifyUpdated(final DeviceRegistry data) throws CouldNotPerformException {
        deviceClassRemoteRegistry.notifyRegistryUpdated(data.getDeviceClassList());
        deviceConfigRemoteRegistry.notifyRegistryUpdated(data.getDeviceConfigList());
    }

    @Override
    public DeviceConfigType.DeviceConfig registerDeviceConfig(final DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return (DeviceConfigType.DeviceConfig) callMethodAsync("registerDeviceConfig", deviceConfig).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not register device config!", ex);
        }
    }

    @Override
    public DeviceClass getDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        return deviceClassRemoteRegistry.getMessage(deviceClassId);
    }

    @Override
    public DeviceConfig getDeviceConfigById(String deviceConfigId) throws CouldNotPerformException {
        return deviceConfigRemoteRegistry.getMessage(deviceConfigId);
    }

    @Override
    public UnitConfig getUnitConfigById(String unitConfigId) throws CouldNotPerformException {
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
    public Boolean containsDeviceConfig(final DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException {
        return deviceConfigRemoteRegistry.contains(deviceConfig);
    }

    @Override
    public Boolean containsDeviceConfigById(final String deviceConfigId) throws CouldNotPerformException {
        return deviceConfigRemoteRegistry.contains(deviceConfigId);
    }

    @Override
    public DeviceConfigType.DeviceConfig updateDeviceConfig(final DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return (DeviceConfigType.DeviceConfig) callMethodAsync("updateDeviceConfig", deviceConfig).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not update device config!", ex);
        }
    }

    @Override
    public DeviceConfigType.DeviceConfig removeDeviceConfig(final DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return (DeviceConfigType.DeviceConfig) callMethodAsync("removeDeviceConfig", deviceConfig).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not remove device config!", ex);
        }
    }

    @Override
    public DeviceClassType.DeviceClass registerDeviceClass(final DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return (DeviceClassType.DeviceClass) callMethodAsync("registerDeviceClass", deviceClass).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not register device class!", ex);
        }
    }

    @Override
    public Boolean containsDeviceClass(final DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        return deviceClassRemoteRegistry.contains(deviceClass);
    }

    @Override
    public Boolean containsDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        return deviceClassRemoteRegistry.contains(deviceClassId);
    }

    @Override
    public DeviceClassType.DeviceClass updateDeviceClass(final DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return (DeviceClassType.DeviceClass) callMethodAsync("updateDeviceClass", deviceClass).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not update device class!", ex);
        }
    }

    @Override
    public DeviceClassType.DeviceClass removeDeviceClass(final DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return (DeviceClassType.DeviceClass) callMethodAsync("removeDeviceClass", deviceClass).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not remove device class!", ex);
        }
    }

    @Override
    public List<UnitConfigType.UnitConfig> getUnitConfigs() throws CouldNotPerformException {
        List<UnitConfigType.UnitConfig> unitConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRemoteRegistry.getEntries()) {
            unitConfigs.addAll(deviceConfig.getMessage().getUnitConfigList());
        }
        return unitConfigs;
    }

    @Override
    public List<ServiceConfigType.ServiceConfig> getServiceConfigs() throws CouldNotPerformException {
        List<ServiceConfigType.ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfigType.UnitConfig unitConfig : getUnitConfigs()) {
            serviceConfigs.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigs;
    }

    @Override
    public List<DeviceClass> getDeviceClasses() throws CouldNotPerformException {
        return deviceClassRemoteRegistry.getMessages();
    }

    @Override
    public List<DeviceConfig> getDeviceConfigs() throws CouldNotPerformException {
        return deviceConfigRemoteRegistry.getMessages();
    }
}
