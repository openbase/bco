/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service;

import org.dc.bco.dal.lib.layer.service.provider.PowerProvider;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.InvocationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author mpohling
 */
public interface PowerService extends Service, PowerProvider {

    public void setPower(final PowerState.State state) throws CouldNotPerformException;

    public class SetPowerCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(SetPowerCallback.class);

        private final PowerService service;

        public SetPowerCallback(final PowerService service) {
            this.service = service;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                service.setPower(((PowerState) request.getData()).getValue());
                return new Event(Void.class);
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(new InvocationFailedException(this, service, ex), logger);
            }
        }
    }

}
