package org.openbase.bco.registry.location.core;

/*
 * #%L
 * REM LocationRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.bco.registry.location.lib.jp.JPLocationRegistryScope;
import org.openbase.bco.registry.unit.lib.jp.JPConnectionConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPLocationConfigDatabaseDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPForce;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.storage.registry.jp.JPGitRegistryPlugin;
import org.openbase.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import org.openbase.jul.storage.registry.jp.JPInitializeDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
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

        logger.info(APP_NAME + " successfully started.");
    }
}
