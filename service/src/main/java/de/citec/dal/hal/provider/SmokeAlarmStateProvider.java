/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.provider;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InvocationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.homeautomation.state.AlarmStateType.AlarmState;

/**
 *
 * @author thuxohl
 */
public interface SmokeAlarmStateProvider extends Provider {
    
    public AlarmState getSmokeAlarmState() throws CouldNotPerformException;

    public class GetSmokeAlarmStateProviderCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetSmokeAlarmStateProviderCallback.class);

        private final SmokeAlarmStateProvider provider;

        public GetSmokeAlarmStateProviderCallback(final SmokeAlarmStateProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(AlarmState.class, provider.getSmokeAlarmState());
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(logger, new InvocationFailedException(this, provider, ex));
            }
        }
    }
}
