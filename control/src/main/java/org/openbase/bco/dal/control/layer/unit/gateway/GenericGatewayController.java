package org.openbase.bco.dal.control.layer.unit.gateway;

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

import org.openbase.bco.dal.lib.layer.service.OperationServiceFactory;
import org.openbase.bco.dal.lib.layer.service.UnitDataSourceFactory;
import org.openbase.bco.dal.lib.layer.unit.device.DeviceController;
import org.openbase.bco.dal.lib.layer.unit.device.DeviceControllerFactory;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * * @author <a href="mailto:mpohling@techfak.uni-bielefeld.com">Marian Pohling</a>
 */
public class GenericGatewayController extends AbstractGatewayController {

    private final DeviceControllerFactory deviceControllerFactory;

    public GenericGatewayController(final DeviceControllerFactory deviceControllerFactory) throws CouldNotPerformException {
        this.deviceControllerFactory = deviceControllerFactory;
    }

    @Override
    protected DeviceController buildUnitController(UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        return deviceControllerFactory.newInstance(unitConfig);
    }

    public DeviceControllerFactory getDeviceControllerFactory() {
        return deviceControllerFactory;
    }

    // todo cleanup interfaces since instance is provided via the device manager interface.
    @Override
    public OperationServiceFactory getOperationServiceFactory() throws NotAvailableException {
        return deviceControllerFactory.getOperationServiceFactory();
    }

    // todo cleanup interfaces since instance is provided via the device manager interface.
    @Override
    public UnitDataSourceFactory getUnitDataSourceFactory() throws NotAvailableException {
        return deviceControllerFactory.getUnitDataSourceFactory();
    }
}
