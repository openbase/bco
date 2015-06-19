/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.hal.provider.ColorProvider;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InvocationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.vision.HSVColorType;

/**
 *
 * @author mpohling
 */
public interface ColorService extends ColorProvider {

    public void setColor(HSVColorType.HSVColor color) throws CouldNotPerformException;

	public class SetColorCallback extends EventCallback {

		private static final Logger logger = LoggerFactory.getLogger(SetColorCallback.class);

		private final ColorService service;

		public SetColorCallback(final ColorService service) {
			this.service = service;
		}

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                service.setColor(((HSVColorType.HSVColor) request.getData()));
                return new Event(Void.class);
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(logger, new InvocationFailedException(this, service, ex));
            }
        }
    }
}
