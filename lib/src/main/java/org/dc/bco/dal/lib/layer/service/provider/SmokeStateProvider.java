/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.provider;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.InvocationFailedException;
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

    public class GetSmokeStateCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetSmokeStateCallback.class);

        private final SmokeStateProvider provider;

        public GetSmokeStateCallback(final SmokeStateProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(SmokeState.class, provider.getSmokeState());
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(new InvocationFailedException(this, provider, ex), logger);
            }
        }
    }
}
