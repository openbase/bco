/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.core;

import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.bco.manager.device.lib.DeviceFactory;
import org.dc.bco.manager.device.lib.Device;
import org.dc.bco.manager.device.lib.DeviceManager;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public abstract class AbstractDeviceFactory implements DeviceFactory {

    public AbstractDeviceFactory() throws InstantiationException {
        super();
    }

    public Device newInstance(final DeviceConfig deviceConfig, final DeviceManager deviceManager) throws InstantiationException {
        try {
            return newInstance(deviceConfig, deviceManager.getServiceFactory());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(Device.class, deviceConfig.getId(), ex);
        }
    }

    @Override
    public Device newInstance(final DeviceConfig deviceConfig, final ServiceFactory serviceFactory) throws InstantiationException {
        try {
            if (deviceConfig == null) {
                throw new NotAvailableException("deviceConfig");
            }

            if (!deviceConfig.hasId()) {
                throw new NotAvailableException("deviceConfig.id");
            }

            if (!deviceConfig.hasLabel()) {
                throw new NotAvailableException("deviceConfig.label");
            }

            if (!deviceConfig.hasPlacementConfig()) {
                throw new NotAvailableException("deviceConfig.placement");
            }

            if (!deviceConfig.getPlacementConfig().hasLocationId()) {
                throw new NotAvailableException("deviceConfig.placement.locationId");
            }

            return new GenericDeviceController(deviceConfig, serviceFactory);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(Device.class, deviceConfig.getId(), ex);
        }
    }
}
