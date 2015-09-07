/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.hal.provider.StandbyProvider;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InvocationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.homeautomation.state.StandbyStateType.StandbyState;

/**
 *
 * @author mpohling
 */
public interface StandbyService extends Service, StandbyProvider {

    public void setStandby(final StandbyState.State state) throws CouldNotPerformException;

    public class SetStandbyCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(SetStandbyCallback.class);

        private final StandbyService service;

        public SetStandbyCallback(final StandbyService service) {
            this.service = service;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                service.setStandby(((StandbyState) request.getData()).getValue());
                return new Event(Void.class);
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(logger, new InvocationFailedException(this, service, ex));
            }
        }
    }

}
