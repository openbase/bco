/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.core;

/*
 * #%L
 * COMA DeviceManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.bco.manager.device.lib.Device;
import org.dc.bco.manager.device.lib.DeviceFactory;
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

    public Device newInstance(final DeviceConfig deviceConfig, final DeviceManager deviceManager) throws InstantiationException, InterruptedException {
        try {
            return newInstance(deviceConfig, deviceManager.getServiceFactory());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(Device.class, deviceConfig.getId(), ex);
        }
    }

    @Override
    public Device newInstance(final DeviceConfig deviceConfig, final ServiceFactory serviceFactory) throws InstantiationException, InterruptedException {
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

            final GenericDeviceController genericDeviceController = new GenericDeviceController(serviceFactory);
            genericDeviceController.init(deviceConfig);
            return genericDeviceController;

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(Device.class, deviceConfig.getId(), ex);
        }
    }
}
