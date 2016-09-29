package org.openbase.bco.registry.agent.remote;

/*
 * #%L
 * REM AgentRegistry Remote
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.registry.agent.lib.AgentRegistry;
import org.openbase.bco.registry.agent.lib.jp.JPAgentRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBRemoteService;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.pattern.Remote;
import org.openbase.jul.storage.registry.RemoteRegistry;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.agent.AgentClassType.AgentClass;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.agent.AgentRegistryDataType.AgentRegistryData;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class AgentRegistryRemote extends RSBRemoteService<AgentRegistryData> implements AgentRegistry, Remote<AgentRegistryData> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentClass.getDefaultInstance()));
    }

    private final RemoteRegistry<String, UnitConfig, UnitConfig.Builder, AgentRegistryData.Builder> agentUnitConfigRemoteRegistry;
    private final RemoteRegistry<String, AgentClass, AgentClass.Builder, AgentRegistryData.Builder> agentClassRemoteRegistry;

    public AgentRegistryRemote() throws InstantiationException {
        super(AgentRegistryData.class);
        try {
            agentUnitConfigRemoteRegistry = new RemoteRegistry<>();
            agentClassRemoteRegistry = new RemoteRegistry<>();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Method initializes the remote with the given scope for the server
     * registry connection.
     *
     * @param scope {@inheritDoc}
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
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
     * @param scope {@inheritDoc}
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
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

    @Override
    public void shutdown() {
        try {
            agentUnitConfigRemoteRegistry.shutdown();
            agentClassRemoteRegistry.shutdown();
        } finally {
            super.shutdown();
        }
    }

    @Override
    public void notifyDataUpdate(final AgentRegistryData data) throws CouldNotPerformException {
        agentUnitConfigRemoteRegistry.notifyRegistryUpdate(data.getAgentUnitConfigList());
        agentClassRemoteRegistry.notifyRegistryUpdate(data.getAgentClassList());
    }

    public RemoteRegistry<String, UnitConfig, UnitConfig.Builder, AgentRegistryData.Builder> getAgentConfigRemoteRegistry() {
        return agentUnitConfigRemoteRegistry;
    }

    public RemoteRegistry<String, AgentClass, AgentClass.Builder, AgentRegistryData.Builder> getAgentClassRemoteRegistry() {
        return agentClassRemoteRegistry;
    }

    @Override
    public Future<UnitConfig> registerAgentConfig(final UnitConfig agentUnitConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(agentUnitConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register agent config!", ex);
        }
    }

    @Override
    public UnitConfig getAgentConfigById(String agentUnitConfigId) throws CouldNotPerformException, NotAvailableException {
        validateData();
        return agentUnitConfigRemoteRegistry.getMessage(agentUnitConfigId);
    }

    @Override
    public Boolean containsAgentConfig(final UnitConfig agentUnitConfig) throws CouldNotPerformException {
        validateData();
        return agentUnitConfigRemoteRegistry.contains(agentUnitConfig);
    }

    @Override
    public Boolean containsAgentConfigById(final String agentUnitConfigId) throws CouldNotPerformException {
        validateData();
        return agentUnitConfigRemoteRegistry.contains(agentUnitConfigId);
    }

    @Override
    public Future<UnitConfig> updateAgentConfig(final UnitConfig agentUnitConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(agentUnitConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update agent config!", ex);
        }
    }

    @Override
    public Future<UnitConfig> removeAgentConfig(final UnitConfig agentUnitConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(agentUnitConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove agent config!", ex);
        }
    }

    @Override
    public List<UnitConfig> getAgentConfigs() throws CouldNotPerformException, NotAvailableException {
        validateData();
        List<UnitConfig> messages = agentUnitConfigRemoteRegistry.getMessages();
        return messages;
    }

    @Override
    public Boolean isAgentConfigRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        validateData();
        return getData().getAgentUnitConfigRegistryReadOnly();
    }

    @Override
    public List<UnitConfig> getAgentConfigsByAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return getAgentConfigsByAgentClassId(agentClass.getId());
    }

    @Override
    public List<UnitConfig> getAgentConfigsByAgentClassId(String agentClassId) throws CouldNotPerformException {
        if (!containsAgentClassById(agentClassId)) {
            throw new NotAvailableException("agentClassId [" + agentClassId + "]");
        }

        List<UnitConfig> agentConfigs = new ArrayList<>();
        for (UnitConfig agentConfig : getAgentConfigs()) {
            if (agentConfig.getAgentConfig().getAgentClassId().equals(agentClassId)) {
                agentConfigs.add(agentConfig);
            }
        }
        return agentConfigs;
    }

    @Override
    public Future<AgentClass> registerAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(agentClass, this, AgentClass.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register agent class!", ex);
        }
    }

    @Override
    public Boolean containsAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        validateData();
        return agentClassRemoteRegistry.contains(agentClass);
    }

    @Override
    public Boolean containsAgentClassById(String agentClassId) throws CouldNotPerformException {
        validateData();
        return agentClassRemoteRegistry.contains(agentClassId);
    }

    @Override
    public Future<AgentClass> updateAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(agentClass, this, AgentClass.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update agent class!", ex);
        }
    }

    @Override
    public Future<AgentClass> removeAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(agentClass, this, AgentClass.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove agent class!", ex);
        }
    }

    @Override
    public List<AgentClass> getAgentClasses() throws CouldNotPerformException {
        validateData();
        return agentClassRemoteRegistry.getMessages();
    }

    @Override
    public Boolean isAgentClassRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        validateData();
        return getData().getAgentClassRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isAgentClassRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getAgentClassRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isAgentConfigRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getAgentUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }
}
