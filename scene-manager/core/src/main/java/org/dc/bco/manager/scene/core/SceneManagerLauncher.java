/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.scene.core;

import org.dc.bco.manager.scene.lib.SceneManager;
import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine
 * Threepwood</a>
 */
public class SceneManagerLauncher {

    protected static final Logger logger = LoggerFactory.getLogger(SceneManagerLauncher.class);

    private final SceneManagerController sceneManagerController;

    public SceneManagerLauncher() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        try {
            this.sceneManagerController = new SceneManagerController();
        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    public void launch() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        try {
            sceneManagerController.init();
        } catch (CouldNotPerformException ex) {
            sceneManagerController.shutdown();
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    public void shutdown() {
        sceneManagerController.shutdown();
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.dc.jul.exception.CouldNotPerformException
     */
    public static void main(final String[] args) throws InterruptedException, CouldNotPerformException {

        /* Setup JPService */
        JPService.setApplicationName(SceneManager.class);
        JPService.parseAndExitOnError(args);

        /* Start main app */
        logger.info("Start " + JPService.getApplicationName() + "...");
        try {
            new SceneManagerLauncher().launch();
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }
}
