/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.core;

import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.bco.manager.device.lib.Device;
import org.dc.jul.exception.InstantiationException;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author mpohling
 */
public class DeviceFactoryImpl extends AbstractDeviceFactory {

    private final ServiceFactory serviceFactory;

    public DeviceFactoryImpl(final ServiceFactory serviceFactory) throws InstantiationException {
        this.serviceFactory = serviceFactory;
    }

    @Override
    public Device newInstance(final DeviceConfig config) throws InstantiationException {
        return newInstance(config, serviceFactory);
    }
}
