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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.openbase.bco.registry.agent.core.consistency.LabelConsistencyHandler;
import org.openbase.bco.registry.agent.core.consistency.LocationIdConsistencyHandler;
import org.openbase.bco.registry.agent.core.consistency.ScopeConsistencyHandler;
import org.openbase.bco.registry.agent.core.dbconvert.AgentConfig_0_To_1_DBConverter;
import org.openbase.bco.registry.agent.lib.generator.AgentConfigIdGenerator;
import org.openbase.bco.registry.agent.lib.jp.JPAgentConfigDatabaseDirectory;
import org.openbase.bco.registry.agent.lib.jp.JPAgentRegistryScope;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBCommunicationService;
import org.openbase.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.agent.AgentConfigType;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.agent.AgentRegistryType.AgentRegistry;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author mpohling
 */
public class AgentRegistryController extends RSBCommunicationService<AgentRegistry, AgentRegistry.Builder> implements org.openbase.bco.registry.agent.lib.AgentRegistry {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentConfigType.AgentConfig.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, AgentConfig, AgentConfig.Builder, AgentRegistry.Builder> agentConfigRegistry;

    private final LocationRegistryRemote locationRegistryRemote;
    private Observer<LocationRegistry> locationRegistryUpdateObserver;

    public AgentRegistryController() throws InstantiationException, InterruptedException {
        super(AgentRegistry.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            agentConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(AgentConfig.class, getBuilderSetup(), getDataFieldDescriptor(AgentRegistry.AGENT_CONFIG_FIELD_NUMBER), new AgentConfigIdGenerator(), JPService.getProperty(JPAgentConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            agentConfigRegistry.activateVersionControl(AgentConfig_0_To_1_DBConverter.class.getPackage());

            locationRegistryUpdateObserver = new Observer<LocationRegistry>() {

                @Override
                public void update(final Observable<LocationRegistry> source, LocationRegistry data) throws Exception {
                    agentConfigRegistry.checkConsistency();
                }
            };

            locationRegistryRemote = new LocationRegistryRemote();

            agentConfigRegistry.setName("AgentConfigRegistry");
            agentConfigRegistry.loadRegistry();

            agentConfigRegistry.registerConsistencyHandler(new LocationIdConsistencyHandler(locationRegistryRemote));
            agentConfigRegistry.registerConsistencyHandler(new LabelConsistencyHandler());
            agentConfigRegistry.registerConsistencyHandler(new ScopeConsistencyHandler(locationRegistryRemote));
            agentConfigRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, AgentConfig, AgentConfig.Builder>>>() {

                @Override
                public void update(final Observable<Map<String, IdentifiableMessage<String, AgentConfig, AgentConfig.Builder>>> source, Map<String, IdentifiableMessage<String, AgentConfig, AgentConfig.Builder>> data) throws Exception {
                    notifyChange();
                }
            });

        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            init(JPService.getProperty(JPAgentRegistryScope.class).getValue());
            locationRegistryRemote.init();
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            super.activate();
            locationRegistryRemote.activate();
            locationRegistryRemote.waitForData();
            locationRegistryRemote.addDataObserver(locationRegistryUpdateObserver);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate location registry!", ex);
        }

        try {
            agentConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            logger.warn("Initial consistency check failed!");
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        locationRegistryRemote.removeDataObserver(locationRegistryUpdateObserver);
        locationRegistryRemote.deactivate();
        super.deactivate();
    }

    @Override
    public void shutdown() {
        if (agentConfigRegistry != null) {
            agentConfigRegistry.shutdown();
        }

        try {
            deactivate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
        
        locationRegistryRemote.shutdown();
    }

    @Override
    public final void notifyChange() throws CouldNotPerformException {
        // sync read only flags
        setDataField(AgentRegistry.AGENT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, agentConfigRegistry.isReadOnly());
        super.notifyChange();
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(org.openbase.bco.registry.agent.lib.AgentRegistry.class, this, server);
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

    public ProtoBufFileSynchronizedRegistry<String, AgentConfig, AgentConfig.Builder, AgentRegistry.Builder> getAgentConfigRegistry() {
        return agentConfigRegistry;
    }
}
