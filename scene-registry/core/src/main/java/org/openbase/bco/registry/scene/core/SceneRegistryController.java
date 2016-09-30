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
import org.openbase.bco.registry.lib.com.AbstractVirtualRegistryController;
import org.openbase.bco.registry.scene.lib.SceneRegistry;
import org.openbase.bco.registry.scene.lib.jp.JPSceneRegistryScope;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.storage.registry.RemoteRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.control.scene.SceneRegistryDataType.SceneRegistryData;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitRegistryDataType;
import rst.homeautomation.unit.UnitRegistryDataType.UnitRegistryData;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class SceneRegistryController extends AbstractVirtualRegistryController<SceneRegistryData, SceneRegistryData.Builder> implements SceneRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneConfig.getDefaultInstance()));
    }

    private final UnitRegistryRemote unitRegistryRemote;
    private final RemoteRegistry<String, UnitConfig, UnitConfig.Builder, SceneRegistryData.Builder> sceneUnitConfigRemoteRegistry;

    public SceneRegistryController() throws InstantiationException, InterruptedException {
        super(JPSceneRegistryScope.class, SceneRegistryData.newBuilder());
        unitRegistryRemote = new UnitRegistryRemote();
        sceneUnitConfigRemoteRegistry = new RemoteRegistry<>();
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        super.init();
        unitRegistryRemote.addDataObserver(new Observer<UnitRegistryDataType.UnitRegistryData>() {

            @Override
            public void update(Observable<UnitRegistryData> source, UnitRegistryData data) throws Exception {
                sceneUnitConfigRemoteRegistry.notifyRegistryUpdate(data.getSceneUnitConfigList());
                setDataField(SceneRegistryData.SCENE_UNIT_CONFIG_FIELD_NUMBER, data.getSceneUnitConfigList());
                setDataField(SceneRegistryData.SCENE_UNIT_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, data.getSceneUnitConfigRegistryConsistent());
                setDataField(SceneRegistryData.SCENE_UNIT_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, data.getSceneUnitConfigRegistryReadOnly());
                notifyChange();
            }
        });
    }

    @Override
    public void shutdown() {
        super.shutdown();
        sceneUnitConfigRemoteRegistry.shutdown();
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

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(SceneRegistry.class, this, server);
    }

    @Override
    public Boolean isSceneConfigRegistryReadOnly() throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return getData().getSceneUnitConfigRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isSceneConfigRegistryConsistent() throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return getData().getSceneUnitConfigRegistryConsistent();
    }

    @Override
    public Future<UnitConfig> registerSceneConfig(UnitConfig sceneUnitConfig) throws CouldNotPerformException {
        return unitRegistryRemote.registerUnitConfig(sceneUnitConfig);
    }

    @Override
    public Boolean containsSceneConfig(UnitConfig sceneUnitConfig) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return sceneUnitConfigRemoteRegistry.contains(sceneUnitConfig);
    }

    @Override
    public Boolean containsSceneConfigById(String sceneUnitConfigId) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return sceneUnitConfigRemoteRegistry.contains(sceneUnitConfigId);
    }

    @Override
    public Future<UnitConfig> updateSceneConfig(UnitConfig sceneUnitConfig) throws CouldNotPerformException {
        return unitRegistryRemote.updateUnitConfig(sceneUnitConfig);
    }

    @Override
    public Future<UnitConfig> removeSceneConfig(UnitConfig sceneUnitConfig) throws CouldNotPerformException {
        return unitRegistryRemote.removeUnitConfig(sceneUnitConfig);
    }

    @Override
    public UnitConfig getSceneConfigById(String sceneUnitConfigId) throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return sceneUnitConfigRemoteRegistry.getMessage(sceneUnitConfigId);
    }

    @Override
    public List<UnitConfig> getSceneConfigs() throws CouldNotPerformException {
        unitRegistryRemote.validateData();
        return sceneUnitConfigRemoteRegistry.getMessages();
    }
}
