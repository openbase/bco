/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.scm.core;

import org.dc.bco.coma.scm.lib.Scene;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;

/**
 *
 * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class SceneController implements Scene {
    private SceneConfig config;

    public SceneController(SceneConfig config) {
        this.config = config;
        System.out.println("create agent:" + config.getLabel());
    }

    @Override
    public SceneConfig getConfig() throws NotAvailableException {
        return config;
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        System.out.println("activate agent:" + config.getLabel());
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        System.out.println("deactivate agent:" + config.getLabel());
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
