package org.openbase.bco.registry.location.remote;

/*
 * #%L
 * REM LocationRegistry Remote
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openbase.bco.registry.location.lib.LocationRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class CachedLocationRegistryRemote {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedLocationRegistryRemote.class);
    private static LocationRegistryRemote locationRegistryRemote;
    private static boolean shutdown = false;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                shutdown = true;
                shutdown();
            }
        });
    }

    public static void reinitialize() throws InterruptedException, CouldNotPerformException {
        try {
            getRegistry();
            locationRegistryRemote.requestData().get(10, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not reinitialize " + CachedLocationRegistryRemote.class.getSimpleName() + "!", ex);
        }
    }

    /**
     *
     * @return @throws InterruptedException
     * @throws NotAvailableException
     */
    public synchronized static LocationRegistry getRegistry() throws InterruptedException, NotAvailableException {
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
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not start cached location registry remote!", ex), LOGGER);
                }
            }
            return locationRegistryRemote;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("cached location registry", ex);
        }
    }

    public static void waitForData() throws InterruptedException, CouldNotPerformException {
        if (locationRegistryRemote == null) {
            getRegistry();
        }
        locationRegistryRemote.waitForData();
    }

    public static void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        if (locationRegistryRemote == null) {
            getRegistry();
        }
        locationRegistryRemote.waitForData(timeout, timeUnit);
    }

    public static void shutdown() {
        if (locationRegistryRemote != null) {
            locationRegistryRemote.shutdown();
            locationRegistryRemote = null;
        }
    }
}
