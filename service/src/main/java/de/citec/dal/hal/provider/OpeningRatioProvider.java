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

/**
 *
 * @author thuxohl
 */
public interface OpeningRatioProvider extends Provider {

    public Double getOpeningRatio() throws CouldNotPerformException;

    public class GetOpeningRatioCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetOpeningRatioCallback.class);

        private final OpeningRatioProvider provider;
        public GetOpeningRatioCallback(final OpeningRatioProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(Double.class, provider.getOpeningRatio());
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(new InvocationFailedException(this, provider, ex), logger);
            }
        }
    }
}
