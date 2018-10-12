package org.openbase.bco.manager.location.core;

/*
 * #%L
 * BCO Manager Location Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistryImpl;
import org.openbase.bco.manager.location.lib.*;
import org.openbase.bco.manager.location.lib.unitgroup.UnitGroupController;
import org.openbase.bco.manager.location.lib.unitgroup.UnitGroupControllerFactory;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationManagerController implements LocationManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(LocationManagerController.class);

//    private final UnitSimulationManager unitSimulationManager;

    private final LocationControllerFactory locationControllerFactory;
    private final ConnectionControllerFactory connectionControllerFactory;
    private final UnitGroupControllerFactory unitGroupFactory;

    private final UnitControllerRegistry<LocationController> locationControllerRegistry;
    private final UnitControllerRegistry<ConnectionController> connectionControllerRegistry;
    private final UnitControllerRegistry<UnitGroupController> unitGroupControllerRegistry;

    private final UnitControllerRegistrySynchronizer<LocationController> locationRegistrySynchronizer;
    private final UnitControllerRegistrySynchronizer<ConnectionController> connectionRegistrySynchronizer;
    private final UnitControllerRegistrySynchronizer<UnitGroupController> unitGroupRegistrySynchronizer;

    public LocationManagerController() throws org.openbase.jul.exception.InstantiationException {
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
            this.locationRegistrySynchronizer = new UnitControllerRegistrySynchronizer<>(locationControllerRegistry, Registries.getUnitRegistry().getLocationUnitConfigRemoteRegistry(), locationControllerFactory);
            this.connectionRegistrySynchronizer = new UnitControllerRegistrySynchronizer<>(connectionControllerRegistry, Registries.getUnitRegistry().getConnectionUnitConfigRemoteRegistry(), connectionControllerFactory);
            this.unitGroupRegistrySynchronizer = new UnitControllerRegistrySynchronizer<>(unitGroupControllerRegistry, Registries.getUnitRegistry().getUnitGroupUnitConfigRemoteRegistry(), unitGroupFactory);

            // handle simulation mode
//            this.unitSimulationManager = new UnitSimulationManager();
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        // this overwrite is needed to overwrite the default implementation!
//        unitSimulationManager.init(locationControllerRegistry);
//        unitSimulationManager.init(connectionControllerRegistry);
//        unitSimulationManager.init(unitGroupControllerRegistry);
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        BCOLogin.loginBCOUser();
        locationRegistrySynchronizer.activate();
        connectionRegistrySynchronizer.activate();
        unitGroupRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return locationRegistrySynchronizer.isActive() && connectionRegistrySynchronizer.isActive() && unitGroupRegistrySynchronizer.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        locationRegistrySynchronizer.deactivate();
        connectionRegistrySynchronizer.deactivate();
        unitGroupRegistrySynchronizer.deactivate();
    }

    @Override
    public void shutdown() {
        locationRegistrySynchronizer.shutdown();
        connectionRegistrySynchronizer.shutdown();
        unitGroupRegistrySynchronizer.shutdown();
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
