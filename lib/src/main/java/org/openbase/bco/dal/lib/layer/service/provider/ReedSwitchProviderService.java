package org.openbase.bco.dal.lib.layer.service.provider;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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

import org.openbase.jul.exception.NotAvailableException;
import rst.homeautomation.state.ReedSwitchStateType.ReedSwitchState;

/**
 *
 * @author thuxohl
 */
public interface ReedSwitchProviderService extends ProviderService {

    public ReedSwitchState getReedSwitch() throws NotAvailableException;

//    public class GetReedSwitchCallback extends EventCallback {
//
//        private static final Logger logger = LoggerFactory.getLogger(GetReedSwitchCallback.class);
//
//        private final ReedSwitchProviderService provider;
//
//        public GetReedSwitchCallback(final ReedSwitchProviderService provider) {
//            this.provider = provider;
//        }
//
//        @Override
//        public Event invoke(final Event request) throws UserCodeException {
//            try {
//                return new Event(ReedSwitchState.class, provider.getReedSwitch());
//            } catch (Exception ex) {
//                throw ExceptionPrinter.printHistoryAndReturnThrowable(new UserCodeException(new InvocationFailedException(this, provider, ex)), logger);
//            }
//        }
//    }
}
