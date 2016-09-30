package org.openbase.bco.manager.app.binding.openhab;

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
import org.openbase.bco.registry.app.lib.jp.JPAppRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.openhab.binding.interfaces.OpenHABBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class AppBindingOpenHABLauncher {

    private static final Logger logger = LoggerFactory.getLogger(AppBindingOpenHABLauncher.class);

    private AppBindingOpenHABImpl openHABBinding;

    public void launch() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            openHABBinding = new AppBindingOpenHABImpl();
            openHABBinding.init();
        } catch (CouldNotPerformException | JPServiceException ex) {
            openHABBinding.shutdown();
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    public void shutdown() throws InterruptedException {
        openHABBinding.shutdown();
    }

    public AppBindingOpenHABImpl getOpenHABBinding() {
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
        JPService.registerProperty(JPAppRegistryScope.class);
        JPService.parseAndExitOnError(args);

        /* Start main app */
        logger.info("Start " + JPService.getApplicationName() + "...");
        try {
            new AppBindingOpenHABLauncher().launch();
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }
}
