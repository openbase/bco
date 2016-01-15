package org.dc.bco.manager.device.core;

import org.dc.bco.manager.device.lib.DeviceManager;
import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.dc.bco.dal.lib.layer.unit.UnitControllerRegistryImpl;
import org.dc.bco.manager.device.lib.DeviceControllerRegistry;
import org.dc.bco.manager.device.lib.DeviceFactory;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.NotAvailableException;
import rst.homeautomation.device.DeviceConfigType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class DeviceManagerController implements DeviceManager {

    private static DeviceManagerController instance;

    private final DeviceFactory deviceFactory;
    private final ServiceFactory serviceFactory;

    private final LocationRegistryRemote locationRegistry;
    private final DeviceRegistryRemote deviceRegistry;

    private final DeviceRegistrySynchronizer deviceRegistrySynchronizer;

    private final DeviceControllerRegistryImpl deviceControllerRegistry;
    private final UnitControllerRegistryImpl unitControllerRegistry;

    public DeviceManagerController(final ServiceFactory serviceFactory) throws org.dc.jul.exception.InstantiationException {
        this(serviceFactory, new DeviceFactoryImpl(serviceFactory));
    }

    public DeviceManagerController(final ServiceFactory serviceFactory, final DeviceFactory deviceFactory) throws org.dc.jul.exception.InstantiationException {
        try {
            this.instance = this;
            this.deviceFactory = deviceFactory;
            this.serviceFactory = serviceFactory;
            this.deviceControllerRegistry = new DeviceControllerRegistryImpl();
            this.unitControllerRegistry = new UnitControllerRegistryImpl();
            this.locationRegistry = new LocationRegistryRemote();
            this.deviceRegistry = new DeviceRegistryRemote();
            this.deviceRegistrySynchronizer = new DeviceRegistrySynchronizer(this, deviceFactory);
        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    public static DeviceManager getDeviceManager() throws NotAvailableException {
        if (instance == null) {
            throw new NotAvailableException(DeviceManager.class);
        }
        return instance;
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            locationRegistry.init();
            locationRegistry.activate();
            deviceRegistry.init();
            deviceRegistry.activate();
            deviceRegistrySynchronizer.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        deviceControllerRegistry.shutdown();
        unitControllerRegistry.shutdown();
        locationRegistry.shutdown();
        deviceRegistry.shutdown();
        deviceRegistrySynchronizer.shutdown();
        instance = null;
    }

    @Override
    public DeviceRegistryRemote getDeviceRegistry() {
        return deviceRegistry;
    }

    @Override
    public LocationRegistryRemote getLocationRegistry() {
        return locationRegistry;
    }

    @Override
    public DeviceControllerRegistry getDeviceControllerRegistry() {
        return deviceControllerRegistry;
    }

    public DeviceControllerRegistryImpl getDeviceControllerRegistryImpl() {
        return deviceControllerRegistry;
    }

    @Override
    public UnitControllerRegistry getUnitControllerRegistry() {
        return unitControllerRegistry;
    }

    /**
     * All devices will be supported by default. Feel free to overwrite method to changing this behavior.
     *
     * @param config
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public boolean isSupported(DeviceConfigType.DeviceConfig config) throws CouldNotPerformException {
        return true;
    }

    @Override
    public ServiceFactory getServiceFactory() throws NotAvailableException {
        return serviceFactory;
    }

    @Override
    public DeviceFactory getDeviceFactory() throws NotAvailableException {
        return deviceFactory;
    }
}
