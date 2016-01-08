/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.app.core;

import org.dc.bco.registry.app.lib.jp.JPAppClassDatabaseDirectory;
import org.dc.bco.registry.app.lib.jp.JPAppConfigDatabaseDirectory;
import org.dc.bco.registry.app.lib.jp.JPAppRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.preset.JPDebugMode;
import org.dc.jps.preset.JPReadOnly;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.MultiException;
import org.dc.jul.exception.VerificationFailedException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.storage.registry.jp.JPGitRegistryPlugin;
import org.dc.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import org.dc.jul.storage.registry.jp.JPInitializeDB;
import org.dc.jps.preset.JPForce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class AppRegistryLauncher {

    private static final Logger logger = LoggerFactory.getLogger(AppRegistryLauncher.class);

    public static final String APP_NAME = AppRegistryLauncher.class.getSimpleName();

    private final AppRegistryController appRegistry;

    public AppRegistryLauncher() throws InitializationException, InterruptedException {
        try {
            this.appRegistry = new AppRegistryController();
            this.appRegistry.init();
            this.appRegistry.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        if (appRegistry != null) {
            appRegistry.shutdown();
        }
    }

    public AppRegistryController getAppRegistry() {
        return appRegistry;
    }

    public static void main(String args[]) throws Throwable {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPAppRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPInitializeDB.class);
        JPService.registerProperty(JPAppConfigDatabaseDirectory.class);
        JPService.registerProperty(JPAppClassDatabaseDirectory.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);

        JPService.parseAndExitOnError(args);

        AppRegistryLauncher appRegistry;
        try {
            appRegistry = new AppRegistryLauncher();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }

        MultiException.ExceptionStack exceptionStack = null;

        if (!appRegistry.getAppRegistry().getAppConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(appRegistry, new VerificationFailedException("AppConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        try {
            MultiException.checkAndThrow(APP_NAME + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
