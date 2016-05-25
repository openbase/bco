package org.dc.bco.manager.device.core;

/*
 * #%L
 * COMA DeviceManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
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
    public void waitForInit(long timeout, TimeUnit timeUnit) throws CouldNotPerformException {
        locationRegistry.waitForData(timeout, timeUnit);
        deviceRegistry.waitForData(timeout, timeUnit);
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
