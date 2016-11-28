package org.openbase.bco.registry.device.remote;

/*
 * #%L
 * REM DeviceRegistry Remote
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
import org.openbase.bco.registry.device.lib.DeviceRegistry;
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
public class CachedDeviceRegistryRemote {

    private static final Logger logger = LoggerFactory.getLogger(CachedDeviceRegistryRemote.class);
    private static DeviceRegistryRemote deviceRegistryRemote;
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
            deviceRegistryRemote.requestData().get(10, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not reinitialize " + CachedDeviceRegistryRemote.class.getSimpleName() + "!", ex);
        }
    }

    /**
     *
     * @return @throws InterruptedException
     * @throws NotAvailableException
     */
    public synchronized static DeviceRegistryRemote getRegistry() throws InterruptedException, NotAvailableException {
        try {
            if (shutdown) {
                throw new InvalidStateException("Remote service is shutting down!");
            }

            if (deviceRegistryRemote == null) {
                try {
                    deviceRegistryRemote = new DeviceRegistryRemote();
                    deviceRegistryRemote.init();
                    deviceRegistryRemote.activate();    
                } catch (CouldNotPerformException ex) {
                    if (deviceRegistryRemote != null) {
                        deviceRegistryRemote.shutdown();
                        deviceRegistryRemote = null;
                    }
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not start cached device registry remote!", ex), logger);
                }
            }
            return deviceRegistryRemote;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("cached device registry", ex);
        }
    }
    
    public static void waitForData() throws InterruptedException, CouldNotPerformException {
        if (deviceRegistryRemote == null) {
            getRegistry();
        }
        deviceRegistryRemote.waitForData();
    }
    
    public static void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        if (deviceRegistryRemote == null) {
            getRegistry();
        }
        deviceRegistryRemote.waitForData(timeout, timeUnit);
    }

    public static void shutdown() {
        if (deviceRegistryRemote != null) {
            deviceRegistryRemote.shutdown();
            deviceRegistryRemote = null;
        }
    }
}
