package org.openbase.bco.registry.agent.core;

/*
 * #%L
 * REM AgentRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.openbase.bco.registry.agent.core.dbconvert.AgentConfig_0_To_1_DBConverter;
import org.openbase.bco.registry.agent.lib.AgentRegistry;
import org.openbase.bco.registry.agent.lib.generator.AgentClassIdGenerator;
import org.openbase.bco.registry.agent.lib.jp.JPAgentClassDatabaseDirectory;
import org.openbase.bco.registry.agent.lib.jp.JPAgentRegistryScope;
import org.openbase.bco.registry.lib.AbstractRegistryController;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.agent.AgentClassType;
import rst.homeautomation.control.agent.AgentClassType.AgentClass;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.agent.AgentRegistryDataType.AgentRegistryData;

/**
 *
 * @author mpohling
 */
public class AgentRegistryController extends AbstractRegistryController<AgentRegistryData, AgentRegistryData.Builder> implements AgentRegistry {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentClass.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, AgentClass, AgentClass.Builder, AgentRegistryData.Builder> agentClassRegistry;

    private final LocationRegistryRemote locationRegistryRemote;

    public AgentRegistryController() throws InstantiationException, InterruptedException {
        super(JPAgentRegistryScope.class, AgentRegistryData.newBuilder());
        try {
            agentClassRegistry = new ProtoBufFileSynchronizedRegistry<>(AgentClass.class, getBuilderSetup(), getDataFieldDescriptor(AgentRegistryData.AGENT_CLASS_FIELD_NUMBER), new AgentClassIdGenerator(), JPService.getProperty(JPAgentClassDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            locationRegistryRemote = new LocationRegistryRemote();
        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void activateVersionControl() throws CouldNotPerformException {
        agentClassRegistry.activateVersionControl(AgentConfig_0_To_1_DBConverter.class.getPackage());
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void loadRegistries() throws CouldNotPerformException {
        agentClassRegistry.loadRegistry();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException {
        // TODO: Implement basic consistency handler if needed
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRegistryRemotes() throws CouldNotPerformException {
        registerRegistryRemote(locationRegistryRemote);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerObserver() throws CouldNotPerformException {
        agentClassRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, AgentClassType.AgentClass, AgentClassType.AgentClass.Builder>>> source, Map<String, IdentifiableMessage<String, AgentClassType.AgentClass, AgentClassType.AgentClass.Builder>> data) -> {
            notifyChange();
        });
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerDependencies() throws CouldNotPerformException {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void removeDependencies() throws CouldNotPerformException {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void performInitialConsistencyCheck() throws CouldNotPerformException, InterruptedException {
        try {
            agentClassRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }
    }

    @Override
    public void shutdown() {
        if (agentClassRegistry != null) {
            agentClassRegistry.shutdown();
        }
        super.shutdown();
    }

    @Override
    public final void syncDataTypeFlags() throws CouldNotPerformException, InterruptedException {
        setDataField(AgentRegistryData.AGENT_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, agentClassRegistry.isReadOnly());
        setDataField(AgentRegistryData.AGENT_CLASS_REGISTRY_CONSISTENT_FIELD_NUMBER, agentClassRegistry.isConsistent());
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(AgentRegistry.class, this, server);
    }

    @Override
    public Future<AgentConfig> registerAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> agentConfigRegistry.register(agentConfig));
    }

    @Override
    public AgentConfig getAgentConfigById(String agentConfigId) throws CouldNotPerformException {
        return agentConfigRegistry.get(agentConfigId).getMessage();
    }

    @Override
    public Boolean containsAgentConfigById(String agentConfigId) throws CouldNotPerformException {
        return agentConfigRegistry.contains(agentConfigId);
    }

    @Override
    public Boolean containsAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException {
        return agentConfigRegistry.contains(agentConfig);
    }

    @Override
    public Future<AgentConfig> updateAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> agentConfigRegistry.update(agentConfig));
    }

    @Override
    public Future<AgentConfig> removeAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> agentConfigRegistry.remove(agentConfig));
    }

    @Override
    public List<AgentConfig> getAgentConfigs() throws CouldNotPerformException {
        return agentConfigRegistry.getMessages();
    }

    @Override
    public Boolean isAgentConfigRegistryReadOnly() throws CouldNotPerformException {
        return agentConfigRegistry.isReadOnly();
    }

    public ProtoBufFileSynchronizedRegistry<String, AgentConfig, AgentConfig.Builder, AgentRegistryData.Builder> getAgentConfigRegistry() {
        return agentConfigRegistry;
    }

    @Override
    public List<AgentConfig> getAgentConfigsByAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return getAgentConfigsByAgentClassId(agentClass.getId());
    }

    @Override
    public List<AgentConfig> getAgentConfigsByAgentClassId(String agentClassId) throws CouldNotPerformException {
        if (!containsAgentClassById(agentClassId)) {
            throw new NotAvailableException("agentClassId [" + agentClassId + "]");
        }

        List<AgentConfig> agentConfigs = new ArrayList<>();
        for (AgentConfig agentConfig : getAgentConfigs()) {
            if (agentConfig.getAgentClassId().equals(agentClassId)) {
                agentConfigs.add(agentConfig);
            }
        }
        return agentConfigs;
    }

    @Override
    public Future<AgentClass> registerAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> agentClassRegistry.register(agentClass));
    }

    @Override
    public Boolean containsAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return agentClassRegistry.contains(agentClass);
    }

    @Override
    public Boolean containsAgentClassById(String agentClassId) throws CouldNotPerformException {
        return agentClassRegistry.contains(agentClassId);
    }

    @Override
    public Future<AgentClass> updateAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> agentClassRegistry.update(agentClass));
    }

    @Override
    public Future<AgentClass> removeAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> agentClassRegistry.remove(agentClass));
    }

    @Override
    public List<AgentClass> getAgentClasses() throws CouldNotPerformException {
        return agentClassRegistry.getMessages();
    }

    @Override
    public Boolean isAgentClassRegistryReadOnly() throws CouldNotPerformException {
        return agentClassRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isAgentClassRegistryConsistent() throws CouldNotPerformException {
        return agentClassRegistry.isConsistent();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isAgentConfigRegistryConsistent() throws CouldNotPerformException {
        return agentConfigRegistry.isConsistent();
    }

}
