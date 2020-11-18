package org.openbase.bco.device.openhab.manager.unit;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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

import org.openbase.bco.dal.control.layer.unit.gateway.GenericGatewayController;
import org.openbase.bco.dal.lib.layer.unit.device.DeviceControllerFactory;
import org.openbase.bco.device.openhab.communication.OpenHABRestCommunicator;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;

public class OpenHABGatewayController extends GenericGatewayController {

    public static final String OPENHAB_EXTENSION_ID_META_CONFIG_KEY = "OPENHAB_EXTENSION_ID";

    public OpenHABGatewayController(final DeviceControllerFactory deviceControllerFactory) throws CouldNotPerformException {
        super(deviceControllerFactory);
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        final String value = generateVariablePool().getValue(OPENHAB_EXTENSION_ID_META_CONFIG_KEY);

        try {
            // check if installed
            if (OpenHABRestCommunicator.getInstance().isBindingInstalled(value)) {
                OpenHABRestCommunicator.getInstance().installBinding(value);
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not install openhab binding: " + value, ex, logger);
        }
    }
}
