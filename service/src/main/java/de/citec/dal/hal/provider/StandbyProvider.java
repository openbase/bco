/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.provider;

import de.citec.dal.hal.provider.Provider;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InvocationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.homeautomation.state.StandbyStateType;

/**
 *
 * @author mpohling
 */
public interface StandbyProvider extends Provider {

    public StandbyStateType.StandbyState getStandby() throws CouldNotPerformException;
    
    public class GetStandbyCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetStandbyCallback.class);

        private final StandbyProvider provider;

        public GetStandbyCallback(final StandbyProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(StandbyStateType.StandbyState.class, provider.getStandby());
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(new InvocationFailedException(this, provider, ex), logger);
            }
        }
    }
}
