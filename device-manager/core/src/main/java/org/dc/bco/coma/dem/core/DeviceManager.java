package org.dc.bco.coma.dem.core;

import org.dc.bco.dal.lib.registry.UnitRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class DeviceManager {

    private static RegistryProvider registryProvider;

    private final DALBindingRegistry bindingRegistry;

    private final DeviceRegistry deviceRegistry;
    private final DeviceRegistrySynchronizer deviceRegistrySynchronizer;
    private final UnitRegistry unitRegistry;
    private final LocationRegistryRemote locationRegistryRemote;
    private final DeviceRegistryRemote deviceRegistryRemote;

    public DeviceManager() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        try {
            this.bindingRegistry = new DALBindingRegistry();
            this.deviceRegistry = new DeviceRegistry();
            this.unitRegistry = new UnitRegistry();
            this.locationRegistryRemote = new LocationRegistryRemote();
            this.deviceRegistryRemote = new DeviceRegistryRemote();
            this.deviceRegistrySynchronizer = new DeviceRegistrySynchronizer(deviceRegistry, deviceRegistryRemote);

            registryProvider = this;

        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    public static RegistryProvider getRegistryProvider() throws NotAvailableException {
        if (registryProvider == null) {
            throw new NotAvailableException(RegistryProvider.class);
        }
        return registryProvider;
    }

    public void init() throws CouldNotPerformException, InterruptedException {
        try {
            locationRegistryRemote.init();
            locationRegistryRemote.activate();
            deviceRegistryRemote.init();
            deviceRegistryRemote.activate();
            initBindings();
            deviceRegistrySynchronizer.init();
        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    private void initBindings() throws CouldNotPerformException {
        bindingRegistry.register(OpenHABBinding.class);
    }

    public void shutdown() {
        bindingRegistry.shutdown();
        deviceRegistry.shutdown();
        unitRegistry.shutdown();
        locationRegistryRemote.shutdown();
        deviceRegistryRemote.shutdown();
        deviceRegistrySynchronizer.shutdown();
        registryProvider = null;
    }

    @Override
    public DALBindingRegistry getBindingRegistry() {
        return bindingRegistry;
    }

    @Override
    public DeviceRegistry getDeviceRegistry() {
        return deviceRegistry;
    }

    @Override
    public UnitRegistry getUnitRegistry() {
        return unitRegistry;
    }

    @Override
    public DeviceRegistryRemote getDeviceRegistryRemote() {
        return deviceRegistryRemote;
    }

    @Override
    public LocationRegistryRemote getLocationRegistryRemote() {
        return locationRegistryRemote;
    }
}
