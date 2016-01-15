package org.dc.bco.manager.location.core;

import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.storage.registry.RegistryImpl;
import org.dc.jul.storage.registry.RegistrySynchronizer;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;
import org.dc.bco.manager.location.lib.Location;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class LocationManager {

    protected static final Logger logger = LoggerFactory.getLogger(LocationManager.class);

    private final LocationFactory factory;
    private final RegistryImpl<String, Location> locationRegistry;
    private final LocationRegistryRemote locationRegistryRemote;
    private final RegistrySynchronizer<String, Location, LocationConfig, LocationConfigType.LocationConfig.Builder> registrySynchronizer;

    public LocationManager() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        logger.info("Starting " + JPService.getApplicationName());
        try {
            this.factory = new LocationFactoryImpl();
            this.locationRegistry = new RegistryImpl<>();

            locationRegistryRemote = new LocationRegistryRemote();

            this.registrySynchronizer = new RegistrySynchronizer<>(locationRegistry, locationRegistryRemote.getLocationConfigRemoteRegistry(), factory);

            locationRegistryRemote.init();
            locationRegistryRemote.activate();
            registrySynchronizer.init();
            logger.info("waiting for locations...");

        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        try {
            JPService.setApplicationName(LocationManager.class.getSimpleName());

            JPService.parseAndExitOnError(args);
            new LocationManager();
        } catch (CouldNotPerformException | NullPointerException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
    }
}
