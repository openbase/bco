/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.scene.lib;

import org.dc.jul.exception.InitializationException;
import org.dc.jul.iface.Activatable;
import org.dc.jul.iface.Configurable;
import org.dc.jul.iface.Identifiable;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface SceneController extends Identifiable<String>, Configurable<String, SceneConfig>, Activatable, Scene {

    public void init(final SceneConfig config) throws InitializationException, InterruptedException;
}
