/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.bindings.openhab.service.OpenhabServiceFactory;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.AbstractDeviceController;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.jul.exception.InstantiationException;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 * @param <M>
 * @param <MB>
 */
public abstract class AbstractOpenHABDeviceController<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> extends AbstractDeviceController<M, MB> {

    public static final String ITEM_ID_DELIMITER = "_";
    private final static ServiceFactory defaultServiceFactory = new OpenhabServiceFactory();

	@Deprecated
    public AbstractOpenHABDeviceController(String name, String label, Location location, MB builder) throws InstantiationException {
		this(label, location, builder);
	}

    public AbstractOpenHABDeviceController(String label, Location location, MB builder) throws InstantiationException {
        super(label, location, builder);
    }

    @Override
    public ServiceFactory getDefaultServiceFactory() {
        return defaultServiceFactory;
    }
}
