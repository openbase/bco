/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.AbstractUnitCollectionController;
import de.citec.dal.hal.device.Device;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author Divine Threepwood
 * @param <M>
 * @param <MB>
 */
    public abstract class AbstractDeviceController<M extends GeneratedMessage, MB extends M.Builder<MB>> extends AbstractUnitCollectionController<M, MB> implements Device {

    public final static String DEVICE_TYPE_FILED_CONFIG = "config";

    private final DeviceConfig config;
    protected final Location location;

    public AbstractDeviceController(final DeviceConfig config, final MB builder) throws InstantiationException, CouldNotTransformException {
        super(builder);
        try {
            this.config = config;
            this.location = new Location(DALService.getRegistryProvider().getLocationRegistryRemote().getLocationConfigById(config.getPlacementConfig().getLocationId()));

            setField(DEVICE_TYPE_FILED_CONFIG, config);

            try {
                init(config.getScope());
            } catch (InitializationException ex) {
                throw new InstantiationException("Could not init RSBCommunicationService!", ex);
            }

            try {
                registerUnits(config.getUnitConfigList());
            } catch (CouldNotPerformException ex) {
                throw new InstantiationException(this, ex);
            }
            
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException("Could not init RSBCommunicationService!", ex);
        }
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public String getId() {
        return config.getId();
    }

    @Override
    public String getLabel() {
        return config.getLabel();
    }

    @Override
    public DeviceConfig getConfig() {
        return config;
    }
}
