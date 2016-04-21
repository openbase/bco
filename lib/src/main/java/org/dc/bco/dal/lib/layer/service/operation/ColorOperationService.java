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
import org.dc.bco.dal.lib.layer.service.provider.ColorProviderService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.vision.HSVColorType;

/**
 *
 * @author mpohling
 */
public interface ColorOperationService extends OperationService, ColorProviderService {

    public Future<Void> setColor(final HSVColorType.HSVColor color) throws CouldNotPerformException;

//    public class SetColorCallback extends EventCallback {
//
//        private static final Logger logger = LoggerFactory.getLogger(SetColorCallback.class);
//
//        private final ColorService service;
//
//        public SetColorCallback(final ColorService service) {
//            this.service = service;
//        }
//
//        @Override
//        public Event invoke(final Event request) throws UserCodeException {
//            try {
//                service.setColor(((HSVColorType.HSVColor) request.getData())).get();
//                return new Event(Void.class);
//            } catch (InterruptedException ex) {
//                Thread.currentThread().interrupt();
//            } catch (Exception ex) {
//                throw ExceptionPrinter.printHistoryAndReturnThrowable(new UserCodeException(new InvocationFailedException(this, service, ex)), logger);
//            }
//        }
//    }
}
