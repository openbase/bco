package org.openbase.bco.manager.location.binding.openhab;

/*
 * #%L
 * BCO Manager Location Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.openhab.binding.AbstractOpenHABBinding;
import org.openbase.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.openbase.jul.storage.registry.RemoteControllerRegistryImpl;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class LocationBindingOpenHABImpl extends AbstractOpenHABBinding {

    //TODO: Should be declared in the openhab config generator and used from there
    public static final String LOCATION_MANAGER_ITEM_FILTER = "bco.manager.location";

    private final LocationRegistryRemote locationRegistryRemote;
    private final LocationRemoteFactoryImpl locationRemoteFactory;
    private final ConnectionRemoteFactoryImpl connectionRemoteFactory;
    private final ActivatableEntryRegistrySynchronizer<String, LocationRemote, UnitConfig, UnitConfig.Builder> locationRegistrySynchronizer;
    private final ActivatableEntryRegistrySynchronizer<String, ConnectionRemote, UnitConfig, UnitConfig.Builder> connectionRegistrySynchronizer;
    private final RemoteControllerRegistryImpl<String, LocationRemote> locationRegistry;
    private final RemoteControllerRegistryImpl<String, ConnectionRemote> connectionRegistry;
    private final boolean hardwareSimulationMode;
    private boolean active;

    public LocationBindingOpenHABImpl() throws InstantiationException, JPNotAvailableException, InterruptedException {
        super();
        hardwareSimulationMode = JPService.getProperty(JPHardwareSimulationMode.class).getValue();
        locationRegistryRemote = new LocationRegistryRemote();
        locationRegistry = new RemoteControllerRegistryImpl<>();
        connectionRegistry = new RemoteControllerRegistryImpl<>();
        locationRemoteFactory = new LocationRemoteFactoryImpl();
        connectionRemoteFactory = new ConnectionRemoteFactoryImpl();
        this.locationRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, LocationRemote, UnitConfig, UnitConfig.Builder>(locationRegistry, locationRegistryRemote.getLocationConfigRemoteRegistry(), locationRemoteFactory) {

            @Override
            public boolean activationCondition(UnitConfig config) {
                return true;
            }
        };
        this.connectionRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, ConnectionRemote, UnitConfig, UnitConfig.Builder>(connectionRegistry, locationRegistryRemote.getConnectionConfigRemoteRegistry(), connectionRemoteFactory) {

            @Override
            public boolean activationCondition(UnitConfig config) {
                return true;
            }
        };
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        super.init(LOCATION_MANAGER_ITEM_FILTER, new LocationBindingOpenHABRemote(hardwareSimulationMode, locationRegistry, connectionRegistry));
        try {
            locationRemoteFactory.init(openHABRemote);
            locationRegistryRemote.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        active = true;
        locationRegistryRemote.activate();
        locationRegistrySynchronizer.activate();
        connectionRegistrySynchronizer.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;
        locationRegistryRemote.deactivate();
        locationRegistrySynchronizer.deactivate();
        connectionRegistrySynchronizer.deactivate();
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
