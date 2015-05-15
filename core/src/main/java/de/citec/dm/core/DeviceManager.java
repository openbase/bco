/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core;

import de.citec.dm.core.registry.DeviceRegistryService;
import de.citec.jp.JPDeviceDatabaseDirectory;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jul.storage.jp.JPInitializeDB;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPDebugMode;
import de.citec.jps.preset.JPReadOnly;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class DeviceManager {

    private static final Logger logger = LoggerFactory.getLogger(DeviceManager.class);

    public static final String APP_NAME = DeviceManager.class.getSimpleName();

    private final DeviceRegistryService deviceRegistry;

    public DeviceManager() throws InitializationException {
        try {
            this.deviceRegistry = new DeviceRegistryService();
            this.deviceRegistry.init();
            this.deviceRegistry.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        if (deviceRegistry != null) {
            deviceRegistry.shutdown();
        }
    }

    public static void main(String args[]) throws Throwable {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPDeviceRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPInitializeDB.class);
        JPService.registerProperty(JPDeviceDatabaseDirectory.class);
        JPService.registerProperty(JPDeviceConfigDatabaseDirectory.class);
        JPService.registerProperty(JPDeviceClassDatabaseDirectory.class);

        JPService.parseAndExitOnError(args);

        try {
            new DeviceManager();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistory(logger, ex);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
