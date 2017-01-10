package org.openbase.bco.registry.agent.core;

/*
 * #%L
 * BCO Registry Agent Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.concurrent.Future;
import org.openbase.bco.registry.agent.lib.AgentRegistry;
import org.openbase.bco.registry.agent.lib.generator.AgentClassIdGenerator;
import org.openbase.bco.registry.agent.lib.jp.JPAgentClassDatabaseDirectory;
import org.openbase.bco.registry.agent.lib.jp.JPAgentRegistryScope;
import org.openbase.bco.registry.lib.com.AbstractVirtualRegistryController;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.registry.AgentRegistryDataType.AgentRegistryData;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.domotic.unit.agent.AgentConfigType.AgentConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class AgentRegistryController extends AbstractVirtualRegistryController<AgentRegistryData, AgentRegistryData.Builder, UnitRegistryData> implements AgentRegistry {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentClass.getDefaultInstance()));
    }

    private final ProtoBufFileSynchronizedRegistry<String, AgentClass, AgentClass.Builder, AgentRegistryData.Builder> agentClassRegistry;

    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> agentUnitConfigRemoteRegistry;
    private final UnitRegistryRemote unitRegistryRemote;

    public AgentRegistryController() throws InstantiationException, InterruptedException {
        super(JPAgentRegistryScope.class, AgentRegistryData.newBuilder());
        try {
            unitRegistryRemote = new UnitRegistryRemote();
            agentClassRegistry = new ProtoBufFileSynchronizedRegistry<>(AgentClass.class, getBuilderSetup(), getDataFieldDescriptor(AgentRegistryData.AGENT_CLASS_FIELD_NUMBER), new AgentClassIdGenerator(), JPService.getProperty(JPAgentClassDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            agentUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(unitRegistryRemote, UnitRegistryData.AGENT_UNIT_CONFIG_FIELD_NUMBER);
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
    protected void registerConsistencyHandler() throws CouldNotPerformException {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRegistryRemotes() throws CouldNotPerformException {
        registerRegistryRemote(unitRegistryRemote);
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
    protected void registerPlugins() throws CouldNotPerformException, InterruptedException {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRegistries() throws CouldNotPerformException {
        registerRegistry(agentClassRegistry);
    }

    @Override
    protected void registerRemoteRegistries() throws CouldNotPerformException {
        registerRemoteRegistry(agentUnitConfigRemoteRegistry);
    }

    @Override
    public final void syncRegistryFlags() throws CouldNotPerformException, InterruptedException {
        setDataField(AgentRegistryData.AGENT_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, agentClassRegistry.isReadOnly());
        setDataField(AgentRegistryData.AGENT_CLASS_REGISTRY_CONSISTENT_FIELD_NUMBER, agentClassRegistry.isConsistent());
    }

    @Override
    protected void syncVirtualRegistryFields(final AgentRegistryData.Builder virtualDataBuilder, final UnitRegistryData realData) throws CouldNotPerformException {
        virtualDataBuilder.clearAgentUnitConfig();
        virtualDataBuilder.addAllAgentUnitConfig(realData.getAgentUnitConfigList());

        virtualDataBuilder.setAgentUnitConfigRegistryConsistent(realData.getAgentUnitConfigRegistryConsistent());
        virtualDataBuilder.setAgentUnitConfigRegistryReadOnly(realData.getAgentUnitConfigRegistryReadOnly());
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(AgentRegistry.class, this, server);
    }

    private void verifyAgentUnitConfig(UnitConfig unitConfig) throws VerificationFailedException {
        UnitConfigProcessor.verifyUnitConfig(unitConfig, UnitType.AGENT);
    }

    @Override
    public Future<UnitConfig> registerAgentConfig(UnitConfig agentUnitConfig) throws CouldNotPerformException {
        verifyAgentUnitConfig(agentUnitConfig);
        return unitRegistryRemote.registerUnitConfig(agentUnitConfig);
    }

    @Override
    public UnitConfig getAgentConfigById(String agentUnitConfigId) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return agentUnitConfigRemoteRegistry.getMessage(agentUnitConfigId);
    }

    @Override
    public Boolean containsAgentConfigById(String agentUnitConfigId) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return agentUnitConfigRemoteRegistry.contains(agentUnitConfigId);
    }

    @Override
    public Boolean containsAgentConfig(UnitConfig agentUnitConfig) throws CouldNotPerformException {
        verifyAgentUnitConfig(agentUnitConfig);
        unitRegistryRemote.validateData();
        return agentUnitConfigRemoteRegistry.contains(agentUnitConfig);
    }

    @Override
    public Future<UnitConfig> updateAgentConfig(UnitConfig agentUnitConfig) throws CouldNotPerformException {
        verifyAgentUnitConfig(agentUnitConfig);
        return unitRegistryRemote.updateUnitConfig(agentUnitConfig);
    }

    @Override
    public Future<UnitConfig> removeAgentConfig(UnitConfig agentUnitConfig) throws CouldNotPerformException {
        verifyAgentUnitConfig(agentUnitConfig);
        return unitRegistryRemote.removeUnitConfig(agentUnitConfig);
    }

    @Override
    public List<UnitConfig> getAgentConfigs() throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return agentUnitConfigRemoteRegistry.getMessages();
    }

    @Override
    public Boolean isAgentConfigRegistryReadOnly() throws CouldNotPerformException {
        unitRegistryRemote.validateData();
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

        unitRegistryRemote.validateData();
        List<UnitConfig> agentUnitConfigs = new ArrayList<>();
        for (UnitConfig agentUnitConfig : getAgentConfigs()) {
            if (agentUnitConfig.getAgentConfig().getAgentClassId().equals(agentClassId)) {
                agentUnitConfigs.add(agentUnitConfig);
            }
        }
        return agentUnitConfigs;
    }

    @Override
    public Future<AgentClass> registerAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> agentClassRegistry.register(agentClass));
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
        return GlobalCachedExecutorService.submit(() -> agentClassRegistry.update(agentClass));
    }

    @Override
    public Future<AgentClass> removeAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> agentClassRegistry.remove(agentClass));
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
        unitRegistryRemote.validateData();
        return getData().getAgentUnitConfigRegistryConsistent();
    }

    public ProtoBufFileSynchronizedRegistry<String, AgentClass, AgentClass.Builder, AgentRegistryData.Builder> getAgentClassRegistry() {
        return agentClassRegistry;
    }

    @Override
    public AgentClass getAgentClassById(String agentClassId) throws CouldNotPerformException {
        return agentClassRegistry.getMessage(agentClassId);
    }
}
