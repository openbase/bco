package org.openbase.bco.dal.control.layer.unit.device;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.dal.control.layer.unit.UnitControllerRegistrySynchronizer;
import org.openbase.bco.dal.control.layer.unit.gateway.GatewayControllerFactoryImpl;
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactory;
import org.openbase.bco.dal.lib.layer.service.UnitDataSourceFactory;
import org.openbase.bco.dal.lib.layer.service.mock.OperationServiceFactoryMock;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistryImpl;
import org.openbase.bco.dal.lib.layer.unit.device.DeviceController;
import org.openbase.bco.dal.lib.layer.unit.device.DeviceControllerFactory;
import org.openbase.bco.dal.lib.layer.unit.device.DeviceManager;
import org.openbase.bco.dal.lib.layer.unit.gateway.GatewayController;
import org.openbase.bco.dal.lib.layer.unit.gateway.GatewayControllerFactory;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DeviceManagerImpl implements DeviceManager, Launchable<Void>, VoidInitializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceManagerImpl.class);

    //TODO: please remove in future release
    private static DeviceManagerImpl instance;

    private final GatewayControllerFactory gatewayControllerFactory;

    private final UnitControllerRegistryImpl<GatewayController> gatewayControllerRegistry;
    private final UnitControllerRegistryImpl<DeviceController> deviceControllerRegistry;
    private final UnitControllerRegistryImpl<UnitController<?, ?>> unitControllerRegistry;

    private final UnitControllerRegistrySynchronizer<GatewayController> gatewayRegistrySynchronizer;
    private final UnitControllerRegistrySynchronizer<DeviceController> deviceRegistrySynchronizer;

    /**
     * This construction is using a service factory mock and is only suitable for the testing purpose.
     *
     * @throws org.openbase.jul.exception.InstantiationException
     * @throws InterruptedException
     */
    public DeviceManagerImpl() throws InstantiationException, InterruptedException {
        this(OperationServiceFactoryMock.getInstance(), true);
    }

    public DeviceManagerImpl(final OperationServiceFactory operationServiceFactory, final boolean autoLogin) throws InstantiationException, InterruptedException {
        this(operationServiceFactory, null, autoLogin);
    }

    public DeviceManagerImpl(final OperationServiceFactory operationServiceFactory, final UnitDataSourceFactory unitDataSourceFactory, final boolean autoLogin) throws InstantiationException, InterruptedException {
        this(new GatewayControllerFactoryImpl(new DeviceControllerFactoryImpl(operationServiceFactory, unitDataSourceFactory)), autoLogin);
    }

    public DeviceManagerImpl(final GatewayControllerFactory gatewayControllerFactory, final boolean autoLogin) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            if(autoLogin) {
                BCOLogin.getSession().loginBCOUser();
            }
            DeviceManagerImpl.instance = this;

            this.gatewayControllerFactory = gatewayControllerFactory;

            this.gatewayControllerRegistry = new UnitControllerRegistryImpl<>();
            this.unitControllerRegistry = new UnitControllerRegistryImpl<>();
            this.deviceControllerRegistry = new UnitControllerRegistryImpl<>();

            Registries.waitForData();

            this.gatewayRegistrySynchronizer = new UnitControllerRegistrySynchronizer<>(gatewayControllerRegistry, Registries.getUnitRegistry().getGatewayUnitConfigRemoteRegistry(false), gatewayControllerFactory);
            this.gatewayRegistrySynchronizer.addFilter(unitConfig -> !DeviceManagerImpl.this.isGatewaySupported(unitConfig));

            this.deviceRegistrySynchronizer = new UnitControllerRegistrySynchronizer<>(deviceControllerRegistry, Registries.getUnitRegistry().getDeviceUnitConfigRemoteRegistry(false), gatewayControllerFactory.getDeviceControllerFactory());
            this.deviceRegistrySynchronizer.addFilter(unitConfig -> !DeviceManagerImpl.this.isUnitSupported(unitConfig));
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
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        gatewayControllerRegistry.activate();
        deviceControllerRegistry.activate();
        unitControllerRegistry.activate();

        gatewayRegistrySynchronizer.activate();
        deviceRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return gatewayControllerRegistry.isActive() &&
                deviceControllerRegistry.isActive() &&
                unitControllerRegistry.isActive() &&
                gatewayRegistrySynchronizer.isActive() &&
                deviceRegistrySynchronizer.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        gatewayRegistrySynchronizer.deactivate();
        deviceRegistrySynchronizer.deactivate();
        gatewayControllerRegistry.deactivate();
        deviceControllerRegistry.deactivate();
        unitControllerRegistry.deactivate();
    }

    @Override
    public void shutdown() {
        gatewayRegistrySynchronizer.shutdown();
        deviceRegistrySynchronizer.shutdown();

        gatewayControllerRegistry.shutdown();
        deviceControllerRegistry.shutdown();
        unitControllerRegistry.shutdown();

        instance = null;
    }

    @Override
    public UnitControllerRegistry<GatewayController> getGatewayControllerRegistry() {
        return gatewayControllerRegistry;
    }

    @Override
    public UnitControllerRegistry<DeviceController> getDeviceControllerRegistry() {
        return deviceControllerRegistry;
    }

    @Override
    public UnitControllerRegistry<UnitController<?, ?>> getUnitControllerRegistry() {
        return unitControllerRegistry;
    }

    @Override
    public OperationServiceFactory getOperationServiceFactory() throws NotAvailableException {
        return getDeviceControllerFactory().getOperationServiceFactory();
    }

    @Override
    public UnitDataSourceFactory getUnitDataSourceFactory() throws NotAvailableException {
        if (getDeviceControllerFactory().getUnitDataSourceFactory() == null) {
            throw new NotAvailableException("UnitDataSourceFactory", new NotSupportedException("UnitDataSource", this));
        }
        return getDeviceControllerFactory().getUnitDataSourceFactory();
    }

    @Override
    public DeviceControllerFactory getDeviceControllerFactory() throws NotAvailableException {
        return gatewayControllerFactory.getDeviceControllerFactory();
    }
}
