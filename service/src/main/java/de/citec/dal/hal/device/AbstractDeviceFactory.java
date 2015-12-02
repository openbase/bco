/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device;

import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jul.exception.CouldNotPerformException;
import rst.homeautomation.device.DeviceConfigType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public abstract class AbstractDeviceFactory implements DeviceFactory {

    private final DeviceRegistryRemote deviceRegistryRemote;
    
    public AbstractDeviceFactory(final DeviceRegistryRemote deviceRegistryRemote) {
        this.deviceRegistryRemote = deviceRegistryRemote;
    }

    public Device newInstance(final DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException {
        return newInstance(deviceConfig, deviceRegistryRemote);
    }
    
    @Override
    public Device newInstance(DeviceConfigType.DeviceConfig deviceConfig, DeviceRegistryRemote deviceRegistryRemote) throws CouldNotPerformException {
        return newInstance(deviceConfig, deviceRegistryRemote.getDeviceClassById(deviceConfig.getDeviceClassId()));
    }
}
