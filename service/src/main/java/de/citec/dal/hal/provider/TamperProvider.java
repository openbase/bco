/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.provider;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InvocationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.homeautomation.state.TamperStateType.TamperState;

/**
 *
 * @author thuxohl
 */
public interface TamperProvider extends Provider {

    public TamperState getTamper() throws CouldNotPerformException;

    public class GetTamperCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetTamperCallback.class);

        private final TamperProvider provider;

        public GetTamperCallback(final TamperProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(TamperState.class, provider.getTamper());
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(logger, new InvocationFailedException(this, provider, ex));
            }
        }
    }
}
