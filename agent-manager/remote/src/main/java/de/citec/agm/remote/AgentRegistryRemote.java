/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.agm.remote;

import de.citec.agm.lib.generator.AgentConfigIdGenerator;
import de.citec.agm.lib.registry.AgentRegistryInterface;
import de.citec.jp.JPAgentRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.rsb.com.RSBRemoteService;
import de.citec.jul.storage.registry.RemoteRegistry;
import java.util.List;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.agent.AgentConfigType;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.agent.AgentRegistryType.AgentRegistry;
import de.citec.jul.exception.InstantiationException;

/**
 *
 * @author mpohling
 */
public class AgentRegistryRemote extends RSBRemoteService<AgentRegistry> implements AgentRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentConfigType.AgentConfig.getDefaultInstance()));
    }

    private final RemoteRegistry<String, AgentConfig, AgentConfig.Builder, AgentRegistry.Builder> agentConfigRemoteRegistry;

    public AgentRegistryRemote() throws InstantiationException {
        try {
            agentConfigRemoteRegistry = new RemoteRegistry<>(new AgentConfigIdGenerator());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        super.init(JPService.getProperty(JPAgentRegistryScope.class).getValue());
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        try {
            notifyUpdated(requestStatus());
        } catch(CouldNotPerformException ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(logger, new CouldNotPerformException("Initial registry sync failed!", ex));
        }
    }

    @Override
    public void notifyUpdated(final AgentRegistry data) throws CouldNotPerformException {
        agentConfigRemoteRegistry.notifyRegistryUpdated(data.getAgentConfigList());
    }

    public RemoteRegistry<String, AgentConfig, AgentConfig.Builder, AgentRegistry.Builder> getAgentConfigRemoteRegistry() {
        return agentConfigRemoteRegistry;
    }
    
    

    @Override
    public AgentConfigType.AgentConfig registerAgentConfig(final AgentConfigType.AgentConfig agentConfig) throws CouldNotPerformException {
        try {
            return (AgentConfigType.AgentConfig) callMethod("registerAgentConfig", agentConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register agent config!", ex);
        }
    }

    @Override
    public AgentConfig getAgentConfigById(String agentConfigId) throws CouldNotPerformException, NotAvailableException {
        getData();
        return agentConfigRemoteRegistry.getMessage(agentConfigId);
    }

    @Override
    public Boolean containsAgentConfig(final AgentConfigType.AgentConfig agentConfig) throws CouldNotPerformException {
        getData();
        return agentConfigRemoteRegistry.contains(agentConfig);
    }

    @Override
    public Boolean containsAgentConfigById(final String agentConfigId) throws CouldNotPerformException {
        getData();
        return agentConfigRemoteRegistry.contains(agentConfigId);
    }

    @Override
    public AgentConfigType.AgentConfig updateAgentConfig(final AgentConfigType.AgentConfig agentConfig) throws CouldNotPerformException {
        try {
            return (AgentConfigType.AgentConfig) callMethod("updateAgentConfig", agentConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update agent config!", ex);
        }
    }

    @Override
    public AgentConfigType.AgentConfig removeAgentConfig(final AgentConfigType.AgentConfig agentConfig) throws CouldNotPerformException {
        try {
            return (AgentConfigType.AgentConfig) callMethod("removeAgentConfig", agentConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove agent config!", ex);
        }
    }

    @Override
    public List<AgentConfig> getAgentConfigs() throws CouldNotPerformException, NotAvailableException {
        getData();
        List<AgentConfig> messages = agentConfigRemoteRegistry.getMessages();
        return messages;
    }
}
