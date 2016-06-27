package org.openbase.bco.manager.device.core;

/*
 * #%L
 * COMA DeviceManager Core
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import java.util.concurrent.TimeUnit;
import org.openbase.bco.manager.device.lib.DeviceManager;
import org.openbase.bco.dal.lib.layer.service.ServiceFactory;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistryImpl;
import org.openbase.bco.manager.device.lib.DeviceController;
import org.openbase.bco.manager.device.lib.DeviceFactory;
import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.openbase.jul.storage.registry.ControllerRegistry;
import org.openbase.jul.storage.registry.RegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.state.InventoryStateType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public class DeviceManagerController implements DeviceManager {

    private static final Logger logger = LoggerFactory.getLogger(DeviceManagerController.class);

    private static DeviceManagerController instance;

    private final DeviceFactory deviceFactory;
    private final ServiceFactory serviceFactory;

    private final LocationRegistryRemote locationRegistryRemote;
    private final DeviceRegistryRemote deviceRegistryRemote;

    private final ControllerRegistry<String, DeviceController> deviceControllerRegistry;
    private final UnitControllerRegistryImpl unitControllerRegistry;

    private final ActivatableEntryRegistrySynchronizer<String, DeviceController, DeviceConfig, DeviceConfig.Builder> deviceRegistrySynchronizer;

    public DeviceManagerController(final ServiceFactory serviceFactory) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        this(serviceFactory, new DeviceFactoryImpl(serviceFactory));
    }

    public DeviceManagerController(final ServiceFactory serviceFactory, final DeviceFactory deviceFactory) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.instance = this;

            this.deviceFactory = deviceFactory;
            this.serviceFactory = serviceFactory;

            this.unitControllerRegistry = new UnitControllerRegistryImpl();
            this.deviceControllerRegistry = new ControllerRegistry<>();
            CachedLocationRegistryRemote.waitForData();
            CachedDeviceRegistryRemote.waitForData();
            this.locationRegistryRemote = (LocationRegistryRemote) CachedLocationRegistryRemote.getRegistry();
            this.deviceRegistryRemote = (DeviceRegistryRemote) CachedDeviceRegistryRemote.getRegistry();

            this.deviceRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, DeviceController, DeviceConfig, DeviceConfig.Builder>(deviceControllerRegistry, deviceRegistryRemote.getDeviceConfigRemoteRegistry(), deviceFactory) {

                @Override
                public boolean activationCondition(DeviceConfig config) {
                    return true;
                }

                @Override
                public boolean verifyConfig(final DeviceConfig config) throws VerificationFailedException {
                    try {

                        // verify device class.
                        try {
                            deviceRegistryRemote.containsDeviceClassById(config.getDeviceClassId());
                        } catch (CouldNotPerformException ex) {
                            throw new VerificationFailedException("DeviceClass[" + config.getDeviceClassId() + "] of Device[" + config.getId() + "] is not supported yet!", ex);
                        }

                        // verify device manager support.
                        if (!DeviceManagerController.this.isSupported(config)) {
                            return false;
                        }

                        // verify device state.
                        if (config.getInventoryState().getValue() != InventoryStateType.InventoryState.State.INSTALLED) {
                            logger.info("Skip Device[" + config.getLabel() + "] because it is currently not installed!");
                            return false;
                        }
                        return true;
                    } catch (CouldNotPerformException ex) {
                        throw new VerificationFailedException("Could not verify device config!", ex);
                    }
                }
            };
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
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
            System.out.println("Init & activate device registry");
            deviceRegistryRemote.init();
            deviceRegistryRemote.activate();
            System.out.println("Wait for device data");
            deviceRegistryRemote.waitForData();

            System.out.println("Init & activate location registry");
            locationRegistryRemote.init();
            locationRegistryRemote.activate();
            System.out.println("Wait for location data");
            locationRegistryRemote.waitForData();
            System.out.println("Init registry syncroniser...");
            deviceRegistrySynchronizer.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        deviceControllerRegistry.shutdown();
        unitControllerRegistry.shutdown();
        deviceRegistrySynchronizer.shutdown();
        instance = null;
    }

    @Override
    public void waitForInit(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        System.out.println("Wait for init");
        locationRegistryRemote.waitForData(timeout, timeUnit);
        deviceRegistryRemote.waitForData(timeout, timeUnit);
        System.out.println("Wait for init finished!");
    }

    @Override
    public DeviceRegistryRemote getDeviceRegistry() {
        return deviceRegistryRemote;
    }

    @Override
    public LocationRegistryRemote getLocationRegistry() {
        return locationRegistryRemote;
    }

    @Override
    public RegistryImpl<String, DeviceController> getDeviceControllerRegistry() {
        return deviceControllerRegistry;
    }

    @Override
    public UnitControllerRegistry getUnitControllerRegistry() {
        return unitControllerRegistry;
    }

    /**
     * All devices will be supported by default. Feel free to overwrite method
     * to changing this behavior.
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
