/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service;

import org.dc.bco.dal.lib.layer.service.provider.DimProvider;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.InvocationFailedException;
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
                return new Event(Void.class);
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(new InvocationFailedException(this, service, ex), logger);
            }
        }
    }
}
