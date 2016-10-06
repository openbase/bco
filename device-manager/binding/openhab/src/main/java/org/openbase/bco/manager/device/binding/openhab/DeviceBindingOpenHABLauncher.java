package org.openbase.bco.manager.device.binding.openhab;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.layer.unit.AbstractUnitController;
import org.openbase.bco.registry.device.lib.jp.JPDeviceRegistryScope;
import org.openbase.bco.registry.location.lib.jp.JPLocationRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.openhab.binding.interfaces.OpenHABBinding;
import org.openbase.jul.schedule.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class DeviceBindingOpenHABLauncher {

    private static final Logger logger = LoggerFactory.getLogger(DeviceBindingOpenHABLauncher.class);

    private DeviceBindingOpenHABImpl openHABBinding;

    public void launch() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            openHABBinding = new DeviceBindingOpenHABImpl();
            openHABBinding.init();
        } catch (CouldNotPerformException | JPServiceException ex) {
            openHABBinding.shutdown();
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    public void shutdown() throws InterruptedException {
        openHABBinding.shutdown();
    }

    public DeviceBindingOpenHABImpl getOpenHABBinding() {
        return openHABBinding;
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public static void main(String[] args) throws InterruptedException, CouldNotPerformException {

        /* Setup JPService */
        JPService.setApplicationName(OpenHABBinding.class);
        JPService.registerProperty(JPHardwareSimulationMode.class);
        JPService.registerProperty(JPLocationRegistryScope.class);
        JPService.registerProperty(JPDeviceRegistryScope.class);
        JPService.parseAndExitOnError(args);

        /* Start main app */
        logger.info("Start " + JPService.getApplicationName() + "...");
        Stopwatch stopWatch = new Stopwatch();
        stopWatch.start();
        long time = System.currentTimeMillis();
        try {
            new DeviceBindingOpenHABLauncher().launch();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistoryAndExit(JPService.getApplicationName() + " crashed during startup phase!", ex, logger);
            return;
        }
        //logger.info("Activation took " + ControllerManager.activationTime / 1000.0f + "s.");
        logger.info("Method registration took " + AbstractUnitController.registerMethodTime / 1000.0f + "s.");
        logger.info("UpdateMethod verification took " + AbstractUnitController.updateMethodVerificationTime / 1000.0f + "s.");
        logger.info("Init took " + AbstractUnitController.initTime / 1000.0f + "s.");
        logger.info("Constructor took " + AbstractUnitController.constructorTime / 1000.0f + "s.");
        logger.info(JPService.getApplicationName() + " successfully started in " + stopWatch.stop() / 1000.0f + "s.");
    }
}
