package org.openbase.bco.manager.agent.core.preset;

/*
 * #%L
 * BCO Manager Agent Core
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import rst.domotic.state.TemperatureStateType;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.state.WindowStateType;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.connection.ConnectionConfigType;
import rst.domotic.unit.connection.ConnectionDataType;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class HeaterEnergySavingAgent extends AbstractAgentController {

    private LocationRemote locationRemote;
    private Future<Void> setTemperatureFuture;
    private final Observer<ConnectionDataType.ConnectionData> connectionObserver;
    private boolean regulated = false;
    private Map<UnitConfigType.UnitConfig, TemperatureStateType.TemperatureState> previousTemperatureState;

    public HeaterEnergySavingAgent() throws InstantiationException {
        super(HeaterEnergySavingAgent.class);

        connectionObserver = (final Observable<ConnectionDataType.ConnectionData> source, ConnectionDataType.ConnectionData data) -> {
            if (data.getWindowState().getValue() == WindowStateType.WindowState.State.OPEN) {
                if (!regulated) {
                    regulateHeater();
                }
            } else if (setTemperatureFuture != null && regulated) {
                setTemperatureFuture.cancel(true);
                setTemperatureFuture.get();
                restoreTemperatureState();
            }
        };
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getConfig().getLabel() + "]");
        previousTemperatureState = new HashMap<>();
        locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), true, Units.LOCATION);

        /** Add trigger here and replace dataObserver */
        for (ConnectionRemote connectionRemote : locationRemote.getConnectionList(true)) {
            if (connectionRemote.getConfig().getConnectionConfig().getType().equals(ConnectionConfigType.ConnectionConfig.ConnectionType.WINDOW)) {
                connectionRemote.addDataObserver(connectionObserver);
            }
        }
        locationRemote.waitForData();
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getConfig().getLabel() + "]");
        for (ConnectionRemote connectionRemote : locationRemote.getConnectionList(true)) {
            if (connectionRemote.getConfig().getConnectionConfig().getType().equals(ConnectionConfigType.ConnectionConfig.ConnectionType.WINDOW)) {
                connectionRemote.removeDataObserver(connectionObserver);
            }
        }
    }

    private void regulateHeater() {

        try {
            for (UnitConfigType.UnitConfig temperatureControllerConfig : Registries.getLocationRegistry().getUnitConfigsByLocationLabel(UnitTemplateType.UnitTemplate.UnitType.TEMPERATURE_CONTROLLER, locationRemote.getLabel())) {
                try {
                    previousTemperatureState.put(temperatureControllerConfig, Units.getUnit(temperatureControllerConfig, true, Units.TEMPERATURE_CONTROLLER).getTargetTemperatureState());
                } catch (NotAvailableException | InterruptedException ex) {
                    logger.error("Could not set targetTemperatureState of [ " + temperatureControllerConfig.getId() + "]");
                }
            }
        } catch (CouldNotPerformException | InterruptedException ex) {
            logger.error("Could not get TemperatureControllerConfigs.");
        }

        try {
            locationRemote.setTargetTemperatureState(TemperatureState.newBuilder().setTemperature(13.0).build());
            regulated = true;
        } catch (CouldNotPerformException ex) {
            logger.error("Could not set targetTemperatureState.");
        }
    }

    private void restoreTemperatureState() {
        if (!previousTemperatureState.isEmpty()) {
            for (Map.Entry<UnitConfigType.UnitConfig, TemperatureStateType.TemperatureState> entry : previousTemperatureState.entrySet()) {
                try {
                    Units.getUnit(entry.getKey(), true, Units.TEMPERATURE_CONTROLLER).setTargetTemperatureState(entry.getValue());
                } catch (InterruptedException | CouldNotPerformException ex) {
                    logger.error("Could not set targetTemperatureState of [ " + entry.getKey().getId() + "]");
                }
            }
        }

        regulated = false;
    }
}
