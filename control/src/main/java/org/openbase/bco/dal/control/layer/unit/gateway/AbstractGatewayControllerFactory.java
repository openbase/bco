package org.openbase.bco.dal.control.layer.unit.gateway;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.bco.dal.lib.layer.unit.device.DeviceControllerFactory;
import org.openbase.bco.dal.lib.layer.unit.device.DeviceManager;
import org.openbase.bco.dal.lib.layer.unit.gateway.Gateway;
import org.openbase.bco.dal.lib.layer.unit.gateway.GatewayController;
import org.openbase.bco.dal.lib.layer.unit.gateway.GatewayControllerFactory;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractGatewayControllerFactory implements GatewayControllerFactory {

    private final DeviceControllerFactory deviceControllerFactory;

    public AbstractGatewayControllerFactory(final DeviceControllerFactory deviceControllerFactory) throws InstantiationException {
        this.deviceControllerFactory = deviceControllerFactory;
    }

    @Override
    public GatewayController newInstance(final UnitConfig gatewayUnitConfig) throws InstantiationException, InterruptedException {
        try {
            if (gatewayUnitConfig == null) {
                throw new NotAvailableException("gatewayConfig");
            }

            if (!gatewayUnitConfig.hasId()) {
                throw new NotAvailableException("gatewayConfig.id");
            }

            if (!gatewayUnitConfig.hasLabel()) {
                throw new NotAvailableException("gatewayConfig.label");
            }

            if (!gatewayUnitConfig.hasPlacementConfig()) {
                throw new NotAvailableException("gatewayConfig.placement");
            }

            if (!gatewayUnitConfig.getPlacementConfig().hasLocationId()) {
                throw new NotAvailableException("gatewayConfig.placement.locationId");
            }

            final GenericGatewayController genericGatewayController = new GenericGatewayController(getDeviceControllerFactory());
            genericGatewayController.init(gatewayUnitConfig);

            return genericGatewayController;
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(Gateway.class, gatewayUnitConfig.getId(), ex);
        }
    }

    @Override
    public DeviceControllerFactory getDeviceControllerFactory() {
        return deviceControllerFactory;
    }
}
