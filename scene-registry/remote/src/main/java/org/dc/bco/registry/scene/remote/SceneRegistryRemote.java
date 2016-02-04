/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.scene.remote;

/*
 * #%L
 * REM SceneRegistry Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.dc.bco.registry.scene.lib.generator.SceneConfigIdGenerator;
import org.dc.bco.registry.scene.lib.jp.JPSceneRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.preset.JPReadOnly;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBRemoteService;
import org.dc.jul.extension.rsb.scope.ScopeTransformer;
import org.dc.jul.pattern.Remote;
import org.dc.jul.storage.registry.RemoteRegistry;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.scene.SceneConfigType;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.control.scene.SceneRegistryType.SceneRegistry;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 */
public class SceneRegistryRemote extends RSBRemoteService<SceneRegistry> implements org.dc.bco.registry.scene.lib.SceneRegistry, Remote<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneConfigType.SceneConfig.getDefaultInstance()));
    }

    private final RemoteRegistry<String, SceneConfig, SceneConfig.Builder, SceneRegistry.Builder> sceneConfigRemoteRegistry;

    public SceneRegistryRemote() throws InstantiationException, InterruptedException {
        try {
            sceneConfigRemoteRegistry = new RemoteRegistry<>(new SceneConfigIdGenerator());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Method initializes the remote with the given scope for the server
     * registry connection.
     *
     * @param scope
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException
     */
    @Override
    public void init(final Scope scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeTransformer.transform(scope));
        } catch (CouldNotTransformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * Method initializes the remote with the given scope for the server
     * registry connection.
     *
     * @param scope
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException
     */
    @Override
    public synchronized void init(final ScopeType.Scope scope) throws InitializationException, InterruptedException {
        super.init(scope);
    }

    /**
     * Method initializes the remote with the default registry connection scope.
     *
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    public void init() throws InitializationException, InterruptedException {
        try {
            this.init(JPService.getProperty(JPSceneRegistryScope.class).getValue());
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        try {
            notifyUpdated(requestStatus());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial registry sync failed!", ex), logger, LogLevel.ERROR);
        }
    }

    @Override
    public void notifyUpdated(final SceneRegistry data) throws CouldNotPerformException {
        sceneConfigRemoteRegistry.notifyRegistryUpdated(data.getSceneConfigList());
    }

    public RemoteRegistry<String, SceneConfig, SceneConfig.Builder, SceneRegistry.Builder> getSceneConfigRemoteRegistry() {
        return sceneConfigRemoteRegistry;
    }

    @Override
    public SceneConfigType.SceneConfig registerSceneConfig(final SceneConfigType.SceneConfig sceneConfig) throws CouldNotPerformException {
        try {
            return (SceneConfigType.SceneConfig) callMethod("registerSceneConfig", sceneConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register scene config!", ex);
        }
    }

    @Override
    public SceneConfig getSceneConfigById(String sceneConfigId) throws CouldNotPerformException, NotAvailableException {
        getData();
        return sceneConfigRemoteRegistry.getMessage(sceneConfigId);
    }

    @Override
    public Boolean containsSceneConfig(final SceneConfigType.SceneConfig sceneConfig) throws CouldNotPerformException {
        getData();
        return sceneConfigRemoteRegistry.contains(sceneConfig);
    }

    @Override
    public Boolean containsSceneConfigById(final String sceneConfigId) throws CouldNotPerformException {
        getData();
        return sceneConfigRemoteRegistry.contains(sceneConfigId);
    }

    @Override
    public SceneConfigType.SceneConfig updateSceneConfig(final SceneConfigType.SceneConfig sceneConfig) throws CouldNotPerformException {
        try {
            return (SceneConfigType.SceneConfig) callMethod("updateSceneConfig", sceneConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update scene config!", ex);
        }
    }

    @Override
    public SceneConfigType.SceneConfig removeSceneConfig(final SceneConfigType.SceneConfig sceneConfig) throws CouldNotPerformException {
        try {
            return (SceneConfigType.SceneConfig) callMethod("removeSceneConfig", sceneConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove scene config!", ex);
        }
    }

    @Override
    public List<SceneConfig> getSceneConfigs() throws CouldNotPerformException, NotAvailableException {
        getData();
        List<SceneConfig> messages = sceneConfigRemoteRegistry.getMessages();
        return messages;
    }

    @Override
    public Future<Boolean> isSceneConfigRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return CompletableFuture.completedFuture(true);
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        try {
            return RPCHelper.callRemoteMethod(this, Boolean.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the scene config registry!!", ex);
        }
    }
}
