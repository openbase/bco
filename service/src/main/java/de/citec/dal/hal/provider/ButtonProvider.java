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
import rst.homeautomation.states.ClickType;

/**
 *
 * @author thuxohl
 */
public interface ButtonProvider extends Provider {

    public ClickType.Click.ClickState getButtonState() throws CouldNotPerformException;

    public class GetButtonCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetButtonCallback.class);

        private final ButtonProvider provider;

        public GetButtonCallback(final ButtonProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(ClickType.Click.ClickState.class, provider.getButtonState());
            } catch (Exception ex) {
                logger.warn("Could not invoke method [getButtonState] for [" + provider + "].", ex);
                throw ex;
            }
        }
    }
}
