/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.dem.core;

import de.citec.dal.DALService;
import org.dc.bco.coma.dem.binding.openhab.AbstractDeviceController;
import org.dc.bco.coma.dem.binding.openhab.service.OpenhabServiceFactory;
import de.citec.dal.hal.service.ServiceFactory;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.NotSupportedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
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

    public GenericDeviceController(DeviceConfig config) throws InstantiationException, CouldNotTransformException {
        super(config, GenericDevice.newBuilder());
    }

    @Override
    public ServiceFactory getServiceFactory() throws NotAvailableException {
        try {
            DeviceClassType.DeviceClass deviceClass = DALService.getRegistryProvider().getDeviceRegistryRemote().getDeviceClassById(getConfig().getDeviceClassId());
            switch (deviceClass.getBindingConfig().getType()) {
            case OPENHAB:
                return OpenhabServiceFactory.getInstance();
            default:
                throw new NotSupportedException("Binding[" + deviceClass.getBindingConfig().getType() + "]", this);
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException(ServiceFactory.class, new CouldNotPerformException("Could not detect service factory!", ex));
        }
    }
}
