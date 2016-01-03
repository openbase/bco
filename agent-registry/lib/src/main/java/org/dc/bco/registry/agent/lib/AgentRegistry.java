/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.agent.lib;

import org.dc.jul.exception.CouldNotPerformException;
import java.util.List;
import java.util.concurrent.Future;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;

/**
 *
 * @author mpohling
 */
public interface AgentRegistry {

    public AgentConfig registerAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException;

    public Boolean containsAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException;

    public Boolean containsAgentConfigById(String agentConfigId) throws CouldNotPerformException;

    public AgentConfig updateAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException;

    public AgentConfig removeAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException;

    public AgentConfig getAgentConfigById(final String agentConfigId) throws CouldNotPerformException;
    
    public List<AgentConfig> getAgentConfigs() throws CouldNotPerformException;
    
     public Future<Boolean> isAgentConfigRegistryReadOnly() throws CouldNotPerformException;

    public void shutdown();
}
