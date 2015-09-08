/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.scm.remote;

import de.citec.scm.lib.generator.SceneConfigIdGenerator;
import de.citec.scm.lib.registry.SceneRegistryInterface;
import de.citec.jp.JPSceneRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPReadOnly;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.extension.rsb.com.RPCHelper;
import de.citec.jul.extension.rsb.com.RSBRemoteService;
import de.citec.jul.storage.registry.RemoteRegistry;
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
public class SceneRegistryRemote extends RSBRemoteService<SceneRegistry> implements SceneRegistryInterface {

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
        super.init(JPService.getProperty(JPSceneRegistryScope.class).getValue());
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        try {
            notifyUpdated(requestStatus());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(logger, new CouldNotPerformException("Initial registry sync failed!", ex));
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
        if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
            return CompletableFuture.completedFuture(true);
        }
        try {
            return RPCHelper.callRemoteMethod(Boolean.class, this);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the scene config registry!!", ex);
        }
    }
}
