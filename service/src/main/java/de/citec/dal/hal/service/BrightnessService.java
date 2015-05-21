/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.hal.provider.BrightnessProvider;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InvocationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;

/**
 *
 * @author mpohling
 */
public interface BrightnessService extends Service, BrightnessProvider {

    public void setBrightness(Double brightness) throws CouldNotPerformException;

    public class SetBrightnessCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(SetBrightnessCallback.class);

        private final BrightnessService service;

        public SetBrightnessCallback(final BrightnessService service) {
            this.service = service;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                service.setBrightness(((double) request.getData()));
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistory(logger, new InvocationFailedException(this, service, ex));
            }
            return new Event(Void.class);
        }
    }
}
