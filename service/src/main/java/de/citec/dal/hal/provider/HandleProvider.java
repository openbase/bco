/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.provider;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.InvocationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.homeautomation.state.HandleStateType.HandleState;

/**
 *
 * @author thuxohl
 */
public interface HandleProvider extends Provider {

    public HandleState getHandle() throws CouldNotPerformException;

    public class GetHandleCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetHandleCallback.class);

        private final HandleProvider provider;

        public GetHandleCallback(final HandleProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(HandleState.class, provider.getHandle());
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(new InvocationFailedException(this, provider, ex), logger);
            }
        }
    }
}
