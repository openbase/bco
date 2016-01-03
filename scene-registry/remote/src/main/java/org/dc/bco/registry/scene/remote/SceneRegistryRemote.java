/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.scene.remote;

import org.dc.bco.registry.scene.lib.jp.JPSceneRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.preset.JPReadOnly;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBRemoteService;
import org.dc.jul.storage.registry.RemoteRegistry;
import org.dc.bco.registry.scene.lib.generator.SceneConfigIdGenerator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.scene.SceneConfigType;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.control.scene.SceneRegistryType.SceneRegistry;

/**
 *
 * @author mpohling
 */
public class SceneRegistryRemote extends RSBRemoteService<SceneRegistry> implements org.dc.bco.registry.scene.lib.SceneRegistry {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneConfigType.SceneConfig.getDefaultInstance()));
    }

    private final RemoteRegistry<String, SceneConfig, SceneConfig.Builder, SceneRegistry.Builder> sceneConfigRemoteRegistry;

    public SceneRegistryRemote() throws InstantiationException {
        try {
            sceneConfigRemoteRegistry = new RemoteRegistry<>(new SceneConfigIdGenerator());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        try {
            super.init(JPService.getProperty(JPSceneRegistryScope.class).getValue());
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
