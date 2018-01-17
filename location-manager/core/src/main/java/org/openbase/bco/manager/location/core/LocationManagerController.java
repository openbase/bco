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
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.manager.location.lib.ConnectionController;
import org.openbase.bco.manager.location.lib.ConnectionFactory;
import org.openbase.bco.manager.location.lib.LocationController;
import org.openbase.bco.manager.location.lib.LocationFactory;
import org.openbase.bco.manager.location.lib.LocationManager;
import org.openbase.bco.manager.location.lib.unitgroup.UnitGroupController;
import org.openbase.bco.manager.location.lib.unitgroup.UnitGroupFactory;
import org.openbase.bco.registry.login.SystemLogin;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.openbase.jul.storage.registry.ControllerRegistryImpl;
import org.openbase.jul.storage.registry.RegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.EnablingStateType.EnablingState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import unitgroup.UnitGroupFactoryImpl;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationManagerController implements LocationManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(LocationManagerController.class);

    private final LocationFactory locationFactory;
    private final ConnectionFactory connectionFactory;
    private final ControllerRegistryImpl<String, LocationController> locationRegistry;
    private final ControllerRegistryImpl<String, ConnectionController> connectionRegistry;
    private final ActivatableEntryRegistrySynchronizer<String, LocationController, UnitConfig, UnitConfig.Builder> locationRegistrySynchronizer;
    private final ActivatableEntryRegistrySynchronizer<String, ConnectionController, UnitConfig, UnitConfig.Builder> connectionRegistrySynchronizer;

    private final UnitGroupFactory unitGrouptFactory;
    private final ControllerRegistryImpl<String, UnitGroupController> unitGroupRegistry;
    private final ActivatableEntryRegistrySynchronizer<String, UnitGroupController, UnitConfig, UnitConfig.Builder> unitGroupRegistrySynchronizer;

    public LocationManagerController() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.locationFactory = LocationFactoryImpl.getInstance();
            this.connectionFactory = ConnectionFactoryImpl.getInstance();
            this.locationRegistry = new ControllerRegistryImpl<>();
            this.connectionRegistry = new ControllerRegistryImpl<>();
            this.locationRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, LocationController, UnitConfig, UnitConfig.Builder>(locationRegistry, Registries.getLocationRegistry().getLocationConfigRemoteRegistry(), Registries.getLocationRegistry(), locationFactory) {

                @Override
                public boolean activationCondition(final UnitConfig config) {
                    return config.getEnablingState().getValue() == State.ENABLED;
                }
            };
            this.connectionRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, ConnectionController, UnitConfig, UnitConfig.Builder>(connectionRegistry, Registries.getLocationRegistry().getConnectionConfigRemoteRegistry(), Registries.getLocationRegistry(), connectionFactory) {

                @Override
                public boolean activationCondition(final UnitConfig config) {
                    return config.getEnablingState().getValue() == State.ENABLED;
                }
            };

            this.unitGrouptFactory = UnitGroupFactoryImpl.getInstance();
            this.unitGroupRegistry = new ControllerRegistryImpl<>();
            this.unitGroupRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, UnitGroupController, UnitConfig, UnitConfig.Builder>(unitGroupRegistry, Registries.getUnitRegistry().getUnitGroupUnitConfigRemoteRegistry(), Registries.getUnitRegistry(), unitGrouptFactory) {

                @Override
                public boolean activationCondition(final UnitConfig config) {
                    return config.getEnablingState().getValue() == State.ENABLED;
                }
            };
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        // This has to stay. Else do not implement VoidInitializable. 
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        // TODO: pleminoq: let us analyse why this waitForData is needed. Without the sychnchronizer sync task is interrupted. And why is this never happening in the unit tests???
        Registries.getLocationRegistry().waitForData();

        SystemLogin.loginBCOUser();

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
    public RegistryImpl<String, LocationController> getLocationControllerRegistry() {
        return locationRegistry;
    }

    @Override
    public RegistryImpl<String, ConnectionController> getConnectionControllerRegistry() {
        return connectionRegistry;
    }
}
