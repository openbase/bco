package org.openbase.bco.registry.scene.remote;

/*
 * #%L
 * BCO Registry Scene Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.registry.lib.com.AbstractRegistryRemote;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.scene.lib.SceneRegistry;
import org.openbase.bco.registry.scene.lib.jp.JPSceneRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.pattern.Remote;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.unit.scene.SceneConfigType.SceneConfig;
import rst.domotic.registry.SceneRegistryDataType.SceneRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class SceneRegistryRemote extends AbstractRegistryRemote<SceneRegistryData> implements SceneRegistry, Remote<SceneRegistryData> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneConfig.getDefaultInstance()));
    }

    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> sceneConfigRemoteRegistry;

    public SceneRegistryRemote() throws InstantiationException, InterruptedException {
        super(JPSceneRegistryScope.class, SceneRegistryData.class);
        try {
            sceneConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this, SceneRegistryData.SCENE_UNIT_CONFIG_FIELD_NUMBER);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    protected void registerRemoteRegistries() throws CouldNotPerformException {
        registerRemoteRegistry(sceneConfigRemoteRegistry);
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getSceneConfigRemoteRegistry() {
        return sceneConfigRemoteRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @param sceneUnitConfig
     * @return
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    @Override
    public Future<UnitConfig> registerSceneConfig(final UnitConfig sceneUnitConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(sceneUnitConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register scene config!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param sceneUnitConfigId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public UnitConfig getSceneConfigById(String sceneUnitConfigId) throws CouldNotPerformException, NotAvailableException {
        validateData();
        return sceneConfigRemoteRegistry.getMessage(sceneUnitConfigId);
    }

    @Override
    public Boolean containsSceneConfig(final UnitConfig sceneUnitConfig) throws CouldNotPerformException {
        validateData();
        return sceneConfigRemoteRegistry.contains(sceneUnitConfig);
    }

    @Override
    public Boolean containsSceneConfigById(final String sceneUnitConfigId) throws CouldNotPerformException {
        validateData();
        return sceneConfigRemoteRegistry.contains(sceneUnitConfigId);
    }

    @Override
    public Future<UnitConfig> updateSceneConfig(final UnitConfig sceneUnitConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(sceneUnitConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update scene config!", ex);
        }
    }

    @Override
    public Future<UnitConfig> removeSceneConfig(final UnitConfig sceneUnitConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(sceneUnitConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove scene config!", ex);
        }
    }

    @Override
    public List<UnitConfig> getSceneConfigs() throws CouldNotPerformException, NotAvailableException {
        validateData();
        List<UnitConfig> messages = sceneConfigRemoteRegistry.getMessages();
        return messages;
    }

    @Override
    public Boolean isSceneConfigRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

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
        try {
            validateData();
            return getData().getSceneUnitConfigRegistryReadOnly();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }
}
