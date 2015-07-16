/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.util;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.iface.Activatable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final ExecutorService executorService;
    private int connectionCounter;

    public ConnectionManager() {
        this.executorService = Executors.newCachedThreadPool(); 
        this.connectionCounter = 0;
    }

    public Future<Activatable> activate(final Activatable device) {
        return executorService.submit(new Callable() {

            @Override
            public Object call() throws Exception {
                try {
                    device.activate();
                    connectionCounter++;
                    return device;
                } catch (CouldNotPerformException | InterruptedException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(logger, new CouldNotPerformException("Could not activate: " + device, ex));
                }
            }
        });
    }
    
    public Future<Activatable> deactivate(final Activatable device) {
        return executorService.submit(new Callable<Activatable>() {

            @Override
            public Activatable call() throws Exception {
                try {
                    device.deactivate();
                    connectionCounter--;
                    return device;
                } catch (CouldNotPerformException | InterruptedException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(logger, new CouldNotPerformException("Could not deactivate: " + device, ex));
                }
            }
        });
    }

    public int getConnections() {
        return connectionCounter;
    }
}
