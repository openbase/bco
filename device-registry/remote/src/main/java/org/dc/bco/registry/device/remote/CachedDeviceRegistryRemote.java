package org.dc.bco.registry.device.remote;

/*
 * #%L
 * REM DeviceRegistry Remote
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

import org.dc.bco.registry.device.lib.DeviceRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
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
                if (deviceRegistryRemote != null) {
                    deviceRegistryRemote.shutdown();
                    deviceRegistryRemote = null;
                }
            }
        });
    }

    /**
     *
     * @return
     * @throws InterruptedException
     * @throws NotAvailableException
     */
    public synchronized static DeviceRegistry getDeviceRegistry() throws InterruptedException, NotAvailableException {
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
}
