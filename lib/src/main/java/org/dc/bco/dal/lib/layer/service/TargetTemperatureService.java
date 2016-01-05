/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service;

import org.dc.bco.dal.lib.layer.service.provider.TargetTemperatureProvider;
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
public interface TargetTemperatureService extends Service, TargetTemperatureProvider {

    public void setTargetTemperature(final Double value) throws CouldNotPerformException;

    public class SetTargetTemperatureCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(SetTargetTemperatureCallback.class);

        private final TargetTemperatureService service;

        public SetTargetTemperatureCallback(final TargetTemperatureService service) {
            this.service = service;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                service.setTargetTemperature(((Double) request.getData()));
                return new Event(Void.class);
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(new InvocationFailedException(this, service, ex), logger);
            }
        }
    }
}
