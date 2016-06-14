package org.openbase.bco.registry.device.core;

/*
 * #%L
 * REM DeviceRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.registry.device.lib.DeviceRegistry;
import org.openbase.bco.registry.device.lib.jp.JPDeviceClassDatabaseDirectory;
import org.openbase.bco.registry.device.lib.jp.JPDeviceConfigDatabaseDirectory;
import org.openbase.bco.registry.device.lib.jp.JPDeviceRegistryScope;
import org.openbase.bco.registry.device.lib.jp.JPUnitGroupDatabaseDirectory;
import org.openbase.bco.registry.device.lib.jp.JPUnitTemplateDatabaseDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPForce;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.MultiException.ExceptionStack;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.storage.registry.jp.JPGitRegistryPlugin;
import org.openbase.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import org.openbase.jul.storage.registry.jp.JPInitializeDB;
import org.openbase.jul.storage.registry.jp.JPRecoverDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryLauncher {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistryLauncher.class);

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

        /* Setup JPService */
        JPService.setApplicationName(DeviceRegistry.class);

        logger.info("Start " + JPService.getApplicationName() + "...");

        JPService.registerProperty(JPDeviceRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPRecoverDB.class);
        JPService.registerProperty(JPInitializeDB.class);
        JPService.registerProperty(JPDeviceConfigDatabaseDirectory.class);
        JPService.registerProperty(JPDeviceClassDatabaseDirectory.class);
        JPService.registerProperty(JPUnitTemplateDatabaseDirectory.class);
        JPService.registerProperty(JPUnitGroupDatabaseDirectory.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);

        JPService.parseAndExitOnError(args);

        DeviceRegistryLauncher deviceRegistry;
        try {
            deviceRegistry = new DeviceRegistryLauncher();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }

        ExceptionStack exceptionStack = null;

        if (!deviceRegistry.getDeviceRegistry().getUnitTemplateRegistry().isConsistent()) {
            exceptionStack = MultiException.push(deviceRegistry, new VerificationFailedException("UnitTemplateRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }
        if (!deviceRegistry.getDeviceRegistry().getDeviceClassRegistry().isConsistent()) {
            exceptionStack = MultiException.push(deviceRegistry, new VerificationFailedException("DeviceClassRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }
        if (!deviceRegistry.getDeviceRegistry().getDeviceConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(deviceRegistry, new VerificationFailedException("DeviceConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }
        if (!deviceRegistry.getDeviceRegistry().getUnitGroupRegistry().isConsistent()) {
            exceptionStack = MultiException.push(deviceRegistry, new VerificationFailedException("UnitGroupRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        try {
            MultiException.checkAndThrow(JPService.getApplicationName() + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }
}
