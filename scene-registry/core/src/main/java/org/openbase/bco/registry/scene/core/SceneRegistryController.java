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
import java.util.concurrent.Future;
import org.openbase.bco.registry.lib.controller.AbstractVirtualRegistryController;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.bco.registry.scene.lib.SceneRegistry;
import org.openbase.bco.registry.scene.lib.jp.JPSceneRegistryScope;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.scene.SceneConfigType;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.control.scene.SceneRegistryDataType.SceneRegistryData;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 */
public class SceneRegistryController extends AbstractVirtualRegistryController<SceneRegistryData, SceneRegistryData.Builder> implements SceneRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneConfigType.SceneConfig.getDefaultInstance()));
    }

    private final LocationRegistryRemote locationRegistryRemote;

    public SceneRegistryController() throws InstantiationException, InterruptedException {
        super(JPSceneRegistryScope.class, SceneRegistryData.newBuilder());
        try {
            locationRegistryRemote = new LocationRegistryRemote();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
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

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isSceneConfigRegistryConsistent() throws CouldNotPerformException {
        return sceneConfigRegistry.isConsistent();
    }
}
