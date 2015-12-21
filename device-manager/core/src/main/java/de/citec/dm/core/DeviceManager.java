/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core;

import de.citec.dm.core.registry.DeviceRegistryService;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jp.JPUnitTemplateDatabaseDirectory;
import org.dc.jps.core.JPService;
import org.dc.jps.preset.JPDebugMode;
import org.dc.jps.preset.JPForce;
import org.dc.jps.preset.JPReadOnly;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.MultiException;
import de.citec.jul.exception.MultiException.ExceptionStack;
import de.citec.jul.exception.VerificationFailedException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.storage.registry.jp.JPGitRegistryPlugin;
import de.citec.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import de.citec.jul.storage.registry.jp.JPInitializeDB;
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

    public DeviceManager() throws InitializationException, InterruptedException {
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

    public DeviceRegistryService getDeviceRegistry() {
        return deviceRegistry;
    }

    public static void main(String args[]) throws Throwable {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPDeviceRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPInitializeDB.class);
        JPService.registerProperty(JPDeviceConfigDatabaseDirectory.class);
        JPService.registerProperty(JPDeviceClassDatabaseDirectory.class);
        JPService.registerProperty(JPUnitTemplateDatabaseDirectory.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);

        JPService.parseAndExitOnError(args);

        DeviceManager deviceManager;
        try {
            deviceManager = new DeviceManager();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }

        ExceptionStack exceptionStack = null;

        if (!deviceManager.getDeviceRegistry().getUnitTemplateRegistry().isConsistent()) {
            exceptionStack = MultiException.push(deviceManager, new VerificationFailedException("UnitTemplateRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }
        if (!deviceManager.getDeviceRegistry().getDeviceClassRegistry().isConsistent()) {
            exceptionStack = MultiException.push(deviceManager, new VerificationFailedException("DeviceClassRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }
        if (!deviceManager.getDeviceRegistry().getDeviceConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(deviceManager, new VerificationFailedException("DeviceConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        try {
            MultiException.checkAndThrow(APP_NAME + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
