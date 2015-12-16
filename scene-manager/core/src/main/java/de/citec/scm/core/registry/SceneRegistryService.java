/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.scm.core.registry;

import de.citec.jp.JPSceneConfigDatabaseDirectory;
import de.citec.jp.JPSceneRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.rsb.com.RPCHelper;
import de.citec.jul.extension.rsb.com.RSBCommunicationService;
import de.citec.jul.extension.rsb.iface.RSBLocalServerInterface;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.storage.file.ProtoBufJSonFileProvider;
import de.citec.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import de.citec.lm.remote.LocationRegistryRemote;
import de.citec.scm.lib.generator.SceneConfigIdGenerator;
import de.citec.scm.lib.registry.SceneRegistryInterface;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.scene.SceneConfigType;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.control.scene.SceneRegistryType.SceneRegistry;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author mpohling
 */
public class SceneRegistryService extends RSBCommunicationService<SceneRegistry, SceneRegistry.Builder> implements SceneRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneConfigType.SceneConfig.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, SceneConfig, SceneConfig.Builder, SceneRegistry.Builder> sceneConfigRegistry;

    private final LocationRegistryRemote locationRegistryRemote;
    private Observer<LocationRegistry> locationRegistryUpdateObserver;

    public SceneRegistryService() throws InstantiationException, InterruptedException {
        super(SceneRegistry.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            sceneConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(SceneConfig.class, getBuilderSetup(), getFieldDescriptor(SceneRegistry.SCENE_CONFIG_FIELD_NUMBER), new SceneConfigIdGenerator(), JPService.getProperty(JPSceneConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            locationRegistryUpdateObserver = new Observer<LocationRegistry>() {

                @Override
                public void update(Observable<LocationRegistry> source, LocationRegistry data) throws Exception {
                    sceneConfigRegistry.checkConsistency();
                }
            };

            locationRegistryRemote = new LocationRegistryRemote();

            sceneConfigRegistry.loadRegistry();

//            sceneConfigRegistry.registerConsistencyHandler(new SceneIdConsistencyHandler());
//            sceneConfigRegistry.registerConsistencyHandler(new SceneConfigSceneClassConsistencyHandler(sceneClassRegistry));
//            sceneConfigRegistry.registerConsistencyHandler(new SceneLabelConsistencyHandler());
//            sceneConfigRegistry.registerConsistencyHandler(new SceneLocationIdConsistencyHandler(locationRegistryRemote));
//            sceneConfigRegistry.registerConsistencyHandler(new SceneScopeConsistencyHandler(locationRegistryRemote));
//            sceneConfigRegistry.registerConsistencyHandler(new UnitIdConsistencyHandler());
//            sceneConfigRegistry.registerConsistencyHandler(new UnitLabelConsistencyHandler());
//            sceneConfigRegistry.registerConsistencyHandler(new UnitLocationIdConsistencyHandler(locationRegistryRemote));
//            sceneConfigRegistry.registerConsistencyHandler(new UnitScopeConsistencyHandler(locationRegistryRemote));
//            sceneConfigRegistry.registerConsistencyHandler(new ServiceConfigUnitIdConsistencyHandler());
//            sceneConfigRegistry.registerConsistencyHandler(new ServiceConfigBindingTypeConsistencyHandler());
//            sceneConfigRegistry.registerConsistencyHandler(new OpenhabServiceConfigItemIdConsistenyHandler(locationRegistryRemote));
//            sceneConfigRegistry.registerConsistencyHandler(new TransformationConsistencyHandler());
            sceneConfigRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, SceneConfig, SceneConfig.Builder>>>() {

                @Override
                public void update(Observable<Map<String, IdentifiableMessage<String, SceneConfig, SceneConfig.Builder>>> source, Map<String, IdentifiableMessage<String, SceneConfig, SceneConfig.Builder>> data) throws Exception {
                    notifyChange();
                }
            });

        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        try {
            super.init(JPService.getProperty(JPSceneRegistryScope.class).getValue());
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
            locationRegistryRemote.addObserver(locationRegistryUpdateObserver);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate location registry!", ex);
        }

        try {
            sceneConfigRegistry.checkConsistency();
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

        if (sceneConfigRegistry != null) {
            sceneConfigRegistry.shutdown();
        }

        try {
            deactivate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }

    @Override
    public final void notifyChange() throws CouldNotPerformException {
        // sync read only flags
        setField(SceneRegistry.SCENE_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, sceneConfigRegistry.isReadOnly());
        super.notifyChange();
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(SceneRegistryInterface.class, this, server);
    }

    @Override
    public SceneConfig registerSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException {
        return sceneConfigRegistry.register(sceneConfig);
    }

    @Override
    public SceneConfig getSceneConfigById(String sceneConfigId) throws CouldNotPerformException {
        return sceneConfigRegistry.get(sceneConfigId).getMessage();
    }

    @Override
    public Boolean containsSceneConfigById(String sceneConfigId) throws CouldNotPerformException {
        return sceneConfigRegistry.contains(sceneConfigId);
    }

    @Override
    public Boolean containsSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException {
        return sceneConfigRegistry.contains(sceneConfig);
    }

    @Override
    public SceneConfig updateSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException {
        return sceneConfigRegistry.update(sceneConfig);
    }

    @Override
    public SceneConfig removeSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException {
        return sceneConfigRegistry.remove(sceneConfig);
    }

    @Override
    public List<SceneConfig> getSceneConfigs() throws CouldNotPerformException {
        return sceneConfigRegistry.getMessages();
    }

    @Override
    public Future<Boolean> isSceneConfigRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(sceneConfigRegistry.isReadOnly());
    }

    public ProtoBufFileSynchronizedRegistry<String, SceneConfig, SceneConfig.Builder, SceneRegistry.Builder> getSceneConfigRegistry() {
        return sceneConfigRegistry;
    }
}
