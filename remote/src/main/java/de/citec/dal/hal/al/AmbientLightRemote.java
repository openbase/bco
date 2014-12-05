/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.data.transform.HSVColorToRGBColorTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.service.rsb.RSBRemoteService;
import rst.homeautomation.AmbientLightType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author mpohling
 */
public class AmbientLightRemote extends RSBRemoteService<AmbientLightType.AmbientLight> {
    
    public AmbientLightRemote() {
        
    }
    
    public void setColor(final java.awt.Color color) {
        try {
            setColor(HSVColorToRGBColorTransformer.transform(color));
        } catch (RSBBindingException ex) {
            logger.warn("Could not set color!", ex);
        }
    }
    
    public void setColor(final HSVColor color) {
        callMethod("setColor", color, true);
    }
    
    @Override
    public void notifyUpdated(AmbientLightType.AmbientLight data) {
        
    }
}
