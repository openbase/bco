/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.scm.manager;

import de.citec.jp.JPSceneRegistryScope;
import de.citec.jul.storage.registry.jp.JPInitializeDB;
import org.dc.jps.core.JPService;
import org.dc.jps.preset.JPDebugMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.scm.remote.SceneRegistryRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class SceneCoreManager {

    private static final Logger logger = LoggerFactory.getLogger(SceneCoreManager.class);

    public static final String APP_NAME = SceneCoreManager.class.getSimpleName();

    private final SceneRegistryRemote sceneRegistryRemote;
//    private final Registry<String, SceneController> sceneControllerRegistry;

    public SceneCoreManager() throws InitializationException, InterruptedException {
        try {
            this.sceneRegistryRemote = new SceneRegistryRemote();
            this.sceneRegistryRemote.init();
            this.sceneRegistryRemote.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        if (sceneRegistryRemote != null) {
            sceneRegistryRemote.shutdown();
        }
    }

    public static void main(String args[]) throws Throwable {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPSceneRegistryScope.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPInitializeDB.class);

        JPService.parseAndExitOnError(args);

        try {
            new SceneCoreManager();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
