/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.data.Location;
import de.citec.dal.service.RSBRemoteService;

/**
 *
 * @author mpohling
 */
public class AmbientLightRemote extends RSBRemoteService<GeneratedMessage, GeneratedMessage.Builder>{

    public AmbientLightRemote(String id, Location location, GeneratedMessage.Builder builder) {
        super(id, location, builder);
    }
    
}
