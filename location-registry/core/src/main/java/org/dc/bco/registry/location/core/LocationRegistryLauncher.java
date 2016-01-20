/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core;

import org.dc.bco.registry.device.lib.jp.JPDeviceRegistryScope;
import org.dc.bco.registry.location.lib.jp.JPConnectionConfigDatabaseDirectory;
import org.dc.bco.registry.location.lib.jp.JPLocationConfigDatabaseDirectory;
import org.dc.bco.registry.location.lib.jp.JPLocationRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.preset.JPForce;
import org.dc.jps.preset.JPReadOnly;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.MultiException;
import org.dc.jul.exception.VerificationFailedException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.storage.registry.jp.JPGitRegistryPlugin;
import org.dc.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import org.dc.jul.storage.registry.jp.JPInitializeDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class LocationRegistryLauncher {

    private static final Logger logger = LoggerFactory.getLogger(LocationRegistryLauncher.class);

    public static final String APP_NAME = "LocationRegistry";

    private final LocationRegistryController locationRegistry;

    public LocationRegistryLauncher() throws InitializationException, InterruptedException {
        try {
            this.locationRegistry = new LocationRegistryController();
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

    public LocationRegistryController getLocationRegistry() {
        return locationRegistry;
    }

    public static void main(String args[]) throws Throwable {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPLocationRegistryScope.class);
        JPService.registerProperty(JPDeviceRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPInitializeDB.class);
        JPService.registerProperty(JPLocationConfigDatabaseDirectory.class);
        JPService.registerProperty(JPConnectionConfigDatabaseDirectory.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);

        JPService.parseAndExitOnError(args);

        LocationRegistryLauncher locationRegistry;
        try {
            locationRegistry = new LocationRegistryLauncher();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }

        MultiException.ExceptionStack exceptionStack = null;

        if (!locationRegistry.getLocationRegistry().getLocationConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(locationRegistry, new VerificationFailedException("LocationConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        if (!locationRegistry.getLocationRegistry().getConnectionConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(locationRegistry, new VerificationFailedException("ConnectionConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        try {
            MultiException.checkAndThrow(APP_NAME + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
