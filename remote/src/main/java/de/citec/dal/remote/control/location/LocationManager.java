package de.citec.dal.remote.control.location;

import org.dc.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.storage.registry.Registry;
import de.citec.jul.storage.registry.RegistrySynchronizer;
import de.citec.lm.remote.LocationRegistryRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class LocationManager {

    protected static final Logger logger = LoggerFactory.getLogger(LocationManager.class);

    private final LocationFactory factory;
    private final Registry<String, Location> locationRegistry;
    private final LocationRegistryRemote locationRegistryRemote;
    private final RegistrySynchronizer<String, Location, LocationConfig, LocationConfigType.LocationConfig.Builder> registrySynchronizer;

    public LocationManager() throws de.citec.jul.exception.InstantiationException, InterruptedException {
        logger.info("Starting " + JPService.getApplicationName());
        try {
            this.factory = new LocationFactoryImpl();
            this.locationRegistry = new Registry<>();

            locationRegistryRemote = new LocationRegistryRemote();

            this.registrySynchronizer = new RegistrySynchronizer<>(locationRegistry, locationRegistryRemote.getLocationConfigRemoteRegistry(), factory);

            locationRegistryRemote.init();
            locationRegistryRemote.activate();
            registrySynchronizer.init();
            logger.info("waiting for locations...");

        } catch (CouldNotPerformException ex) {
            throw new de.citec.jul.exception.InstantiationException(this, ex);
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
