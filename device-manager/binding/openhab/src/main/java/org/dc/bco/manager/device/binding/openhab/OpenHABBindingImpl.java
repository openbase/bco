package org.dc.bco.manager.device.binding.openhab;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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

import org.dc.bco.manager.device.binding.openhab.comm.OpenHABCommunicator;
import org.dc.bco.manager.device.binding.openhab.comm.OpenHABCommunicatorImpl;
import org.dc.bco.manager.device.binding.openhab.service.OpenhabServiceFactory;
import org.dc.bco.manager.device.core.DeviceManagerController;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.binding.BindingTypeHolderType.BindingTypeHolder.BindingType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class OpenHABBindingImpl implements OpenHABBinding {

    private static final Logger logger = LoggerFactory.getLogger(OpenHABBindingImpl.class);

    private static OpenHABBinding instance;
    private DeviceManagerController deviceManagerController;
    private OpenHABCommunicatorImpl busCommunicator;
    private DeviceRegistryRemote deviceRegistryRemote;

    public OpenHABBindingImpl() throws InstantiationException {
        try {
            instance = this;
            this.busCommunicator = new OpenHABCommunicatorImpl();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public static OpenHABBinding getInstance() throws NotAvailableException {
        if (instance == null) {
            throw new NotAvailableException(OpenHABBinding.class);
        }
        return instance;
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            this.deviceRegistryRemote = new DeviceRegistryRemote();
            this.deviceRegistryRemote.init();
            this.deviceRegistryRemote.activate();

            this.deviceManagerController = new DeviceManagerController(new OpenhabServiceFactory()) {

                @Override
                public boolean isSupported(DeviceConfigType.DeviceConfig config) throws CouldNotPerformException {
                    try {
                        DeviceClass deviceClass = deviceRegistryRemote.getDeviceClassById(config.getDeviceClassId());
                        if (!deviceClass.getBindingConfig().getType().equals(BindingType.OPENHAB)) {
                            return false;
                        }
                        return super.isSupported(config);
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not check device support!", ex), logger);
                        return false;
                    }
                }
            };

            this.busCommunicator.init();
            this.deviceManagerController.init();
            this.busCommunicator.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() throws InterruptedException {
        if (deviceManagerController != null) {
            deviceManagerController.shutdown();
        }

        if (busCommunicator != null) {
            busCommunicator.shutdown();
        }
        instance = null;
    }

    @Override
    public OpenHABCommunicator getBusCommunicator() throws NotAvailableException {
        return busCommunicator;
    }
}
