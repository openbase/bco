/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device;

import rst.homeautomation.device.DeviceConfigType;

/**
 *
 * @author mpohling
 */
public interface DeviceFactoryInterface {

    Device newDevice(final DeviceConfigType.DeviceConfig deviceConfig);
    
}
