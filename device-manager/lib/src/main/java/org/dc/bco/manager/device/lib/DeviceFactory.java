/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.lib;

import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.pattern.Factory;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author mpohling
 */
public interface DeviceFactory extends Factory<Device, DeviceConfig> {

    public Device newInstance(final DeviceConfig deviceConfig, final ServiceFactory serviceFactory) throws InstantiationException, InterruptedException;

}
