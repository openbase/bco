/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.scene.lib;

import org.dc.jul.pattern.Factory;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface SceneFactory extends Factory<SceneController, SceneConfig> {

    @Override
    public SceneController newInstance(final SceneConfig config) throws org.dc.jul.exception.InstantiationException;
}
