package org.dc.bco.registry.location.remote;

/*
 * #%L
 * REM LocationRegistry Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import org.dc.bco.registry.location.lib.LocationRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine
 * Threepwood</a>
 */
public class CachedLocationRegistryRemote {

    private static final Logger logger = LoggerFactory.getLogger(CachedLocationRegistryRemote.class);
    private static LocationRegistryRemote locationRegistryRemote;
    private static boolean shutdown = false;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                shutdown();
            }
        });
    }

    /**
     *
     * @return @throws InterruptedException
     * @throws NotAvailableException
     */
    public synchronized static LocationRegistry getLocationRegistry() throws InterruptedException, NotAvailableException {
        try {
            if (shutdown) {
                throw new InvalidStateException("Remote service is shutting down!");
            }

            if (locationRegistryRemote == null) {
                try {
                    locationRegistryRemote = new LocationRegistryRemote();
                    locationRegistryRemote.init();
                    locationRegistryRemote.activate();
                } catch (CouldNotPerformException ex) {
                    if (locationRegistryRemote != null) {
                        locationRegistryRemote.shutdown();
                        locationRegistryRemote = null;
                    }
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not start cached location registry remote!", ex), logger);
                }
            }
            return locationRegistryRemote;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("cached location registry", ex);
        }
    }

    public static void shutdown() {
        shutdown = true;
        if (locationRegistryRemote != null) {
            locationRegistryRemote.shutdown();
            locationRegistryRemote = null;
        }
    }
}
