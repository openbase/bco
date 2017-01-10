package org.openbase.bco.registry.scene.core;

/*
 * #%L
 * BCO Registry Scene Core
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
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.registry.lib.com.AbstractVirtualRegistryController;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.scene.lib.SceneRegistry;
import org.openbase.bco.registry.scene.lib.jp.JPSceneRegistryScope;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.unit.scene.SceneConfigType.SceneConfig;
import rst.domotic.registry.SceneRegistryDataType.SceneRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class SceneRegistryController extends AbstractVirtualRegistryController<SceneRegistryData, SceneRegistryData.Builder, UnitRegistryData> implements SceneRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneConfig.getDefaultInstance()));
    }

    private final UnitRegistryRemote unitRegistryRemote;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> sceneUnitConfigRemoteRegistry;

    public SceneRegistryController() throws InstantiationException, InterruptedException {
        super(JPSceneRegistryScope.class, SceneRegistryData.newBuilder());
        unitRegistryRemote = new UnitRegistryRemote();
        sceneUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(unitRegistryRemote, UnitRegistryData.SCENE_UNIT_CONFIG_FIELD_NUMBER);
    }

    @Override
    protected void syncVirtualRegistryFields(final SceneRegistryData.Builder virtualDataBuilder, final UnitRegistryData realData) throws CouldNotPerformException {
        virtualDataBuilder.clearSceneUnitConfig();
        virtualDataBuilder.addAllSceneUnitConfig(realData.getSceneUnitConfigList());

        virtualDataBuilder.setSceneUnitConfigRegistryConsistent(realData.getSceneUnitConfigRegistryConsistent());
        virtualDataBuilder.setSceneUnitConfigRegistryReadOnly(realData.getSceneUnitConfigRegistryReadOnly());
    }

    @Override
    protected void registerRemoteRegistries() throws CouldNotPerformException {
        registerRemoteRegistry(sceneUnitConfigRemoteRegistry);
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

    private void verifySceneUnitConfig(UnitConfig unitConfig) throws VerificationFailedException {
        UnitConfigProcessor.verifyUnitConfig(unitConfig, UnitType.SCENE);
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
        verifySceneUnitConfig(sceneUnitConfig);
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
        verifySceneUnitConfig(sceneUnitConfig);
        return unitRegistryRemote.updateUnitConfig(sceneUnitConfig);
    }

    @Override
    public Future<UnitConfig> removeSceneConfig(UnitConfig sceneUnitConfig) throws CouldNotPerformException {
        verifySceneUnitConfig(sceneUnitConfig);
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
