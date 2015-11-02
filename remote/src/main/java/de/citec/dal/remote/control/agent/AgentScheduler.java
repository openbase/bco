/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.agent;

import de.citec.agm.remote.AgentRegistryRemote;
import de.citec.dal.registry.RegistrySynchronizer;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.storage.registry.Registry;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.state.ActivationStateType;

/**
 *
 * @author mpohling
 */
public class AgentScheduler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final AgentFactory factory;
    private final Registry<String, Agent> agentRegistry;
    private final AgentRegistryRemote agentRegistryRemote;

    private final RegistrySynchronizer<String, Agent, AgentConfig, AgentConfig.Builder> registrySynchronizer;

    public AgentScheduler() throws InstantiationException, InterruptedException {
        logger.info("Starting agent scheduler");
        try {
            this.factory = new AgentFactory();
            this.agentRegistry = new Registry<>();

            agentRegistryRemote = new AgentRegistryRemote();

            this.registrySynchronizer = new RegistrySynchronizer<String, Agent, AgentConfig, AgentConfig.Builder>(agentRegistry, agentRegistryRemote.getAgentConfigRemoteRegistry(), factory) {

                @Override
                public boolean verifyConfig(AgentConfig config) {
                    return config.getActivationState().getValue() == ActivationStateType.ActivationState.State.ACTIVE;
                }

                @Override
                public Agent register(AgentConfig config) throws CouldNotPerformException, InterruptedException {
                    Agent agent = super.register(config);
                    agent.activate();
                    return agent;
                }

                @Override
                public Agent remove(AgentConfig config) throws CouldNotPerformException, InterruptedException {
                    Agent agent = super.remove(config);
                    agent.deactivate();
                    return agent;
                }
            };

            agentRegistryRemote.init();
            agentRegistryRemote.activate();
            logger.info("waiting for agents...");

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

//    private void updateAgents(final AgentRegistryType.AgentRegistry data) throws InterruptedException {
//        logger.info("Updating agents..");
//
//        agentDiffList.diff(data.getAgentConfigList());
//
//        // remove outdated
//        for (AgentConfig agentConfig : agentDiffList.getRemovedMessageMap()) {
//            if (agentRegistry.containsKey(agentConfig.getId())) {
//                agentRegistry.remove(agentConfig.getId()).deactivate();
//            }
//        }
//
//        // update agents
//        for (AgentConfig config : data.getAgentConfigList()) {
//
//            if (!agentRegistry.containsKey(config.getId())) {
//                try {
//                    agentRegistry.put(config.getId(), createAgent(config));
//                } catch (CouldNotPerformException ex) {
//                    ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
//                }
//            }
//        }
//
//        boolean found;
//        // remove outdated agents
//        for (Agent agent : new ArrayList<>(agentRegistry.values())) {
//            found = false;
//            for (AgentConfig config : data.getAgentConfigList()) {
//                try {
//                    if (agent.getConfig().getId().equals(config.getId())) {
//                        found = true;
//                        break;
//                    }
//                } catch (NotAvailableException ex) {
//                    continue;
//                }
//            }
//
//            if (!found) {
//                try {
//                    agentRegistry.remove(agent.getConfig().getId()).deactivate();
//                } catch (CouldNotPerformException ex) {
//                    ExceptionPrinter.printHistory(null, ex);
//                }
//            }
//        }
//    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws de.citec.jul.exception.InstantiationException, InterruptedException, CouldNotPerformException, ExecutionException {
        new AgentScheduler();
    }
}
