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
import rst.homeautomation.states.MotionType;

/**
 *
 * @author thuxohl
 */
public interface MotionProvider extends Provider {

    public MotionType.Motion.MotionState getMotion() throws CouldNotPerformException;

    public class GetMotionCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetMotionCallback.class);

        private final MotionProvider provider;

        public GetMotionCallback(final MotionProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(MotionType.Motion.MotionState.class, provider.getMotion());
            } catch (Exception ex) {
                logger.warn("Could not invoke method [getMotionState] for [" + provider + "].", ex);
                throw ex;
            }
        }
    }
}
