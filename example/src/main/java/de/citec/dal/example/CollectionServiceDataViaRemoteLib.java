/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.example;

import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.lm.remote.LocationRegistryRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class CollectionServiceDataViaRemoteLib {

    public static final String APP_NAME = "CollectionServiceDataViaRemoteLib";

    private static final Logger logger = LoggerFactory.getLogger(CollectionServiceDataViaRemoteLib.class);

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
//            
//            deviceRegistryRemote.gets
//                    
//                    LocationRegistryRemote l;
//                    l.get
            
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }

    }
}
