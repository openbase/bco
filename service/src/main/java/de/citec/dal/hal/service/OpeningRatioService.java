package de.citec.dal.hal.service;

import de.citec.dal.hal.provider.OpeningRatioProvider;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InvocationFailedException;
import de.citec.jul.extension.rsb.com.RSBCommunicationService;
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
                return RSBCommunicationService.RPC_SUCCESS;
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistory(logger, new InvocationFailedException(this, service, ex));
            }
        }
    }
}
