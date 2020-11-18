package org.openbase.bco.dal.control.layer.unit.device;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.dal.lib.layer.unit.device.DeviceController;
import org.openbase.bco.dal.lib.layer.unit.device.DeviceControllerFactory;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractDeviceControllerFactory implements DeviceControllerFactory {

    public AbstractDeviceControllerFactory() throws InstantiationException {
        super();
    }

    @Override
    public DeviceController newInstance(final UnitConfig deviceUnitConfig) throws InstantiationException, InterruptedException {
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

            final GenericDeviceController genericDeviceController = new GenericDeviceController(getOperationServiceFactory(), getUnitDataSourceFactory());
            genericDeviceController.init(deviceUnitConfig);
            return genericDeviceController;
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(org.openbase.bco.dal.lib.layer.unit.device.Device.class, deviceUnitConfig.getId(), ex);
        }
    }
}
