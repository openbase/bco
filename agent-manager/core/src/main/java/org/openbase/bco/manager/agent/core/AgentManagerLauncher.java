package org.openbase.bco.manager.agent.core;

/*
 * #%L
 * COMA AgentManager Core
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import org.openbase.bco.manager.agent.lib.AgentManager;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class AgentManagerLauncher implements Launcher {

    protected static final Logger logger = LoggerFactory.getLogger(AgentManagerLauncher.class);

    private final AgentManagerController agentManagerController;

    public AgentManagerLauncher() throws InstantiationException, InterruptedException {
        try {
            this.agentManagerController = new AgentManagerController();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public boolean launch() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            agentManagerController.init();
        } catch (CouldNotPerformException ex) {
            agentManagerController.shutdown();
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
        return true;
    }

    @Override
    public void shutdown() {
        agentManagerController.shutdown();
    }

    public AgentManager getAgentManager() {
        return agentManagerController;
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.openbase.jul.exception.CouldNotPerformException
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
            ExceptionPrinter.printHistoryAndExit(JPService.getApplicationName() + " crashed during startup phase!", ex, logger);
            return;
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }

    @Override
    public void loadProperties() {
    }
}
