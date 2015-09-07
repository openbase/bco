/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.agent;

import de.citec.agm.remote.AgentRegistryRemote;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.agent.AgentRegistryType;

/**
 *
 * @author mpohling
 */
public class AgentScheduler {

    private final AgentFactory factory;
    private final HashMap<String, AgentInterface> agentMap;
    private final AgentRegistryRemote agentRegistryRemote;


    public AgentScheduler() throws InstantiationException, InterruptedException {
        try {
            this.factory = new AgentFactory();
            agentMap = new HashMap<>();

            agentRegistryRemote = new AgentRegistryRemote();
            agentRegistryRemote.addObserver(new Observer<AgentRegistryType.AgentRegistry>() {

                @Override
                public void update(Observable<AgentRegistryType.AgentRegistry> source, AgentRegistryType.AgentRegistry data) throws Exception {
                    updateAgents(data);
                }
            });
            agentRegistryRemote.init();
            agentRegistryRemote.activate();
            System.out.println("waiting for agents...");

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private void updateAgents(final AgentRegistryType.AgentRegistry data) throws InterruptedException {

        // add new agents
        for (AgentConfig config : data.getAgentConfigList()) {

            if (!agentMap.containsKey(config.getId())) {
                try {
                    agentMap.put(config.getId(), createAgent(config));
                    
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(null, ex);
                }
            }
        }

        boolean found;
        // remove outdated agents
        for (AgentInterface agent : new ArrayList<>(agentMap.values())) {
            found = false;
            for (AgentConfig config : data.getAgentConfigList()) {
                try {
                    if (agent.getConfig().getId().equals(config.getId())) {
                        found = true;
                        break;
                    }
                } catch (NotAvailableException ex) {
                    continue;
                }
            }

            if (!found) {
                try {
                    agentMap.remove(agent.getConfig().getId()).deactivate();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(null, ex);
                }
            }
        }
    }

    public AgentInterface createAgent(final AgentConfig config) throws CouldNotPerformException {
        AgentInterface agent = factory.newAgent(config);
        try {
            agent.activate();
        } catch (InterruptedException ex) {
            ExceptionPrinter.printHistory(null, ex);
        }
        return agent;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws de.citec.jul.exception.InstantiationException, InterruptedException, CouldNotPerformException, ExecutionException {
        new AgentScheduler();
    }
}
