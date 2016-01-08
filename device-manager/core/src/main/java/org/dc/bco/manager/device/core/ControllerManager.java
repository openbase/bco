/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.core;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.iface.Activatable;
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
