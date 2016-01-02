/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.agent.remote;

import org.dc.bco.registry.agent.lib.generator.AgentConfigIdGenerator;
import org.dc.bco.registry.agent.lib.AgentRegistry;
import org.dc.bco.registry.agent.lib.jp.JPAgentRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.preset.JPReadOnly;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBRemoteService;
import org.dc.jul.storage.registry.RemoteRegistry;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.agent.AgentConfigType;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.agent.AgentRegistryType.AgentRegistry;

/**
 *
 * @author mpohling
 */
public class AgentRegistryRemote extends RSBRemoteService<AgentRegistry> implements AgentRegistry {

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
        try {
            super.init(JPService.getProperty(JPAgentRegistryScope.class).getValue());
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        try {
            notifyUpdated(requestStatus());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial registry sync failed!", ex), logger, LogLevel.ERROR);
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

    @Override
    public Future<Boolean> isAgentConfigRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return CompletableFuture.completedFuture(true);
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        try {
            return RPCHelper.callRemoteMethod(this, Boolean.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the agent config registry!!", ex);
        }
    }
}
