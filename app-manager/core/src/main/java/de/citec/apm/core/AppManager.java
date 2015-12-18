/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.apm.core;

import de.citec.apm.core.registry.AppRegistryService;
import de.citec.jp.JPAppClassDatabaseDirectory;
import de.citec.jp.JPAppConfigDatabaseDirectory;
import de.citec.jp.JPAppRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.preset.JPDebugMode;
import org.dc.jps.preset.JPReadOnly;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.MultiException;
import de.citec.jul.exception.VerificationFailedException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.storage.registry.jp.JPGitRegistryPlugin;
import de.citec.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import de.citec.jul.storage.registry.jp.JPInitializeDB;
import org.dc.jps.preset.JPForce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class AppManager {

    private static final Logger logger = LoggerFactory.getLogger(AppManager.class);

    public static final String APP_NAME = AppManager.class.getSimpleName();

    private final AppRegistryService appRegistry;

    public AppManager() throws InitializationException, InterruptedException {
        try {
            this.appRegistry = new AppRegistryService();
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

    public AppRegistryService getAppRegistry() {
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

        AppManager appManager;
        try {
            appManager = new AppManager();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }

        MultiException.ExceptionStack exceptionStack = null;

        if (!appManager.getAppRegistry().getAppConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(appManager, new VerificationFailedException("AppConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        try {
            MultiException.checkAndThrow(APP_NAME + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
