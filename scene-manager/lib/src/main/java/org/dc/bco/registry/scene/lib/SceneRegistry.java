/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.scene.lib;

import org.dc.jul.exception.CouldNotPerformException;
import java.util.List;
import java.util.concurrent.Future;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;

/**
 *
 * @author mpohling
 */
public interface SceneRegistry {

    public SceneConfig registerSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException;

    public Boolean containsSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException;

    public Boolean containsSceneConfigById(String sceneConfigId) throws CouldNotPerformException;

    public SceneConfig updateSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException;

    public SceneConfig removeSceneConfig(SceneConfig sceneConfig) throws CouldNotPerformException;

    public SceneConfig getSceneConfigById(final String sceneConfigId) throws CouldNotPerformException;

    public List<SceneConfig> getSceneConfigs() throws CouldNotPerformException;

    public Future<Boolean> isSceneConfigRegistryReadOnly() throws CouldNotPerformException;

    public void shutdown();
}
