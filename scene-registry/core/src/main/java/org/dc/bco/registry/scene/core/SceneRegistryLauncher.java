/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.scene.core;

import org.dc.bco.registry.scene.lib.jp.JPSceneClassDatabaseDirectory;
import org.dc.bco.registry.scene.lib.jp.JPSceneConfigDatabaseDirectory;
import org.dc.bco.registry.scene.lib.jp.JPSceneRegistryScope;
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
public class SceneRegistryLauncher {

    private static final Logger logger = LoggerFactory.getLogger(SceneRegistryLauncher.class);

    public static final String APP_NAME = SceneRegistryLauncher.class.getSimpleName();

    private final SceneRegistryController sceneRegistry;

    public SceneRegistryLauncher() throws InitializationException, InterruptedException {
        try {
            this.sceneRegistry = new SceneRegistryController();
            this.sceneRegistry.init();
            this.sceneRegistry.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        if (sceneRegistry != null) {
            sceneRegistry.shutdown();
        }
    }

    public SceneRegistryController getSceneRegistry() {
        return sceneRegistry;
    }


    public static void main(String args[]) throws Throwable {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPSceneRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPInitializeDB.class);
        JPService.registerProperty(JPSceneConfigDatabaseDirectory.class);
        JPService.registerProperty(JPSceneClassDatabaseDirectory.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);

        JPService.parseAndExitOnError(args);

        SceneRegistryLauncher sceneManager;
        try {
            sceneManager = new SceneRegistryLauncher();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }

        MultiException.ExceptionStack exceptionStack = null;

        if (!sceneManager.getSceneRegistry().getSceneConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(sceneManager, new VerificationFailedException("SceneConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        try {
            MultiException.checkAndThrow(APP_NAME + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
