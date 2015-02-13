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
import rst.homeautomation.states.OpenClosedTiltedType;

/**
 *
 * @author thuxohl
 */
public interface HandleProvider extends Provider {

    public OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState getHandle() throws CouldNotPerformException;

    public class GetHandleCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetHandleCallback.class);

        private final HandleProvider provider;

        public GetHandleCallback(final HandleProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState.class, provider.getHandle());
            } catch (Exception ex) {
                logger.warn("Could not invoke method [getRotaryHandleState] for [" + provider + "].", ex);
                throw ex;
            }
        }
    }
}
