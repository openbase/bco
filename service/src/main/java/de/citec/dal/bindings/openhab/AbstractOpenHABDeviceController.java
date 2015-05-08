/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.bindings.openhab.service.OpenhabServiceFactory;
import de.citec.dal.hal.device.AbstractDeviceController;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.InstantiationException;
import rst.homeautomation.device.DeviceConfigType;

/**
 *
 * @author Divine Threepwood
 * @param <M>
 * @param <MB>
 */
public abstract class AbstractOpenHABDeviceController<M extends GeneratedMessage, MB extends M.Builder<MB>> extends AbstractDeviceController<M, MB> {

    public static final String ITEM_ID_DELIMITER = "_";
    private final static ServiceFactory defaultServiceFactory = new OpenhabServiceFactory();

    public AbstractOpenHABDeviceController(final DeviceConfigType.DeviceConfig config, MB builder) throws InstantiationException, CouldNotTransformException {
        super(config, builder);
    }

    @Override
    public ServiceFactory getDefaultServiceFactory() {
        return defaultServiceFactory;
    }
}
