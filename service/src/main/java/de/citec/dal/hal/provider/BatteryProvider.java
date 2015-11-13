/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.provider;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InvocationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.homeautomation.state.BatteryStateType.BatteryState;

/**
 *
 * @author thuxohl
 */
public interface BatteryProvider extends Provider {

    public BatteryState getBattery() throws CouldNotPerformException;

    public class GetBatteryCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetBatteryCallback.class);

        private final BatteryProvider provider;

        public GetBatteryCallback(final BatteryProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(BatteryState.class, provider.getBattery());
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(new InvocationFailedException(this, provider, ex), logger);
            }
        }
    }
}
