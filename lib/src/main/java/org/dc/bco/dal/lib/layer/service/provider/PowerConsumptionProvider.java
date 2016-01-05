/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.provider;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.InvocationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.homeautomation.state.PowerConsumptionStateType.PowerConsumptionState;

/**
 *
 * @author thuxohl
 */
public interface PowerConsumptionProvider extends Provider {

    public PowerConsumptionState getPowerConsumption() throws CouldNotPerformException;

    public class GetPowerConsumptionCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetPowerConsumptionCallback.class);

        private final PowerConsumptionProvider provider;

        public GetPowerConsumptionCallback(final PowerConsumptionProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(PowerConsumptionState.class, provider.getPowerConsumption());
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(new InvocationFailedException(this, provider, ex), logger);
            }
        }
    }
}
