/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.example;

import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class ObserveServiceStateChangesViaRemoteLib {

    public static final String APP_NAME = "CollectionServiceDataViaRemoteLib";

    private static final Logger logger = LoggerFactory.getLogger(ObserveServiceStateChangesViaRemoteLib.class);

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);
        JPService.parseAndExitOnError(args);

        try {
            DeviceRegistryRemote deviceRegistryRemote = new DeviceRegistryRemote();
            deviceRegistryRemote.init();
            deviceRegistryRemote.activate();
            
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }

    }
}
