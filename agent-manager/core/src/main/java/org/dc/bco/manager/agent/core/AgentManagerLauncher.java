/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.core;

import org.dc.bco.registry.agent.remote.AgentRegistryRemote;
import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.dc.jul.storage.registry.RegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.state.ActivationStateType.ActivationState;

/**
 *
 * @author mpohling
 */
public class AgentManagerLauncher {

    protected static final Logger logger = LoggerFactory.getLogger(AgentManagerLauncher.class);

    private final AgentFactory factory;
    private final RegistryImpl<String, AgentController> agentRegistry;
    private final AgentRegistryRemote agentRegistryRemote;
    private final ActivatableEntryRegistrySynchronizer<String, AgentController, AgentConfig, AgentConfig.Builder> registrySynchronizer;

    public AgentManagerLauncher() throws InstantiationException, InterruptedException {
        try {
            this.factory = AgentFactoryImpl.getInstance();
            this.agentRegistry = new RegistryImpl<>();

            agentRegistryRemote = new AgentRegistryRemote();

            this.registrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, AgentController, AgentConfig, AgentConfig.Builder>(agentRegistry, agentRegistryRemote.getAgentConfigRemoteRegistry(), factory) {

                @Override
                public boolean activationCondition(final AgentConfig config) {
                    return config.getActivationState().getValue() == ActivationState.State.ACTIVE;
                }
            };

            agentRegistryRemote.init();
            agentRegistryRemote.activate();
            registrySynchronizer.init();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.dc.jul.exception.CouldNotPerformException
     */
    public static void main(String[] args) throws InterruptedException, CouldNotPerformException {

        /* Setup JPService */
        JPService.setApplicationName(AgentManagerLauncher.class);
        JPService.parseAndExitOnError(args);

        /* Start main app */
        logger.info("Start " + JPService.getApplicationName() + "...");
        try {
            new AgentManagerLauncher();
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }
}
