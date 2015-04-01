/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.remote;

import de.citec.csra.dm.registry.DeviceRegistryInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.IdentifiableMessage;
import de.citec.jul.rsb.RSBRemoteService;
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
    
    private final RemoteRegistry<DeviceClass> deviceClassRemoteRegistry;
    private final RemoteRegistry<DeviceConfig> deviceConfigRemoteRegistry;

    public DeviceRegistryRemote() {
        deviceClassRemoteRegistry = new RemoteRegistry<>();
        deviceConfigRemoteRegistry = new RemoteRegistry<>();
    }
    
    @Override
    public void notifyUpdated(final DeviceRegistry data) {
        deviceClassRemoteRegistry.notifyRegistryUpdated(data.getDeviceClassesList());
        deviceConfigRemoteRegistry.notifyRegistryUpdated(data.getDeviceConfigsList());
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
    public Boolean containsDeviceConfig(final DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return (Boolean) callMethodAsync("containsDeviceConfig", deviceConfig).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not check device config!", ex);
        }
    }

    @Override
    public Boolean containsDeviceConfigById(final String deviceConfigId) throws CouldNotPerformException {
        try {
            return (Boolean) callMethodAsync("containsDeviceConfigById", deviceConfigId).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not check device config!", ex);
        }
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
        try {
            return (Boolean) callMethodAsync("containsDeviceClass", deviceClass).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not check device class!", ex);
        }
    }

    @Override
    public Boolean containsDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        try {
            return (Boolean) callMethodAsync("containsDeviceClassById", deviceClassId).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not check device class!", ex);
        }
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
    public List<UnitConfigType.UnitConfig> getUnits() throws CouldNotPerformException {
        List<UnitConfigType.UnitConfig> unitConfigs = new ArrayList<>();
        for (IdentifiableMessage<DeviceConfig> deviceConfig : deviceConfigRemoteRegistry.getEntries()) {
            unitConfigs.addAll(deviceConfig.getMessageOrBuilder().getUnitConfigsList());
        }
        return unitConfigs;
    }

    @Override
    public List<ServiceConfigType.ServiceConfig> getServices() throws CouldNotPerformException {
        List<ServiceConfigType.ServiceConfig> serviceConfigs = new ArrayList<>();
        for (UnitConfigType.UnitConfig unitConfig : getUnits()) {
            serviceConfigs.addAll(unitConfig.getServiceConfigsList());
        }
        return serviceConfigs;
    }
}
