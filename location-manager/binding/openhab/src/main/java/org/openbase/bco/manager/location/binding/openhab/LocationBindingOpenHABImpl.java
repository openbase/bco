package org.openbase.bco.manager.location.binding.openhab;

/*
 * #%L
 * BCO Manager Location Binding OpenHAB
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
import org.openbase.bco.manager.location.binding.openhab.unitgroup.UnitGroupRemoteFactoryImpl;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.unit.unitgroup.UnitGroupRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.openhab.binding.AbstractOpenHABBinding;
import org.openbase.jul.storage.registry.RegistrySynchronizer;
import org.openbase.jul.storage.registry.RemoteControllerRegistryImpl;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class LocationBindingOpenHABImpl extends AbstractOpenHABBinding {

    //TODO: Should be declared in the openhab config generator and used from there
    public static final String LOCATION_MANAGER_ITEM_FILTER = "bco.manager.location";

    private final LocationRemoteFactoryImpl locationRemoteFactory;
    private final ConnectionRemoteFactoryImpl connectionRemoteFactory;
    private final RegistrySynchronizer<String, LocationRemote, UnitConfig, UnitConfig.Builder> locationRegistrySynchronizer;
    private final RegistrySynchronizer<String, ConnectionRemote, UnitConfig, UnitConfig.Builder> connectionRegistrySynchronizer;
    private final RemoteControllerRegistryImpl<String, LocationRemote> locationRegistry;
    private final RemoteControllerRegistryImpl<String, ConnectionRemote> connectionRegistry;
    private final boolean hardwareSimulationMode;
    private boolean active;

    private final UnitGroupRemoteFactoryImpl unitGroupRemoteFactory;
    private final RegistrySynchronizer<String, UnitGroupRemote, UnitConfig, UnitConfig.Builder> unitGroupRegistrySynchronizer;
    private final RemoteControllerRegistryImpl<String, UnitGroupRemote> unitGroupRegistry;

    public LocationBindingOpenHABImpl() throws InstantiationException, JPNotAvailableException, InterruptedException {
        super();
        try {
            hardwareSimulationMode = JPService.getProperty(JPHardwareSimulationMode.class).getValue();
            locationRegistry = new RemoteControllerRegistryImpl<>();
            connectionRegistry = new RemoteControllerRegistryImpl<>();
            locationRemoteFactory = new LocationRemoteFactoryImpl();
            connectionRemoteFactory = new ConnectionRemoteFactoryImpl();
            locationRegistrySynchronizer = new RegistrySynchronizer<String, LocationRemote, UnitConfig, UnitConfig.Builder>(locationRegistry, Registries.getLocationRegistry().getLocationConfigRemoteRegistry(), Registries.getLocationRegistry(), locationRemoteFactory) {

                @Override
                public boolean verifyConfig(UnitConfig config) throws VerificationFailedException {
                    return config.getEnablingState().getValue() == EnablingState.State.ENABLED;
                }
            };
            this.connectionRegistrySynchronizer = new RegistrySynchronizer<String, ConnectionRemote, UnitConfig, UnitConfig.Builder>(connectionRegistry, Registries.getLocationRegistry().getConnectionConfigRemoteRegistry(), Registries.getLocationRegistry(), connectionRemoteFactory) {

                @Override
                public boolean verifyConfig(UnitConfig config) throws VerificationFailedException {
                    return config.getEnablingState().getValue() == EnablingState.State.ENABLED;
                }
            };

            unitGroupRegistry = new RemoteControllerRegistryImpl<>();
            unitGroupRemoteFactory = new UnitGroupRemoteFactoryImpl();
            unitGroupRegistrySynchronizer = new RegistrySynchronizer<String, UnitGroupRemote, UnitConfig, UnitConfig.Builder>(unitGroupRegistry, Registries.getUnitRegistry().getUnitGroupUnitConfigRemoteRegistry(), Registries.getUnitRegistry(), unitGroupRemoteFactory) {

                @Override
                public boolean verifyConfig(UnitConfig config) throws VerificationFailedException {
                    return config.getEnablingState().getValue() == EnablingState.State.ENABLED;
                }
            };
        } catch (final CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        super.init(LOCATION_MANAGER_ITEM_FILTER, new LocationBindingOpenHABRemote(hardwareSimulationMode, locationRegistry, connectionRegistry, unitGroupRegistry));
        try {
            Registries.getLocationRegistry().waitForData();
            locationRemoteFactory.init(openHABRemote);
            connectionRemoteFactory.init(openHABRemote);
            unitGroupRemoteFactory.init(openHABRemote);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        active = true;
        locationRegistrySynchronizer.activate();
        connectionRegistrySynchronizer.activate();
        unitGroupRegistrySynchronizer.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;
        locationRegistrySynchronizer.deactivate();
        connectionRegistrySynchronizer.deactivate();
        unitGroupRegistrySynchronizer.deactivate();
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
