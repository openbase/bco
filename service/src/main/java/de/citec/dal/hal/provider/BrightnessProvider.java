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

/**
 *
 * @author thuxohl
 */
public interface BrightnessProvider extends Provider {

    public Double getBrightness() throws CouldNotPerformException;
    
    public class GetBrightnessCallback extends EventCallback {

		private static final Logger logger = LoggerFactory.getLogger(GetBrightnessCallback.class);

		private final BrightnessProvider provider;

		public GetBrightnessCallback(final BrightnessProvider provider) {
			this.provider = provider;
		}

		@Override
		public Event invoke(final Event request) throws Throwable {
			try {
				return new Event(Double.class, provider.getBrightness());
			} catch (Exception ex) {
				throw ExceptionPrinter.printHistoryAndReturnThrowable(logger, new InvocationFailedException(this, provider, ex));
			}
		}
	}
}
