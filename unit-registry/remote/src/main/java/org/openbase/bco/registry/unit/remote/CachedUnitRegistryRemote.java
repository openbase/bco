package org.openbase.bco.registry.unit.remote;

/*
 * #%L
 * REM UnitRegistry Remote
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
public class CachedUnitRegistryRemote {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedUnitRegistryRemote.class);
    private static UnitRegistryRemote unitRegistryRemote;
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
            unitRegistryRemote.requestData().get(10, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not reinitialize " + CachedUnitRegistryRemote.class.getSimpleName() + "!", ex);
        }
    }

    /**
     *
     * @return @throws InterruptedException
     * @throws NotAvailableException
     */
    public synchronized static UnitRegistryRemote getRegistry() throws InterruptedException, NotAvailableException {
        try {
            if (shutdown) {
                throw new InvalidStateException("Remote service is shutting down!");
            }

            if (unitRegistryRemote == null) {
                try {
                    unitRegistryRemote = new UnitRegistryRemote();
                    unitRegistryRemote.init();
                    unitRegistryRemote.activate();
                } catch (CouldNotPerformException ex) {
                    if (unitRegistryRemote != null) {
                        unitRegistryRemote.shutdown();
                        unitRegistryRemote = null;
                    }
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not start cached unit registry remote!", ex), LOGGER);
                }
            }
            return unitRegistryRemote;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("cached unit registry", ex);
        }
    }

    public static void waitForData() throws InterruptedException, CouldNotPerformException {
        if (unitRegistryRemote == null) {
            getRegistry();
        }
        unitRegistryRemote.waitForData();
    }

    public static void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        if (unitRegistryRemote == null) {
            getRegistry();
        }
        unitRegistryRemote.waitForData(timeout, timeUnit);
    }

    public static void shutdown() {
        if (unitRegistryRemote != null) {
            unitRegistryRemote.shutdown();
            unitRegistryRemote = null;
        }
    }
}
