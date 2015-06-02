/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.core;

import de.citec.lm.core.registry.LocationRegistryService;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jp.JPLocationDatabaseDirectory;
import de.citec.jp.JPLocationConfigDatabaseDirectory;
import de.citec.jp.JPLocationRegistryScope;
import de.citec.jul.storage.jp.JPInitializeDB;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPReadOnly;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class LocationManager {

    private static final Logger logger = LoggerFactory.getLogger(LocationManager.class);

    public static final String APP_NAME = "LocationManager";

    private final LocationRegistryService locationRegistry;

    public LocationManager() throws InitializationException, InterruptedException {
        try {
            this.locationRegistry = new LocationRegistryService();
            this.locationRegistry.init();
            this.locationRegistry.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        if (locationRegistry != null) {
            locationRegistry.shutdown();
        }
    }

    public static void main(String args[]) throws Throwable {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPLocationRegistryScope.class);
        JPService.registerProperty(JPDeviceRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPInitializeDB.class);
        JPService.registerProperty(JPLocationDatabaseDirectory.class);
        JPService.registerProperty(JPLocationConfigDatabaseDirectory.class);

        JPService.parseAndExitOnError(args);

        try {
            new LocationManager();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistory(logger, ex);
        }
        logger.info("=== " + APP_NAME + " successfully started. ===");
    }
}
