package org.openbase.bco.manager.device.core;

/*
 * #%L
 * BCO Manager Device Core
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.ServiceFactory;
import org.openbase.bco.dal.lib.layer.service.mock.ServiceFactoryMock;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistryImpl;
import org.openbase.bco.manager.device.lib.DeviceController;
import org.openbase.bco.manager.device.lib.DeviceFactory;
import org.openbase.bco.manager.device.lib.DeviceManager;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.openbase.jul.storage.registry.ControllerRegistry;
import org.openbase.jul.storage.registry.RegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.InventoryStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class DeviceManagerController implements DeviceManager, Launchable<Void>, VoidInitializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceManagerController.class);

    private static DeviceManagerController instance;

    private final DeviceFactory deviceFactory;
    private final ServiceFactory serviceFactory;

    private final UnitRegistryRemote unitRegistryRemote;

    private final ControllerRegistry<String, DeviceController> deviceControllerRegistry;
    private final UnitControllerRegistryImpl unitControllerRegistry;

    private final ActivatableEntryRegistrySynchronizer<String, DeviceController, UnitConfig, UnitConfig.Builder> deviceRegistrySynchronizer;
    
    private boolean active = false;

    /**
     * This construction is using a service factory mock and is only suitable for the testing purpose.
     * @throws org.openbase.jul.exception.InstantiationException
     * @throws InterruptedException 
     */
    public DeviceManagerController() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        this(new ServiceFactoryMock());
    }
    
    public DeviceManagerController(final ServiceFactory serviceFactory) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        this(serviceFactory, new DeviceFactoryImpl(serviceFactory));
    }

    public DeviceManagerController(final ServiceFactory serviceFactory, final DeviceFactory deviceFactory) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            DeviceManagerController.instance = this;

            this.deviceFactory = deviceFactory;
            this.serviceFactory = serviceFactory;

            this.unitControllerRegistry = new UnitControllerRegistryImpl();
            this.deviceControllerRegistry = new ControllerRegistry<>();
            
            CachedUnitRegistryRemote.waitForData();
            
            this.unitRegistryRemote = CachedUnitRegistryRemote.getRegistry();

            this.deviceRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, DeviceController, UnitConfig, UnitConfig.Builder>(deviceControllerRegistry, unitRegistryRemote.getDeviceUnitConfigRemoteRegistry(), deviceFactory) {

                @Override
                public boolean activationCondition(UnitConfig config) {
                    return true;
                }

                @Override
                public boolean verifyConfig(final UnitConfig config) throws VerificationFailedException {
                    try {

                        // verify device manager support.
                        if (!DeviceManagerController.this.isSupported(config)) {
                            return false;
                        }

                        // verify device state.
                        if (config.getDeviceConfig().getInventoryState().getValue() != InventoryStateType.InventoryState.State.INSTALLED) {
                            LOGGER.info("Skip Device[" + config.getLabel() + "] because it is currently not installed!");
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

    @Override
    public void init() throws InitializationException, InterruptedException {
//        try {
//        } catch (CouldNotPerformException ex) {
//            throw new InitializationException(this, ex);
//        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        active = true;
        // TODO: pleminoq: let us analyse why this wait For data is needed. Without the sychnchronizer sync task is interrupted. And why is this never happening in the unit tests???
        unitRegistryRemote.waitForData();
        deviceRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;
        deviceRegistrySynchronizer.deactivate();
        deviceControllerRegistry.clear();
        unitControllerRegistry.clear();
    }

    @Override
    public void shutdown() {
        deviceControllerRegistry.shutdown();
        unitControllerRegistry.shutdown();
        deviceRegistrySynchronizer.shutdown();
        instance = null;
    }

    @Override
    public void waitForInit(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        unitRegistryRemote.waitForData(timeout, timeUnit);
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
    public boolean isSupported(UnitConfig config) throws CouldNotPerformException {
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
