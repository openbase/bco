package org.openbase.bco.dal.control.layer.unit.location;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import org.openbase.bco.dal.control.layer.unit.connection.ConnectionControllerFactoryImpl;
import org.openbase.bco.dal.control.layer.unit.unitgroup.UnitGroupControllerFactoryImpl;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistryImpl;
import org.openbase.bco.dal.lib.layer.unit.connection.ConnectionController;
import org.openbase.bco.dal.lib.layer.unit.connection.ConnectionControllerFactory;
import org.openbase.bco.dal.lib.layer.unit.location.LocationController;
import org.openbase.bco.dal.lib.layer.unit.location.LocationControllerFactory;
import org.openbase.bco.dal.lib.layer.unit.location.LocationManager;
import org.openbase.bco.dal.lib.layer.unit.unitgroup.UnitGroupController;
import org.openbase.bco.dal.lib.layer.unit.unitgroup.UnitGroupControllerFactory;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationManagerImpl implements LocationManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(LocationManagerImpl.class);

    private final LocationControllerFactory locationControllerFactory;
    private final ConnectionControllerFactory connectionControllerFactory;
    private final UnitGroupControllerFactory unitGroupFactory;

    private final UnitControllerRegistry<LocationController> locationControllerRegistry;
    private final UnitControllerRegistry<ConnectionController> connectionControllerRegistry;
    private final UnitControllerRegistry<UnitGroupController> unitGroupControllerRegistry;

    private final UnitControllerRegistrySynchronizer<LocationController> locationRegistrySynchronizer;
    private final UnitControllerRegistrySynchronizer<ConnectionController> connectionRegistrySynchronizer;
    private final UnitControllerRegistrySynchronizer<UnitGroupController> unitGroupRegistrySynchronizer;

    public LocationManagerImpl() throws InstantiationException {
        try {
            // init factories
            this.locationControllerFactory = LocationControllerFactoryImpl.getInstance();
            this.connectionControllerFactory = ConnectionControllerFactoryImpl.getInstance();
            this.unitGroupFactory = UnitGroupControllerFactoryImpl.getInstance();

            // init controller registries
            this.locationControllerRegistry = new UnitControllerRegistryImpl<>();
            this.connectionControllerRegistry = new UnitControllerRegistryImpl<>();
            this.unitGroupControllerRegistry = new UnitControllerRegistryImpl<>();

            // init synchronizer
            this.locationRegistrySynchronizer = new UnitControllerRegistrySynchronizer<>(locationControllerRegistry, Registries.getUnitRegistry().getLocationUnitConfigRemoteRegistry(false), locationControllerFactory);
            this.connectionRegistrySynchronizer = new UnitControllerRegistrySynchronizer<>(connectionControllerRegistry, Registries.getUnitRegistry().getConnectionUnitConfigRemoteRegistry(false), connectionControllerFactory);
            this.unitGroupRegistrySynchronizer = new UnitControllerRegistrySynchronizer<>(unitGroupControllerRegistry, Registries.getUnitRegistry().getUnitGroupUnitConfigRemoteRegistry(false), unitGroupFactory);
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        // this overwrite is needed to overwrite the default implementation!
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        BCOLogin.getSession().loginBCOUser();
        locationControllerRegistry.activate();
        connectionControllerRegistry.activate();
        unitGroupControllerRegistry.activate();
        locationRegistrySynchronizer.activate();
        connectionRegistrySynchronizer.activate();
        unitGroupRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return locationControllerRegistry.isActive() &&
                connectionControllerRegistry.isActive() &&
                unitGroupControllerRegistry.isActive() &&
                locationRegistrySynchronizer.isActive() &&
                locationRegistrySynchronizer.isActive() &&
                unitGroupRegistrySynchronizer.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        locationRegistrySynchronizer.deactivate();
        connectionRegistrySynchronizer.deactivate();
        unitGroupRegistrySynchronizer.deactivate();
        locationControllerRegistry.deactivate();
        connectionControllerRegistry.deactivate();
        unitGroupControllerRegistry.deactivate();
    }

    @Override
    public void shutdown() {
        locationRegistrySynchronizer.shutdown();
        connectionRegistrySynchronizer.shutdown();
        unitGroupRegistrySynchronizer.shutdown();
        locationControllerRegistry.shutdown();
        connectionControllerRegistry.shutdown();
        unitGroupControllerRegistry.shutdown();
    }

    @Override
    public UnitControllerRegistry<LocationController> getLocationControllerRegistry() {
        return this.locationControllerRegistry;
    }

    @Override
    public UnitControllerRegistry<ConnectionController> getConnectionControllerRegistry() {
        return connectionControllerRegistry;
    }
}
