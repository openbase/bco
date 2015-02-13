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
import rst.homeautomation.states.ShutterType;

/**
 *
 * @author thuxohl
 */
public interface ShutterProvider extends Provider {

    public ShutterType.Shutter.ShutterState getShutter() throws CouldNotPerformException;

    public class GetShutterCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetShutterCallback.class);

        private final ShutterProvider provider;

        public GetShutterCallback(final ShutterProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(ShutterType.Shutter.ShutterState.class, provider.getShutter());
            } catch (Exception ex) {
                logger.warn("Could not invoke method [getShutterState] for [" + provider + "].", ex);
                throw ex;
            }
        }
    }
}
