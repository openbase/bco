/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.operation;

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
import java.util.concurrent.Future;
import org.dc.bco.dal.lib.layer.service.provider.BrightnessProviderService;
import org.dc.jul.exception.CouldNotPerformException;

/**
 *
 * @author mpohling
 */
public interface BrightnessOperationService extends OperationService, BrightnessProviderService {

    public Future<Void> setBrightness(Double brightness) throws CouldNotPerformException;

//    public class SetBrightnessCallback extends EventCallback {
//
//        private static final Logger logger = LoggerFactory.getLogger(SetBrightnessCallback.class);
//
//        private final BrightnessOperationService service;
//
//        public SetBrightnessCallback(final BrightnessOperationService service) {
//            this.service = service;
//        }
//
//        @Override
//        public Event invoke(final Event request) throws UserCodeException {
//            try {
//                service.setBrightness(((double) request.getData())).get();
//            } catch (InterruptedException ex) {
//                Thread.currentThread().interrupt();
//            } catch (CouldNotPerformException | ExecutionException ex) {
//                throw ExceptionPrinter.printHistoryAndReturnThrowable(new UserCodeException(new InvocationFailedException(this, service, ex)), logger);
//            }
//            return new Event(Void.class);
//        }
//    }
}
