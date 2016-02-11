package org.dc.bco.manager.location.core;

/*
 * #%L
 * COMA LocationManager Core
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

import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.storage.registry.RegistryImpl;
import org.dc.jul.storage.registry.RegistrySynchronizer;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;
import org.dc.bco.manager.location.lib.Location;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class LocationManagerLauncher {

    protected static final Logger logger = LoggerFactory.getLogger(LocationManagerLauncher.class);

    private final LocationFactory factory;
    private final RegistryImpl<String, Location> locationRegistry;
    private final LocationRegistryRemote locationRegistryRemote;
    private final RegistrySynchronizer<String, Location, LocationConfig, LocationConfigType.LocationConfig.Builder> registrySynchronizer;

    public LocationManagerLauncher() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        logger.info("Starting " + JPService.getApplicationName());
        try {
            this.factory = new LocationFactoryImpl();
            this.locationRegistry = new RegistryImpl<>();

            locationRegistryRemote = new LocationRegistryRemote();

            this.registrySynchronizer = new RegistrySynchronizer<>(locationRegistry, locationRegistryRemote.getLocationConfigRemoteRegistry(), factory);

            locationRegistryRemote.init();
            locationRegistryRemote.activate();
            registrySynchronizer.init();
            logger.info("waiting for locations...");

        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        try {
            JPService.setApplicationName(LocationManagerLauncher.class.getSimpleName());

            JPService.parseAndExitOnError(args);
            new LocationManagerLauncher();
        } catch (CouldNotPerformException | NullPointerException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
    }
}
