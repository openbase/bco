/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.remote;

import de.citec.csra.dm.registry.DeviceRegistryInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.RSBRemoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.registry.DeviceRegistryType.DeviceRegistry;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryRemote extends RSBRemoteService<DeviceRegistry> implements DeviceRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceRegistry.getDefaultInstance()));
    }
    
    @Override
    public void notifyUpdated(DeviceRegistry data) {
        
    }

    @Override
    public void registerDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException {
        callMethodAsync("register", deviceConfig);
    }

    @Override
    public void updateDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException {
        callMethodAsync("update", deviceConfig);
    }

    @Override
    public void removeDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException {
        callMethodAsync("remove", deviceConfig);
    }

    @Override
    public void registerDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        callMethodAsync("register", deviceClass);
    }

    @Override
    public void updateDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        callMethodAsync("update", deviceClass);
    }

    @Override
    public void removeDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        callMethodAsync("remove", deviceClass);
    }
    
}
