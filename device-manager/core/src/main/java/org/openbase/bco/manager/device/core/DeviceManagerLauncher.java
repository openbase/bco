package org.openbase.bco.manager.device.core;

/*
 * #%L
 * COMA DeviceManager Core
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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

import java.util.logging.Level;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.layer.service.mock.ServiceFactoryMock;
import org.openbase.bco.manager.device.lib.DeviceManager;
import org.openbase.bco.registry.device.lib.jp.JPDeviceRegistryScope;
import org.openbase.bco.registry.location.lib.jp.JPLocationRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class DeviceManagerLauncher {

    private static final Logger logger = LoggerFactory.getLogger(DeviceManagerLauncher.class);

    private DeviceManagerController deviceManagerController;
    
    public void launch() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            logger.info("Launching device manager...");
            deviceManagerController = new DeviceManagerController(new ServiceFactoryMock());
            logger.info("Init device manager controller...");
            deviceManagerController.init();
            logger.info("Device manager launched!");
        } catch (CouldNotPerformException ex) {
            deviceManagerController.shutdown();
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    public void shutdown() {
        deviceManagerController.shutdown();
    }

    public DeviceManager getDeviceManager() {
        return deviceManagerController;
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public static void main(String[] args) throws InterruptedException, CouldNotPerformException {

        /* Setup JPService */
        JPService.setApplicationName(DeviceManager.class);
        JPService.registerProperty(JPHardwareSimulationMode.class);
        JPService.registerProperty(JPLocationRegistryScope.class);
        JPService.registerProperty(JPDeviceRegistryScope.class);
        JPService.parseAndExitOnError(args);

        /* Start main app */
        logger.info("Start " + JPService.getApplicationName() + "...");
        try {
            new DeviceManagerLauncher().launch();
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }
}
