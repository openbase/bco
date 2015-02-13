/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.provider;

import de.citec.jul.exception.CouldNotPerformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author thuxohl
 */
public interface PowerProvider extends Provider {

    public PowerType.Power.PowerState getPowerState() throws CouldNotPerformException;
    
    public class GetPowerCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetPowerCallback.class);

        private final PowerProvider provider;

        public GetPowerCallback(final PowerProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(PowerType.Power.PowerState.class, provider.getPowerState());
            } catch (Exception ex) {
                logger.warn("Could not invoke method [getPowerState] for [" + provider + "].", ex);
                throw ex;
            }
        }
    }
}
