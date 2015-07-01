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
import rst.homeautomation.state.SmokeStateType.SmokeState;

/**
 *
 * @author thuxohl
 */
public interface SmokeStateProvider extends Provider {

    public SmokeState getSmokeState() throws CouldNotPerformException;

    public class GetSmokeStateProviderCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetSmokeStateProviderCallback.class);

        private final SmokeStateProvider provider;

        public GetSmokeStateProviderCallback(final SmokeStateProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(SmokeState.class, provider.getSmokeState());
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(logger, new InvocationFailedException(this, provider, ex));
            }
        }
    }
}
