package org.dc.bco.registry.agent.remote;

/*
 * #%L
 * REM AgentRegistry Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.dc.bco.registry.agent.lib.jp.JPAgentRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.preset.JPReadOnly;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBRemoteService;
import org.dc.jul.extension.rsb.scope.ScopeTransformer;
import org.dc.jul.pattern.Remote;
import org.dc.jul.storage.registry.RemoteRegistry;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.agent.AgentRegistryType.AgentRegistry;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 */
public class AgentRegistryRemote extends RSBRemoteService<AgentRegistry> implements org.dc.bco.registry.agent.lib.AgentRegistry, Remote<AgentRegistry> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentConfig.getDefaultInstance()));
    }

    private final RemoteRegistry<String, AgentConfig, AgentConfig.Builder, AgentRegistry.Builder> agentConfigRemoteRegistry;

    public AgentRegistryRemote() throws InstantiationException, InterruptedException {
        super(AgentRegistry.class);
        try {
            agentConfigRemoteRegistry = new RemoteRegistry<>();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Method initializes the remote with the given scope for the server
     * registry connection.
     *
     * @param scope
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException
     */
    @Override
    public void init(final Scope scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeTransformer.transform(scope));
        } catch (CouldNotTransformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * Method initializes the remote with the given scope for the server
     * registry connection.
     *
     * @param scope
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException
     */
    @Override
    public synchronized void init(final ScopeType.Scope scope) throws InitializationException, InterruptedException {
        super.init(scope);
    }

    /**
     * Method initializes the remote with the default registry connection scope.
     *
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    public void init() throws InitializationException, InterruptedException {
        try {
            this.init(JPService.getProperty(JPAgentRegistryScope.class).getValue());
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

//    @Override
//    public void activate() throws InterruptedException, CouldNotPerformException {
//        super.activate();
//        try {
//            waitForData();
//        } catch (CouldNotPerformException ex) {
//            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial registry sync failed!", ex), logger, LogLevel.ERROR);
//        }
//    }
    @Override
    public void shutdown() {
        try {
            agentConfigRemoteRegistry.shutdown();
        } finally {
            super.shutdown();
        }
    }

    @Override
    public void notifyDataUpdate(final AgentRegistry data) throws CouldNotPerformException {
        agentConfigRemoteRegistry.notifyRegistryUpdate(data.getAgentConfigList());
    }

    public RemoteRegistry<String, AgentConfig, AgentConfig.Builder, AgentRegistry.Builder> getAgentConfigRemoteRegistry() {
        return agentConfigRemoteRegistry;
    }

    @Override
    public Future<AgentConfig> registerAgentConfig(final AgentConfig agentConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(agentConfig, this, AgentConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register agent config!", ex);
        }
    }

    @Override
    public AgentConfig getAgentConfigById(String agentConfigId) throws CouldNotPerformException, NotAvailableException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return agentConfigRemoteRegistry.getMessage(agentConfigId);
    }

    @Override
    public Boolean containsAgentConfig(final AgentConfig agentConfig) throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return agentConfigRemoteRegistry.contains(agentConfig);
    }

    @Override
    public Boolean containsAgentConfigById(final String agentConfigId) throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        return agentConfigRemoteRegistry.contains(agentConfigId);
    }

    @Override
    public Future<AgentConfig> updateAgentConfig(final AgentConfig agentConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(agentConfig, this, AgentConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update agent config!", ex);
        }
    }

    @Override
    public Future<AgentConfig> removeAgentConfig(final AgentConfig agentConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(agentConfig, this, AgentConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove agent config!", ex);
        }
    }

    @Override
    public List<AgentConfig> getAgentConfigs() throws CouldNotPerformException, NotAvailableException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        List<AgentConfig> messages = agentConfigRemoteRegistry.getMessages();
        return messages;
    }

    @Override
    public Boolean isAgentConfigRegistryReadOnly() throws CouldNotPerformException {
        waitForData(DATA_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getAgentConfigRegistryReadOnly();
    }
}
