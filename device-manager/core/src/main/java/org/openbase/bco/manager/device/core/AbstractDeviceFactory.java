package org.openbase.bco.manager.device.core;

/*
 * #%L
 * BCO Manager Device Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import org.openbase.bco.dal.lib.layer.service.ServiceFactory;
import org.openbase.bco.manager.device.lib.Device;
import org.openbase.bco.manager.device.lib.DeviceController;
import org.openbase.bco.manager.device.lib.DeviceFactory;
import org.openbase.bco.manager.device.lib.DeviceManager;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractDeviceFactory implements DeviceFactory {

    public AbstractDeviceFactory() throws InstantiationException {
        super();
    }

    public DeviceController newInstance(final UnitConfig deviceUnitConfig, final DeviceManager deviceManager) throws InstantiationException, InterruptedException {
        try {
            return newInstance(deviceUnitConfig, deviceManager.getServiceFactory());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(Device.class, deviceUnitConfig.getId(), ex);
        }
    }

    @Override
    public DeviceController newInstance(final UnitConfig deviceUnitConfig, final ServiceFactory serviceFactory) throws InstantiationException, InterruptedException {
        try {
            if (deviceUnitConfig == null) {
                throw new NotAvailableException("deviceConfig");
            }

            if (!deviceUnitConfig.hasId()) {
                throw new NotAvailableException("deviceConfig.id");
            }

            if (!deviceUnitConfig.hasLabel()) {
                throw new NotAvailableException("deviceConfig.label");
            }

            if (!deviceUnitConfig.hasPlacementConfig()) {
                throw new NotAvailableException("deviceConfig.placement");
            }

            if (!deviceUnitConfig.getPlacementConfig().hasLocationId()) {
                throw new NotAvailableException("deviceConfig.placement.locationId");
            }

            final GenericDeviceController genericDeviceController = new GenericDeviceController(serviceFactory);
            genericDeviceController.init(deviceUnitConfig);
            return genericDeviceController;
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(org.openbase.bco.dal.lib.layer.unit.device.Device.class, deviceUnitConfig.getId(), ex);
        }
    }
}
