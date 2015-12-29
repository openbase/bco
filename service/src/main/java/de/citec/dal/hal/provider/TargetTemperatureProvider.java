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

/**
 *
 * @author mpohling
 */
public interface TargetTemperatureProvider extends Provider {

    public Double getTargetTemperature() throws CouldNotPerformException;

    public class GetTargetTemperatureCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetTargetTemperatureCallback.class);

        private final TargetTemperatureProvider provider;

        public GetTargetTemperatureCallback(final TargetTemperatureProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(Double.class, provider.getTargetTemperature());
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(new InvocationFailedException(this, provider, ex), logger);
            }
        }
    }
}
