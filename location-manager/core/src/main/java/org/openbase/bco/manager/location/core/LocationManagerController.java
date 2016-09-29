package org.openbase.bco.manager.location.core;

/*
 * #%L
 * COMA LocationManager Core
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import org.openbase.bco.manager.location.lib.ConnectionController;
import org.openbase.bco.manager.location.lib.ConnectionFactory;
import org.openbase.bco.manager.location.lib.LocationController;
import org.openbase.bco.manager.location.lib.LocationFactory;
import org.openbase.bco.manager.location.lib.LocationManager;
import org.openbase.bco.registry.device.lib.DeviceRegistry;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.bco.registry.location.lib.LocationRegistry;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.openbase.jul.storage.registry.ControllerRegistry;
import org.openbase.jul.storage.registry.RegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationManagerController implements LocationManager {

    protected static final Logger logger = LoggerFactory.getLogger(LocationManagerController.class);

    private static LocationManagerController instance;
    private final LocationRegistryRemote locationRegistryRemote;
    private final DeviceRegistryRemote deviceRegistryRemote;
    private final LocationFactory locationFactory;
    private final ConnectionFactory connectionFactory;
    private final ControllerRegistry<String, LocationController> locationRegistry;
    private final ControllerRegistry<String, ConnectionController> connectionRegistry;
    private final ActivatableEntryRegistrySynchronizer<String, LocationController, LocationConfig, LocationConfig.Builder> locationRegistrySynchronizer;
    private final ActivatableEntryRegistrySynchronizer<String, ConnectionController, ConnectionConfig, ConnectionConfig.Builder> connectionRegistrySynchronizer;

    public LocationManagerController() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.instance = this;
            this.locationRegistryRemote = new LocationRegistryRemote();
            this.deviceRegistryRemote = new DeviceRegistryRemote();
            this.locationFactory = LocationFactoryImpl.getInstance();
            this.connectionFactory = ConnectionFactoryImpl.getInstance();
            this.locationRegistry = new ControllerRegistry<>();
            this.connectionRegistry = new ControllerRegistry<>();
            this.locationRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, LocationController, LocationConfig, LocationConfig.Builder>(locationRegistry, locationRegistryRemote.getLocationConfigRemoteRegistry(), locationFactory) {

                @Override
                public boolean activationCondition(LocationConfig config) {
                    return true;
                }
            };
            this.connectionRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, ConnectionController, ConnectionConfig, ConnectionConfig.Builder>(connectionRegistry, locationRegistryRemote.getConnectionConfigRemoteRegistry(), connectionFactory) {

                @Override
                public boolean activationCondition(ConnectionConfig config) {
                    return true;
                }
            };
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    public static LocationManagerController getInstance() throws NotAvailableException {
        if (instance == null) {
            throw new NotAvailableException(LocationManagerController.class);
        }
        return instance;
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            this.locationRegistryRemote.init();
            this.locationRegistryRemote.activate();
            this.deviceRegistryRemote.init();
            this.deviceRegistryRemote.activate();
            this.locationRegistryRemote.waitForData();
            this.deviceRegistryRemote.waitForData();
            this.locationRegistrySynchronizer.init();
            this.connectionRegistrySynchronizer.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        this.locationRegistryRemote.shutdown();
        this.deviceRegistryRemote.shutdown();
        this.locationRegistrySynchronizer.shutdown();
        this.connectionRegistrySynchronizer.shutdown();
        instance = null;
    }

    @Override
    public LocationRegistry getLocationRegistry() throws NotAvailableException {
        return locationRegistryRemote;
    }

    @Override
    public DeviceRegistry getDeviceRegistry() throws NotAvailableException {
        return deviceRegistryRemote;
    }

    @Override
    public RegistryImpl<String, LocationController> getLocationControllerRegistry() {
        return locationRegistry;
    }

    @Override
    public RegistryImpl<String, ConnectionController> getConnectionControllerRegistry() {
        return connectionRegistry;
    }
}
