package org.openbase.bco.manager.device.core;

/*
 * #%L
 * COMA DeviceManager Core
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Activatable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 * TODO mpohling: use ActivatableEntryRegistySynchronizer and mark as deprecated.
 */
public class ControllerManager {

    private static final Logger logger = LoggerFactory.getLogger(ControllerManager.class);

    private final ExecutorService executorService;
    private int connectionCounter;

    public ControllerManager() {
        this.executorService = Executors.newSingleThreadExecutor(); 
//        this.executorService = Executors.newCachedThreadPool(); 
        this.connectionCounter = 0;
    }

    public Future<Activatable> activate(final Activatable device) {
        Future submit = executorService.submit(new Callable() {
            
            @Override
            public Object call() throws Exception {
                try {
                    device.activate();
                    connectionCounter++;
                    return device;
                } catch (CouldNotPerformException | InterruptedException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not activate: " + device, ex), logger);
                }
            }
        });
        
        try {
            //TODO mpohling: Check way unit tests failed if activation is not synchronized!
            submit.get();
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
        return submit;
    }
    
    public Future<Activatable> deactivate(final Activatable device) {
        Future<Activatable> submit = executorService.submit(new Callable<Activatable>() {
            
            @Override
            public Activatable call() throws Exception {
                try {
                    device.deactivate();
                    connectionCounter--;
                    return device;
                } catch (CouldNotPerformException | InterruptedException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not deactivate: " + device, ex), logger);
                }
            }
        });
        try {
            //TODO mpohling: Check way unit tests failed if activation is not synchronized!
            submit.get();
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
        return submit;
    }

    public int getConnections() {
        return connectionCounter;
    }
}
