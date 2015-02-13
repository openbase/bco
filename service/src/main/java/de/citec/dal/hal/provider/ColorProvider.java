/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.provider;

import de.citec.jul.exception.CouldNotPerformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.vision.HSVColorType;

/**
 *
 * @author thuxohl
 */
public interface ColorProvider extends Provider {
    
    public HSVColorType.HSVColor getColor() throws CouldNotPerformException;
    
    public class GetColorCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetColorCallback.class);

        private final ColorProvider provider;

        public GetColorCallback(final ColorProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(HSVColorType.HSVColor.class, provider.getColor());
            } catch (Exception ex) {
                logger.warn("Could not invoke method [getColor] for [" + provider + "].", ex);
                throw ex;
            }
        }
    }
}
