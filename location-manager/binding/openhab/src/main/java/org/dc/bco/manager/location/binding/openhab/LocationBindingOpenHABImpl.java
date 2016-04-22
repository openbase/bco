package org.dc.bco.manager.location.binding.openhab;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.manager.location.remote.ConnectionRemote;
import org.dc.bco.manager.location.remote.LocationRemote;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPNotAvailableException;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.extension.openhab.binding.AbstractOpenHABBinding;
import org.dc.jul.extension.openhab.binding.AbstractOpenHABRemote;
import org.dc.jul.extension.openhab.binding.interfaces.OpenHABRemote;
import org.dc.jul.storage.registry.RegistryImpl;
import org.dc.jul.storage.registry.RegistrySynchronizer;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public class LocationBindingOpenHABImpl extends AbstractOpenHABBinding {

    //TODO: Should be declared in the openhab config generator and used from there
    public static final String LOCATION_MANAGER_ITEM_FILTER = "bco.manager.location";

    private final LocationRegistryRemote locationRegistryRemote;
    private final LocationRemoteFactoryImpl locationRemoteFactory;
    private final ConnectionRemoteFactoryImpl connectionRemoteFactory;
    private final RegistrySynchronizer<String, LocationRemote, LocationConfig, LocationConfig.Builder> locationRegistrySynchronizer;
    private final RegistrySynchronizer<String, ConnectionRemote, ConnectionConfig, ConnectionConfig.Builder> connectionRegistrySynchronizer;
    private final RegistryImpl<String, LocationRemote> locationRegistry;
    private final RegistryImpl<String, ConnectionRemote> connectionRegistry;
    private final boolean hardwareSimulationMode;

    public LocationBindingOpenHABImpl() throws InstantiationException, JPNotAvailableException, InterruptedException {
        super();
        hardwareSimulationMode = JPService.getProperty(JPHardwareSimulationMode.class).getValue();
        locationRegistryRemote = new LocationRegistryRemote();
        locationRegistry = new RegistryImpl<>();
        connectionRegistry = new RegistryImpl<>();
        locationRemoteFactory = new LocationRemoteFactoryImpl();
        connectionRemoteFactory = new ConnectionRemoteFactoryImpl();
        this.locationRegistrySynchronizer = new RegistrySynchronizer<>(locationRegistry, locationRegistryRemote.getLocationConfigRemoteRegistry(), locationRemoteFactory);
        this.connectionRegistrySynchronizer = new RegistrySynchronizer<>(connectionRegistry, locationRegistryRemote.getConnectionConfigRemoteRegistry(), connectionRemoteFactory);
    }

    public void init() throws InitializationException, InterruptedException {
        init(LOCATION_MANAGER_ITEM_FILTER, new AbstractOpenHABRemote(hardwareSimulationMode) {

            @Override
            public void internalReceiveUpdate(OpenhabCommand command) throws CouldNotPerformException {
                logger.debug("Ignore update for location manager openhab binding.");
            }

            @Override
            public void internalReceiveCommand(OpenhabCommand command) throws CouldNotPerformException {
//                try {
//
//                    if (!command.hasOnOff() || !command.getOnOff().hasState()) {
//                        throw new CouldNotPerformException("Command does not have an onOff value required for scenes");
//                    }
//                    logger.debug("Received command for scene [" + command.getItem() + "] from openhab");
//                    registry.get(getSceneIdFromOpenHABItem(command)).setActivationState(ActivationStateTransformer.transform(command.getOnOff().getState()));
//                } catch (CouldNotPerformException ex) {
//                    throw new CouldNotPerformException("Skip item update [" + command.getItem() + " = " + command.getOnOff() + "]!", ex);
//                }
            }

        });
    }

    @Override
    public void init(String itemFilter, OpenHABRemote openHABRemote) throws InitializationException, InterruptedException {
        super.init(itemFilter, openHABRemote);
        try {
            locationRegistryRemote.init();
            locationRegistryRemote.activate();
            locationRegistrySynchronizer.init();
            connectionRegistrySynchronizer.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }
}
