/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device;

import de.citec.dal.hal.device.fibaro.F_FGS221Controller;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author mpohling
 */
public class DeviceFactory implements DeviceFactoryInterface {

    public DeviceFactory() {
    }
    
    @Override
    public Device newDevice(final DeviceConfig deviceConfig) {
//        deviceConfig.get

        F_FGS221Controller
        return deviceConfig.;
    }
}
