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
import de.citec.jul.storage.registry.jp.JPInitializeDB;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPReadOnly;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.MultiException;
import de.citec.jul.exception.VerificationFailedException;
import de.citec.jul.storage.registry.jp.JPGitRegistryPlugin;
import de.citec.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
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

    public LocationRegistryService getLocationRegistry() {
        return locationRegistry;
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
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);
        
        JPService.parseAndExitOnError(args);

        LocationManager locationManager;
        try {
            locationManager = new LocationManager();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        
        MultiException.ExceptionStack exceptionStack = null;
        
        if (!locationManager.getLocationRegistry().getLocationConfigRegistry().isConsistent()) {
            MultiException.push(locationManager, new VerificationFailedException("Started in read only mode!", new InvalidStateException("LocationConfigRegistry not consistent!")), exceptionStack);
        }
        
        try {
            MultiException.checkAndThrow(APP_NAME + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
