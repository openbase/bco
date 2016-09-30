package org.openbase.bco.registry.unit.core;

/*
 * #%L
 * REM SceneRegistry Core
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
import org.openbase.bco.registry.unit.lib.jp.JPAgentConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPAppConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPAuthorizationGroupConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPConnectionConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPDalUnitConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPDeviceConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPLocationConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPSceneConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPUnitGroupDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPUnitRegistryScope;
import org.openbase.bco.registry.unit.lib.jp.JPUnitTemplateDatabaseDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPForce;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.storage.registry.jp.JPGitRegistryPlugin;
import org.openbase.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import org.openbase.jul.storage.registry.jp.JPInitializeDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitRegistryLauncher {

    private static final Logger logger = LoggerFactory.getLogger(UnitRegistryLauncher.class);

    public static final String APP_NAME = UnitRegistryLauncher.class.getSimpleName();

    private final UnitRegistryController unitRegistry;

    public UnitRegistryLauncher() throws InitializationException, InterruptedException {
        try {
            this.unitRegistry = new UnitRegistryController();
            this.unitRegistry.init();
            this.unitRegistry.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        if (unitRegistry != null) {
            unitRegistry.shutdown();
        }
    }

    public UnitRegistryController getUnitRegistry() {
        return unitRegistry;
    }

    public static void main(String args[]) throws Throwable {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPUnitRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPInitializeDB.class);
        JPService.registerProperty(JPUnitTemplateDatabaseDirectory.class);
        JPService.registerProperty(JPAgentConfigDatabaseDirectory.class);
        JPService.registerProperty(JPAppConfigDatabaseDirectory.class);
        JPService.registerProperty(JPAuthorizationGroupConfigDatabaseDirectory.class);
        JPService.registerProperty(JPConnectionConfigDatabaseDirectory.class);
        JPService.registerProperty(JPDalUnitConfigDatabaseDirectory.class);
        JPService.registerProperty(JPDeviceConfigDatabaseDirectory.class);
        JPService.registerProperty(JPLocationConfigDatabaseDirectory.class);
        JPService.registerProperty(JPSceneConfigDatabaseDirectory.class);
        JPService.registerProperty(JPUnitGroupDatabaseDirectory.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);

        JPService.parseAndExitOnError(args);

        UnitRegistryLauncher unitRegistry;
        try {
            unitRegistry = new UnitRegistryLauncher();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }

        MultiException.ExceptionStack exceptionStack = null;

        if (!unitRegistry.getUnitRegistry().getUnitTemplateRegistry().isConsistent()) {
            exceptionStack = MultiException.push(unitRegistry, new VerificationFailedException("UnitTemplateRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        if (!unitRegistry.getUnitRegistry().getAgentUnitConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(unitRegistry, new VerificationFailedException("AgentUnitConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        if (!unitRegistry.getUnitRegistry().getAppUnitConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(unitRegistry, new VerificationFailedException("AppUnitConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        if (!unitRegistry.getUnitRegistry().getAuthorizationGroupUnitConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(unitRegistry, new VerificationFailedException("AuthorizationGroupUnitConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        if (!unitRegistry.getUnitRegistry().getConnectionUnitConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(unitRegistry, new VerificationFailedException("ConnectionUnitConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        if (!unitRegistry.getUnitRegistry().getDalUnitConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(unitRegistry, new VerificationFailedException("DalUnitConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        if (!unitRegistry.getUnitRegistry().getDeviceUnitConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(unitRegistry, new VerificationFailedException("DeviceUnitConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        if (!unitRegistry.getUnitRegistry().getLocationUnitConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(unitRegistry, new VerificationFailedException("LocationUnitConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        if (!unitRegistry.getUnitRegistry().getSceneUnitConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(unitRegistry, new VerificationFailedException("SceneUnitConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        if (!unitRegistry.getUnitRegistry().getUnitGroupUnitConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(unitRegistry, new VerificationFailedException("UnitGroupUnitConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        try {
            MultiException.checkAndThrow(APP_NAME + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
