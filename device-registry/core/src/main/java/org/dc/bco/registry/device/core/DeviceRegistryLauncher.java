/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core;

import org.dc.bco.registry.device.lib.jp.JPDeviceClassDatabaseDirectory;
import org.dc.bco.registry.device.lib.jp.JPDeviceConfigDatabaseDirectory;
import org.dc.bco.registry.device.lib.jp.JPDeviceRegistryScope;
import org.dc.bco.registry.device.lib.jp.JPUnitGroupDatabaseDirectory;
import org.dc.bco.registry.device.lib.jp.JPUnitTemplateDatabaseDirectory;
import org.dc.jps.core.JPService;
import org.dc.jps.preset.JPDebugMode;
import org.dc.jps.preset.JPForce;
import org.dc.jps.preset.JPReadOnly;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.MultiException;
import org.dc.jul.exception.MultiException.ExceptionStack;
import org.dc.jul.exception.VerificationFailedException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.storage.registry.jp.JPGitRegistryPlugin;
import org.dc.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import org.dc.jul.storage.registry.jp.JPInitializeDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryLauncher {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistryLauncher.class);

    public static final String APP_NAME = DeviceRegistryLauncher.class.getSimpleName();

    private final DeviceRegistryController deviceRegistry;

    public DeviceRegistryLauncher() throws InitializationException, InterruptedException {
        try {
            this.deviceRegistry = new DeviceRegistryController();
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

    public DeviceRegistryController getDeviceRegistry() {
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
        JPService.registerProperty(JPUnitGroupDatabaseDirectory.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);

        JPService.parseAndExitOnError(args);

        DeviceRegistryLauncher deviceManager;
        try {
            deviceManager = new DeviceRegistryLauncher();
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
        if (!deviceManager.getDeviceRegistry().getUnitGroupRegistry().isConsistent()) {
            exceptionStack = MultiException.push(deviceManager, new VerificationFailedException("UnitGroupRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        try {
            MultiException.checkAndThrow(APP_NAME + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
