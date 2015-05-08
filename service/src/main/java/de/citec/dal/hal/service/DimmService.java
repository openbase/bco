/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.hal.provider.DimmProvider;
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
public interface DimmService extends Service, DimmProvider {

    //TODO tamino: rename to DimService. Dimm is not a valid english word ;)

    public void setDimm(Double dimm) throws CouldNotPerformException;

    public class SetDimmCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(SetDimmCallback.class);

        private final DimmService service;

        public SetDimmCallback(final DimmService service) {
            this.service = service;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                service.setDimm(((double) request.getData()));
                return RSBCommunicationService.RPC_SUCCESS;
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistory(logger, new InvocationFailedException(this, service, ex));
            }
        }
    }
}
