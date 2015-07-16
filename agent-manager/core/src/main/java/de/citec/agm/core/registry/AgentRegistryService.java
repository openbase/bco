/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.agm.core.registry;

import de.citec.agm.lib.generator.AgentConfigIdGenerator;
import de.citec.agm.lib.registry.AgentRegistryInterface;
import de.citec.jp.JPAgentConfigDatabaseDirectory;
import de.citec.jp.JPAgentRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.storage.file.ProtoBufJSonFileProvider;
import de.citec.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import java.util.List;
import java.util.Map;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.extension.rsb.com.RSBCommunicationService;
import de.citec.jul.extension.rsb.iface.RSBLocalServerInterface;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.rsb.util.RPCHelper;
import de.citec.lm.remote.LocationRegistryRemote;
import rst.homeautomation.control.agent.AgentConfigType;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.agent.AgentRegistryType.AgentRegistry;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author mpohling
 */
public class AgentRegistryService extends RSBCommunicationService<AgentRegistry, AgentRegistry.Builder> implements AgentRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentConfigType.AgentConfig.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, AgentConfig, AgentConfig.Builder, AgentRegistry.Builder> agentConfigRegistry;

    private final LocationRegistryRemote locationRegistryRemote;
    private Observer<LocationRegistry> locationRegistryUpdateObserver;

    public AgentRegistryService() throws InstantiationException, InterruptedException {
        super(JPService.getProperty(JPAgentRegistryScope.class).getValue(), AgentRegistry.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            agentConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(AgentConfig.class, getBuilderSetup(), getFieldDescriptor(AgentRegistry.AGENT_CONFIG_FIELD_NUMBER), new AgentConfigIdGenerator(), JPService.getProperty(JPAgentConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            locationRegistryUpdateObserver = new Observer<LocationRegistry>() {

                @Override
                public void update(Observable<LocationRegistry> source, LocationRegistry data) throws Exception {
                    agentConfigRegistry.checkConsistency();
                }
            };

            locationRegistryRemote = new LocationRegistryRemote();

            agentConfigRegistry.loadRegistry();

//            agentConfigRegistry.registerConsistencyHandler(new AgentIdConsistencyHandler());
//            agentConfigRegistry.registerConsistencyHandler(new AgentConfigAgentClassConsistencyHandler(agentClassRegistry));
//            agentConfigRegistry.registerConsistencyHandler(new AgentLabelConsistencyHandler());
//            agentConfigRegistry.registerConsistencyHandler(new AgentLocationIdConsistencyHandler(locationRegistryRemote));
//            agentConfigRegistry.registerConsistencyHandler(new AgentScopeConsistencyHandler(locationRegistryRemote));
//            agentConfigRegistry.registerConsistencyHandler(new UnitIdConsistencyHandler());
//            agentConfigRegistry.registerConsistencyHandler(new UnitTemplateConsistencyHandler());
//            agentConfigRegistry.registerConsistencyHandler(new UnitLabelConsistencyHandler());
//            agentConfigRegistry.registerConsistencyHandler(new UnitLocationIdConsistencyHandler(locationRegistryRemote));
//            agentConfigRegistry.registerConsistencyHandler(new UnitScopeConsistencyHandler(locationRegistryRemote));
//            agentConfigRegistry.registerConsistencyHandler(new ServiceConfigUnitIdConsistencyHandler());
//            agentConfigRegistry.registerConsistencyHandler(new ServiceConfigBindingTypeConsistencyHandler());
//            agentConfigRegistry.registerConsistencyHandler(new OpenhabServiceConfigItemIdConsistenyHandler(locationRegistryRemote));
//            agentConfigRegistry.registerConsistencyHandler(new TransformationConsistencyHandler());
            agentConfigRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, AgentConfig, AgentConfig.Builder>>>() {

                @Override
                public void update(Observable<Map<String, IdentifiableMessage<String, AgentConfig, AgentConfig.Builder>>> source, Map<String, IdentifiableMessage<String, AgentConfig, AgentConfig.Builder>> data) throws Exception {
                    notifyChange();
                }
            });

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException {
        super.init();
        locationRegistryRemote.init();
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            super.activate();
            locationRegistryRemote.activate();
            locationRegistryRemote.addObserver(locationRegistryUpdateObserver);
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
        locationRegistryRemote.removeObserver(locationRegistryUpdateObserver);
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
            ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
        }
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(AgentRegistryInterface.class, this, server);
    }

    @Override
    public AgentConfig registerAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException {
        return agentConfigRegistry.register(agentConfig);
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
    public AgentConfig updateAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException {
        return agentConfigRegistry.update(agentConfig);
    }

    @Override
    public AgentConfig removeAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException {
        return agentConfigRegistry.remove(agentConfig);
    }

    @Override
    public List<AgentConfig> getAgentConfigs() throws CouldNotPerformException {
        return agentConfigRegistry.getMessages();
    }
}
