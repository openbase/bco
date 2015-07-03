/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.scm.core;

import de.citec.scm.core.registry.SceneRegistryService;
import de.citec.jp.JPSceneDatabaseDirectory;
import de.citec.jp.JPSceneClassDatabaseDirectory;
import de.citec.jp.JPSceneConfigDatabaseDirectory;
import de.citec.jp.JPSceneRegistryScope;
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
public class SceneManager {

    private static final Logger logger = LoggerFactory.getLogger(SceneManager.class);

    public static final String APP_NAME = SceneManager.class.getSimpleName();

    private final SceneRegistryService sceneRegistry;

    public SceneManager() throws InitializationException, InterruptedException {
        try {
            this.sceneRegistry = new SceneRegistryService();
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

    public static void main(String args[]) throws Throwable {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPSceneRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPInitializeDB.class);
        JPService.registerProperty(JPSceneDatabaseDirectory.class);
        JPService.registerProperty(JPSceneConfigDatabaseDirectory.class);
        JPService.registerProperty(JPSceneClassDatabaseDirectory.class);

        JPService.parseAndExitOnError(args);

        try {
            new SceneManager();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
