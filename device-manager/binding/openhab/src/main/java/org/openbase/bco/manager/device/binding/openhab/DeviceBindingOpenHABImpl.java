package org.openbase.bco.manager.device.binding.openhab;

/*
 * #%L
 * BCO Manager Device Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.openbase.bco.manager.device.binding.openhab.comm.DeviceOpenHABRemote;
import org.openbase.bco.manager.device.binding.openhab.service.OpenhabServiceFactory;
import org.openbase.bco.manager.device.core.DeviceManagerController;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.openhab.binding.AbstractOpenHABBinding;
import org.openbase.jul.extension.openhab.binding.interfaces.OpenHABRemote;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class DeviceBindingOpenHABImpl extends AbstractOpenHABBinding {

    //TODO: Should be declared in the openhab config generator and used from there
    public static final String DEVICE_MANAGER_ITEM_FILTER = "";

    private DeviceManagerController deviceManagerController;
    private DeviceRegistryRemote deviceRegistryRemote;
    
    public DeviceBindingOpenHABImpl() throws InstantiationException, JPNotAvailableException {
        super();
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            init(DEVICE_MANAGER_ITEM_FILTER, new DeviceOpenHABRemote());
        } catch (InstantiationException | JPNotAvailableException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void init(String itemFilter, OpenHABRemote openHABRemote) throws InitializationException, InterruptedException {
        try {
            deviceRegistryRemote = Registries.getDeviceRegistry();
            deviceRegistryRemote.waitForData();
            deviceManagerController = new DeviceManagerController(new OpenhabServiceFactory()) {

                @Override
                public boolean isSupported(UnitConfig config) throws CouldNotPerformException {
                    try {
                        DeviceClass deviceClass = deviceRegistryRemote.getDeviceClassById(config.getDeviceConfig().getDeviceClassId());
                        if (!deviceClass.getBindingConfig().getBindingId().equals("OPENHAB")) {
                            return false;
                        }
                        return super.isSupported(config);
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not check device support!", ex), LOGGER);
                        return false;
                    }
                }
            };

            super.init(itemFilter, openHABRemote);
            deviceManagerController.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void shutdown() {
        if (deviceManagerController != null) {
            deviceManagerController.shutdown();
        }
        super.shutdown();
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        deviceManagerController.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        deviceManagerController.deactivate();
    }

    @Override
    public boolean isActive() {
        return deviceManagerController.isActive();
    }
}
