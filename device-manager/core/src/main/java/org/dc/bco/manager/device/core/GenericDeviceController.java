/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.core;

import org.dc.bco.dal.lib.layer.service.ServiceFactory;
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
public class GenericDeviceController extends AbstractDeviceController<GenericDevice, GenericDevice.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(GenericDevice.getDefaultInstance()));
    }

    private final ServiceFactory serviceFactory;

    public GenericDeviceController(final ServiceFactory serviceFactory) throws InstantiationException, CouldNotPerformException {
        super(GenericDevice.newBuilder());

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
}
