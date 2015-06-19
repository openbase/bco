/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.provider;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InvocationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.homeautomation.state.MotionStateType.MotionState;

/**
 *
 * @author thuxohl
 */
public interface MotionProvider extends Provider {

    public MotionState getMotion() throws CouldNotPerformException;

    public class GetMotionCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetMotionCallback.class);

        private final MotionProvider provider;

        public GetMotionCallback(final MotionProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(MotionState.class, provider.getMotion());
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(logger, new InvocationFailedException(this, provider, ex));
            }
        }
    }
}
