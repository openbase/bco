/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.agent;

import de.citec.agm.remote.AgentRegistryRemote;
import de.citec.dal.registry.ActivatableEntryRegistrySynchronizer;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.storage.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.state.ActivationStateType.ActivationState;

/**
 *
 * @author mpohling
 */
public class AgentScheduler {

    protected static final Logger logger = LoggerFactory.getLogger(AgentScheduler.class);

    private final AgentFactory factory;
    private final Registry<String, Agent> agentRegistry;
    private final AgentRegistryRemote agentRegistryRemote;

    private final ActivatableEntryRegistrySynchronizer<String, Agent, AgentConfig, AgentConfig.Builder> registrySynchronizer;

    public AgentScheduler() throws InstantiationException, InterruptedException {
        logger.info("Starting agent scheduler");
        try {
            this.factory = new AgentFactory();
            this.agentRegistry = new Registry<>();

            agentRegistryRemote = new AgentRegistryRemote();

            this.registrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, Agent, AgentConfig, AgentConfig.Builder>(agentRegistry, agentRegistryRemote.getAgentConfigRemoteRegistry(), factory) {

                @Override
                public boolean activationCondition(final AgentConfig config) {
                    return config.getActivationState().getValue() == ActivationState.State.ACTIVE;
                }                
            };

            agentRegistryRemote.init();
            agentRegistryRemote.activate();
            registrySynchronizer.init();
            logger.info("waiting for agents...");

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        try {
            new AgentScheduler();
        } catch (CouldNotPerformException | NullPointerException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
    }
}
