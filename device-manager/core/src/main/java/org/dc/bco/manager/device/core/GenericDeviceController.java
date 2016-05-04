package org.dc.bco.manager.device.core;

/*
 * #%L
 * COMA DeviceManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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

import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.bco.registry.device.lib.DeviceRegistry;
import org.dc.bco.registry.location.lib.LocationRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.GenericDeviceType.GenericDevice;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 * @author <a href="mailto:mpohling@techfak.uni-bielefeld.com">Marian Pohling</a>
 */
public class GenericDeviceController extends AbstractDeviceController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(GenericDevice.getDefaultInstance()));
    }

    private final ServiceFactory serviceFactory;

    public GenericDeviceController(final ServiceFactory serviceFactory) throws InstantiationException, CouldNotPerformException {
        try {
            if (serviceFactory == null) {
                throw new NotAvailableException("service factory");
            }
            this.serviceFactory = serviceFactory;
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public ServiceFactory getServiceFactory() throws NotAvailableException {
        if (serviceFactory == null) {
            throw new NotAvailableException(ServiceFactory.class);
        }
        return serviceFactory;
    }

    @Override
    public LocationRegistry getLocationRegistry() throws NotAvailableException {
        return DeviceManagerController.getDeviceManager().getLocationRegistry();
    }

    @Override
    public DeviceRegistry getDeviceRegistry() throws NotAvailableException {
        return DeviceManagerController.getDeviceManager().getDeviceRegistry();
    }
}
