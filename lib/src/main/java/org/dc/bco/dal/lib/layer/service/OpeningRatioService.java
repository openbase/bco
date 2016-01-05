package org.dc.bco.dal.lib.layer.service;

import org.dc.bco.dal.lib.layer.service.provider.OpeningRatioProvider;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.InvocationFailedException;
import org.dc.jul.extension.rsb.com.RSBCommunicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;

/**
 *
 * @author thuxohl
 */
public interface OpeningRatioService extends Service, OpeningRatioProvider {

    public void setOpeningRatio(Double openingRatio) throws CouldNotPerformException;

    public class SetOpeningRatioCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(SetOpeningRatioCallback.class);

        private final OpeningRatioService service;

        public SetOpeningRatioCallback(final OpeningRatioService service) {
            this.service = service;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                service.setOpeningRatio(((double) request.getData()));
                return new Event(Void.class);
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(new InvocationFailedException(this, service, ex), logger);
            }
        }
    }
}
