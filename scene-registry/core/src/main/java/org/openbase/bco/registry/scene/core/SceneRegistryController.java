package org.openbase.bco.registry.scene.core;

/*
 * #%L
 * REM SceneRegistry Core
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
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.bco.registry.scene.core.consistency.LabelConsistencyHandler;
import org.openbase.bco.registry.scene.core.consistency.ScopeConsistencyHandler;
import org.openbase.bco.registry.scene.core.dbconvert.SceneConfig_0_To_1_DBConverter;
import org.openbase.bco.registry.scene.lib.SceneRegistry;
import org.openbase.bco.registry.scene.lib.generator.SceneConfigIdGenerator;
import org.openbase.bco.registry.scene.lib.jp.JPSceneConfigDatabaseDirectory;
import org.openbase.bco.registry.scene.lib.jp.JPSceneRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBCommunicationService;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.scene.SceneConfigType;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.control.scene.SceneRegistryDataType.SceneRegistryData;
import rst.rsb.ScopeType;
import rst.spatial.LocationRegistryDataType.LocationRegistryData;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;

/**
 *
 * @author mpohling
 */
public class SceneRegistryController extends RSBCommunicationService<SceneRegistryData, SceneRegistryData.Builder> implements SceneRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneConfigType.SceneConfig.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, SceneConfig, SceneConfig.Builder, SceneRegistryData.Builder> sceneConfigRegistry;

    private final LocationRegistryRemote locationRegistryRemote;
    private Observer<LocationRegistryData> locationRegistryUpdateObserver;

    public SceneRegistryController() throws InstantiationException, InterruptedException {
        super(SceneRegistryData.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            sceneConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(SceneConfig.class, getBuilderSetup(), getDataFieldDescriptor(SceneRegistryData.SCENE_CONFIG_FIELD_NUMBER), new SceneConfigIdGenerator(), JPService.getProperty(JPSceneConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            sceneConfigRegistry.activateVersionControl(SceneConfig_0_To_1_DBConverter.class.getPackage());

            locationRegistryUpdateObserver = new Observer<LocationRegistryData>() {

                @Override
                public void update(final Observable<LocationRegistryData> source, LocationRegistryData data) throws Exception {
                    sceneConfigRegistry.checkConsistency();
                }
            };

            locationRegistryRemote = new LocationRegistryRemote();

            sceneConfigRegistry.loadRegistry();

            sceneConfigRegistry.registerConsistencyHandler(new ScopeConsistencyHandler(locationRegistryRemote));
            sceneConfigRegistry.registerConsistencyHandler(new LabelConsistencyHandler());
            sceneConfigRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, SceneConfig, SceneConfig.Builder>>>() {

                @Override
                public void update(final Observable<Map<String, IdentifiableMessage<String, SceneConfig, SceneConfig.Builder>>> source, Map<String, IdentifiableMessage<String, SceneConfig, SceneConfig.Builder>> data) throws Exception {
                    notifyChange();
                }
            });

        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException, InterruptedException {
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
            locationRegistryRemote.waitForData();
            locationRegistryRemote.addDataObserver(locationRegistryUpdateObserver);
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
        locationRegistryRemote.removeDataObserver(locationRegistryUpdateObserver);
        locationRegistryRemote.deactivate();
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

        locationRegistryRemote.shutdown();
    }

    @Override
    public final void notifyChange() throws CouldNotPerformException, InterruptedException {
        // sync read only flags
        setDataField(SceneRegistryData.SCENE_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, sceneConfigRegistry.isReadOnly());
        super.notifyChange();
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(SceneRegistry.class, this, server);
    }

    @Override
    public Future<SceneConfig> registerSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> sceneConfigRegistry.register(sceneConfig));
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
    public Future<SceneConfig> updateSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> sceneConfigRegistry.update(sceneConfig));
    }

    @Override
    public Future<SceneConfig> removeSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> sceneConfigRegistry.remove(sceneConfig));
    }

    @Override
    public List<SceneConfig> getSceneConfigs() throws CouldNotPerformException {
        return sceneConfigRegistry.getMessages();
    }

    @Override
    public Boolean isSceneConfigRegistryReadOnly() throws CouldNotPerformException {
        return sceneConfigRegistry.isReadOnly();
    }

    public ProtoBufFileSynchronizedRegistry<String, SceneConfig, SceneConfig.Builder, SceneRegistryData.Builder> getSceneConfigRegistry() {
        return sceneConfigRegistry;
    }
}
