/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.hal.provider.DimProvider;
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
public interface DimService extends Service, DimProvider {

    public void setDim(Double dim) throws CouldNotPerformException;

    public class SetDimCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(SetDimCallback.class);

        private final DimService service;

        public SetDimCallback(final DimService service) {
            this.service = service;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                service.setDim(((double) request.getData()));
                return RSBCommunicationService.RPC_SUCCESS;
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistory(logger, new InvocationFailedException(this, service, ex));
            }
        }
    }
}
