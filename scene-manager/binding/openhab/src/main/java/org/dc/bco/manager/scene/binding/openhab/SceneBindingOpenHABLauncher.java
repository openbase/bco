package org.dc.bco.manager.scene.binding.openhab;

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
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.registry.scene.lib.jp.JPSceneRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.openhab.binding.interfaces.OpenHABBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public class SceneBindingOpenHABLauncher {

    private static final Logger logger = LoggerFactory.getLogger(SceneBindingOpenHABLauncher.class);

    private SceneBindingOpenHABImpl openHABBinding;

    public void launch() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        try {
            openHABBinding = new SceneBindingOpenHABImpl();
            openHABBinding.init();
        } catch (CouldNotPerformException | JPServiceException ex) {
            openHABBinding.shutdown();
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    public void shutdown() throws InterruptedException {
        openHABBinding.shutdown();
    }

    public SceneBindingOpenHABImpl getOpenHABBinding() {
        return openHABBinding;
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.dc.jul.exception.CouldNotPerformException
     */
    public static void main(String[] args) throws InterruptedException, CouldNotPerformException {

        /* Setup JPService */
        JPService.setApplicationName(OpenHABBinding.class);
        JPService.registerProperty(JPHardwareSimulationMode.class);
        JPService.registerProperty(JPSceneRegistryScope.class);
        JPService.parseAndExitOnError(args);

        /* Start main app */
        logger.info("Start " + JPService.getApplicationName() + "...");
        try {
            new SceneBindingOpenHABLauncher().launch();
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }
}
