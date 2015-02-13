/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.hal.provider.ShutterProvider;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.RSBCommunicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.homeautomation.states.ShutterType;

/**
 *
 * @author thuxohl
 */
public interface ShutterService extends Service, ShutterProvider {
    
    public void setShutterState(ShutterType.Shutter.ShutterState state) throws CouldNotPerformException;
    
    public class SetShutterCallback extends EventCallback {

		private static final Logger logger = LoggerFactory.getLogger(SetShutterCallback.class);

		private final ShutterService service;

		public SetShutterCallback(final ShutterService service) {
			this.service = service;
		}

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                service.setShutterState(((ShutterType.Shutter.ShutterState) request.getData()));
                return RSBCommunicationService.RPC_FEEDBACK_OK;
            } catch (Exception ex) {
                logger.warn("Could not invoke method [setShutterState] for [" + service + "].", ex);
                throw ex;
            }
        }
    }
}
