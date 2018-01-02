package org.openbase.bco.manager.device.core;

/*-
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

import org.openbase.bco.dal.lib.layer.service.mock.ServiceFactoryMock;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class FallbackDeviceManagerController implements Launchable<Void>, VoidInitializable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FallbackDeviceManagerController.class);
    
    private DeviceManagerController deviceManagerController;
    private DeviceRegistryRemote deviceRegistryRemote;
    
    public FallbackDeviceManagerController() throws InstantiationException, JPNotAvailableException {
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            deviceRegistryRemote = Registries.getDeviceRegistry();
            deviceRegistryRemote.waitForData();
            deviceManagerController = new DeviceManagerController(ServiceFactoryMock.getInstance()) {

                @Override
                public boolean isSupported(UnitConfig config) throws CouldNotPerformException {
                    try {
                        DeviceClass deviceClass = deviceRegistryRemote.getDeviceClassById(config.getDeviceConfig().getDeviceClassId());
                        return !deviceClass.getBindingConfig().hasBindingId() || deviceClass.getBindingConfig().getBindingId().isEmpty();
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not check device support!", ex), LOGGER);
                        return false;
                    }
                }
            };
            
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
