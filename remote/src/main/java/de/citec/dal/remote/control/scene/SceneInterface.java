/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.scene;

import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.iface.Activatable;
import rst.homeautomation.control.scene.SceneConfigType;

/**
 *
 * @author mpohling
 */
public interface SceneInterface extends Activatable {
    public SceneConfigType.SceneConfig getConfig() throws NotAvailableException;
}
