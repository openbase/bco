/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.data.transform.HSVColorToRGBColorTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.jul.rsb.RSBRemoteService;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.AmbientLightType;
import rst.homeautomation.states.PowerType;
import rst.vision.HSVColorType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author mpohling
 */
public class AmbientLightRemote extends RSBRemoteService<AmbientLightType.AmbientLight> {
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSVColorType.HSVColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AmbientLightType.AmbientLight.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }
    
    public AmbientLightRemote() {
        
    }
    
    public void setColor(final java.awt.Color color) throws CouldNotPerformException {
        try {
            setColor(HSVColorToRGBColorTransformer.transform(color));
        } catch (RSBBindingException ex) {
            logger.warn("Could not set color!", ex);
        }
    }
    
    public void setColor(final HSVColor color) throws CouldNotPerformException {
        callMethodAsync("setColor", color);
    }
    
    public void setPowerState(final PowerType.Power.PowerState state) throws CouldNotPerformException {
        callMethodAsync("setPowerState", state);
    }
    
    public void setBrightness(final double brightness) throws CouldNotPerformException {
        callMethodAsync("setBrightness", brightness);
    }

    @Override
    public void notifyUpdated(AmbientLightType.AmbientLight data) {
    }
}
