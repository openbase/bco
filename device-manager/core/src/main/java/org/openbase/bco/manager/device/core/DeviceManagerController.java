package org.openbase.bco.manager.device.core;

/*
 * #%L
 * BCO Manager Device Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import org.openbase.bco.dal.lib.layer.service.OperationServiceFactory;
import org.openbase.bco.dal.lib.layer.service.mock.OperationServiceFactoryMock;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistryImpl;
import org.openbase.bco.dal.lib.simulation.UnitSimulationManager;
import org.openbase.bco.manager.device.lib.DeviceController;
import org.openbase.bco.manager.device.lib.DeviceControllerFactory;
import org.openbase.bco.manager.device.lib.DeviceManager;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.openbase.jul.storage.registry.ControllerRegistryImpl;
import org.openbase.jul.storage.registry.RegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.EnablingStateType.EnablingState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DeviceManagerController implements DeviceManager, Launchable<Void>, VoidInitializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceManagerController.class);

    //TODO: please remove in future release
    private static DeviceManagerController instance;

    private final DeviceControllerFactory deviceControllerFactory;
    private final OperationServiceFactory operationServiceFactory;
    private final UnitSimulationManager unitSimulationManager;

    private final ControllerRegistryImpl<String, DeviceController> deviceControllerRegistry;
    private final UnitControllerRegistryImpl unitControllerRegistry;

    private final ActivatableEntryRegistrySynchronizer<String, DeviceController, UnitConfig, UnitConfig.Builder> deviceRegistrySynchronizer;

    private boolean active = false;

    /**
     * This construction is using a service factory mock and is only suitable for the testing purpose.
     *
     * @throws org.openbase.jul.exception.InstantiationException
     * @throws InterruptedException
     */
    public DeviceManagerController() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        this(OperationServiceFactoryMock.getInstance());
    }

    public DeviceManagerController(final OperationServiceFactory operationServiceFactory) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        this(operationServiceFactory, new DeviceControllerFactoryImpl(operationServiceFactory));
    }

    public DeviceManagerController(final OperationServiceFactory operationServiceFactory, final DeviceControllerFactory deviceControllerFactory) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            DeviceManagerController.instance = this;

            this.deviceControllerFactory = deviceControllerFactory;
            this.operationServiceFactory = operationServiceFactory;

            this.unitControllerRegistry = new UnitControllerRegistryImpl();
            this.deviceControllerRegistry = new ControllerRegistryImpl<>();

            Registries.waitForData();

            this.deviceRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, DeviceController, UnitConfig, UnitConfig.Builder>(deviceControllerRegistry, Registries.getUnitRegistry().getDeviceUnitConfigRemoteRegistry(), Registries.getUnitRegistry(), deviceControllerFactory) {

                @Override
                public boolean activationCondition(final UnitConfig config) {
                    return true;
                }

                @Override
                public boolean verifyConfig(final UnitConfig config) throws VerificationFailedException {
                    try {
                        // verify device manager support.
                        if (!DeviceManagerController.this.isSupported(config)) {
                            return false;
                        }

                        // skip if device is not enabled
                        if (config.getEnablingState().getValue() != State.ENABLED) {
                            return false;
                        }
                        return true;
                    } catch (CouldNotPerformException ex) {
                        throw new VerificationFailedException("Could not verify device config!", ex);
                    }
                }
            };

            // handle simulation mode
            this.unitSimulationManager = new UnitSimulationManager();
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
        // this overwrite is needed to overwrite the default implementation!
        unitSimulationManager.init(unitControllerRegistry);
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        active = true;
        BCOLogin.loginBCOUser();
        deviceRegistrySynchronizer.activate();
        unitSimulationManager.activate();
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;
        unitSimulationManager.deactivate();
        deviceRegistrySynchronizer.deactivate();
        deviceControllerRegistry.clear();
        unitControllerRegistry.clear();
    }

    @Override
    public void shutdown() {
        deviceRegistrySynchronizer.shutdown();
        deviceControllerRegistry.shutdown();
        unitControllerRegistry.shutdown();
        unitSimulationManager.shutdown();
        instance = null;
    }

    @Override
    public RegistryImpl<String, DeviceController> getDeviceControllerRegistry() {
        return deviceControllerRegistry;
    }

    @Override
    public UnitControllerRegistry<?, ?> getUnitControllerRegistry() {
        return unitControllerRegistry;
    }

    /**
     * All devices will be supported by default. Feel free to overwrite method
     * to changing this behavior.
     *
     * @param config
     *
     * @return
     *
     * @throws CouldNotPerformException
     */
    @Override
    public boolean isSupported(final UnitConfig config) throws CouldNotPerformException {
        return true;
    }

    @Override
    public OperationServiceFactory getOperationServiceFactory() throws NotAvailableException {
        return operationServiceFactory;
    }

    @Override
    public DeviceControllerFactory getDeviceControllerFactory() throws NotAvailableException {
        return deviceControllerFactory;
    }
}
