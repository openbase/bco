/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.agm.core;

import de.citec.agm.remote.AgentRegistryRemote;
import static org.dc.bco.coma.agm.core.AgentManager.logger;
import org.dc.bco.coma.agm.lib.jp.JPAgentId;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.preset.JPVerbose;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;

/**
 *
 * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class AgentTest {

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws de.citec.jul.exception.InstantiationException
     */
    public static void main(String[] args) throws InterruptedException, Exception {

        /* Setup JPService */
        JPService.setApplicationName(AgentTest.class);
        JPService.registerProperty(JPAgentId.class, "TestPersonLight");
        JPService.registerProperty(JPVerbose.class, true);
        JPService.parseAndExitOnError(args);

        /* Start main app */
        logger.info("Start " + JPService.getApplicationName() + "...");
        try {
            AgentRegistryRemote agentRegistryRemote = new AgentRegistryRemote();
            agentRegistryRemote.init();
            agentRegistryRemote.activate();
            AgentFactoryImpl.getInstance().newInstance(agentRegistryRemote.getAgentConfigById(JPService.getProperty(JPAgentId.class).getValue())).activate();
            agentRegistryRemote.deactivate();
        } catch (JPServiceException | CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }
}
