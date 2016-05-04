package org.dc.bco.dal.lib.layer.service.provider;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.dc.jul.exception.NotAvailableException;

/**
 *
 * @author thuxohl
 */
public interface BrightnessProviderService extends ProviderService {

    public Double getBrightness() throws NotAvailableException;

//    public class GetBrightnessCallback extends EventCallback {
//
//		private static final Logger logger = LoggerFactory.getLogger(GetBrightnessCallback.class);
//
//		private final BrightnessProviderService provider;
//
//		public GetBrightnessCallback(final BrightnessProviderService provider) {
//			this.provider = provider;
//		}
//
//		@Override
//		public Event invoke(final Event request) throws UserCodeException {
//			try {
//				return new Event(Double.class, provider.getBrightness());
//			} catch (Exception ex) {
//				throw ExceptionPrinter.printHistoryAndReturnThrowable(new UserCodeException(new InvocationFailedException(this, provider, ex)), logger);
//			}
//		}
//	}
}
