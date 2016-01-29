/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.core;

import org.dc.bco.manager.agent.lib.AgentManager;
import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class AgentManagerLauncher {

    protected static final Logger logger = LoggerFactory.getLogger(AgentManagerLauncher.class);

    private final AgentManagerController agentManagerController;

    public AgentManagerLauncher() throws InstantiationException, InterruptedException {
        try {
            this.agentManagerController = new AgentManagerController();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void launch() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        try {
            agentManagerController.init();
        } catch (CouldNotPerformException ex) {
            agentManagerController.shutdown();
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    public void shutdown() {
        agentManagerController.shutdown();
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.dc.jul.exception.CouldNotPerformException
     */
    public static void main(final String[] args) throws InterruptedException, CouldNotPerformException {

        /* Setup JPService */
        JPService.setApplicationName(AgentManager.class);
        JPService.parseAndExitOnError(args);

        /* Start main app */
        logger.info("Start " + JPService.getApplicationName() + "...");
        try {
            new AgentManagerLauncher().launch();
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }
}
