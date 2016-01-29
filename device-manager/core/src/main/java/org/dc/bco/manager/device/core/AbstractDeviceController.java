/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.core;

import com.google.protobuf.GeneratedMessage;
import org.dc.bco.dal.lib.data.Location;
import org.dc.bco.dal.lib.layer.unit.AbstractUnitCollectionController;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.bco.manager.device.lib.Device;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.extension.rsb.com.RSBCommunicationService;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author Divine Threepwood
 * @param <M>
 * @param <MB>
 */
public abstract class AbstractDeviceController<M extends GeneratedMessage, MB extends M.Builder<MB>> extends AbstractUnitCollectionController<M, MB> implements Device {

    public final static String DEVICE_TYPE_FILED_CONFIG = "config";

    private DeviceConfig config;
    protected final Location location;

    public AbstractDeviceController(final DeviceConfig config, final MB builder) throws InstantiationException, CouldNotTransformException {
        super(builder);
        try {
            this.config = config;
            this.location = new Location(DeviceManagerController.getDeviceManager().getLocationRegistry().getLocationConfigById(config.getPlacementConfig().getLocationId()));

            setField(DEVICE_TYPE_FILED_CONFIG, config);

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(RSBCommunicationService.class, ex);
        }
    }

    public void init() throws InitializationException {
        try {
            init(config.getScope());

            try {
                registerUnits(config.getUnitConfigList());

                for (Unit unit : getUnits()) {
                    DeviceManagerController.getDeviceManager().getUnitControllerRegistry().register(unit);
                }
            } catch (CouldNotPerformException ex) {
                throw new InstantiationException(this, ex);
            }

        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public DeviceConfig updateConfig(final DeviceConfig config) throws CouldNotPerformException {
        this.config = config;
        return config;
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
