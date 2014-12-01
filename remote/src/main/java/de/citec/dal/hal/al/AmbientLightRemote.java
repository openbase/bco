/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.data.Location;
import de.citec.dal.service.RSBRemoteService;
import rst.homeautomation.AmbientLightType;

/**
 *
 * @author mpohling
 */
public class AmbientLightRemote extends RSBRemoteService<AmbientLightType.AmbientLight> {

    public AmbientLightRemote(String id, Location location) {
        super(id, location);
    }

    @Override
    public void notifyUpdated(AmbientLightType.AmbientLight data) {
        
    }
    
}
