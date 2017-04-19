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

import java.util.List;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import rst.domotic.state.BrightnessStateType.BrightnessState;

import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openbase.bco.dal.remote.unit.Units;
import static org.openbase.bco.dal.remote.unit.Units.LOCATION;
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
import org.openbase.jul.pattern.Observable;
import rst.domotic.state.DoorStateType;
import static rst.domotic.state.PresenceStateType.PresenceState.State.PRESENT;
import rst.domotic.state.WindowStateType;
import static rst.domotic.unit.connection.ConnectionConfigType.ConnectionConfig.ConnectionType.PASSAGE;
import static rst.domotic.unit.connection.ConnectionConfigType.ConnectionConfig.ConnectionType.WINDOW;
import static rst.domotic.unit.connection.ConnectionConfigType.ConnectionConfig.ConnectionType.DOOR;
import rst.domotic.unit.connection.ConnectionDataType.ConnectionData;
import rst.domotic.unit.location.LocationDataType.LocationData;


/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class NearFieldLightAgent extends AbstractAgentController {

    private LocationRemote locationRemote;
    private Future<Void> setBrightnessStateFuture;
    private boolean isDimmed = false;
    private List<ConnectionRemote> connectionRemotes;
    private List<LocationRemote> neighborRemotes;

    public NearFieldLightAgent() throws InstantiationException {
        super(NearFieldLightAgent.class);
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getConfig().getLabel() + "]");
        locationRemote = new LocationRemote();
        Registries.getLocationRegistry().waitForData();
        locationRemote.init(Registries.getLocationRegistry().getLocationConfigById(getConfig().getId()));
        connectionRemotes = locationRemote.getConnectionList(true);
        neighborRemotes = locationRemote.getNeighborLocationList(true);
        
        for (ConnectionRemote connectionRemote : connectionRemotes) {                   
            if (connectionRemote.getConfig().getConnectionConfig().getType().equals(WINDOW) || 
                    connectionRemote.getConfig().getConnectionConfig().getType().equals(DOOR)) {
                connectionRemote.addDataObserver((Observable<ConnectionData> source, ConnectionData data) -> {
                        if ((data.getDoorState().getValue() == DoorStateType.DoorState.State.OPEN || 
                                data.getWindowState().getValue() == WindowStateType.WindowState.State.OPEN) &&
                                    presenceInLocations(connectionRemote.getConfig().getConnectionConfig().getTileIdList())) {
                            dimmLights();
                        } else if (!presenceInOpenLocation() && setBrightnessStateFuture != null) {
                            setBrightnessStateFuture.cancel(true);
                            isDimmed = false;
                        }
                    });
            }
        }
        
        for (LocationRemote neigborRemote : neighborRemotes) {
            boolean passage = false;
            for (ConnectionRemote relatedConnection : locationRemote.getRelatedConnectionRemoteList(neigborRemote.getId(), true)) {
                if (relatedConnection.getConfig().getConnectionConfig().getType().equals(PASSAGE)) {
                    passage = true;
                }
            } 
            
            if (passage) {
                neigborRemote.addDataObserver((Observable<LocationData> source, LocationData data) -> {
                    if (data.getPresenceState().getValue() == PRESENT) {
                        dimmLights();                         
                    } else if (!presenceInOpenLocation() && setBrightnessStateFuture != null) {
                        setBrightnessStateFuture.cancel(true);
                        isDimmed = false;
                    }
                });
            } else {
                neigborRemote.addDataObserver((Observable<LocationData> source, LocationData data) -> {
                    if (data.getPresenceState().getValue() == PRESENT) {
                        for (ConnectionRemote relatedConnection : locationRemote.getRelatedConnectionRemoteList(neigborRemote.getId(), true)) {
                            if (relatedConnection.getDoorState().getValue() == DoorStateType.DoorState.State.OPEN || 
                                    relatedConnection.getWindowState().getValue() == WindowStateType.WindowState.State.OPEN) {
                                dimmLights();
                                return;
                            }
                        }                                
                    } else if (!presenceInOpenLocation() && setBrightnessStateFuture != null) {
                        setBrightnessStateFuture.cancel(true);
                        isDimmed = false;
                    }
                });
            }
        }
             
        locationRemote.activate();
        for (LocationRemote neigborRemote : neighborRemotes) { 
            neigborRemote.activate();
        }
        for (ConnectionRemote connectionRemote : connectionRemotes) { 
            connectionRemote.activate();
        }
        super.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getClass().getSimpleName() + "]");
        // TODO: How to deactivate all the listeners?
        locationRemote.deactivate();
        for (LocationRemote neigborRemote : neighborRemotes) { 
            neigborRemote.deactivate();
        }
        for (ConnectionRemote connectionRemote : connectionRemotes) { 
            connectionRemote.deactivate();
        }
        super.deactivate();
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        locationRemote.activate();
        for (LocationRemote neigborRemote : neighborRemotes) { 
            neigborRemote.activate();
        }
        for (ConnectionRemote connectionRemote : connectionRemotes) { 
            connectionRemote.activate();
        }
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        locationRemote.deactivate();
        for (LocationRemote neigborRemote : neighborRemotes) { 
            neigborRemote.deactivate();
        }
        for (ConnectionRemote connectionRemote : connectionRemotes) { 
            connectionRemote.deactivate();
        }
    }
    
    private boolean presenceInOpenLocation() throws CouldNotPerformException {
        for (ConnectionRemote connectionRemote : locationRemote.getConnectionList(true)) {                   
            switch (connectionRemote.getConfig().getConnectionConfig().getType()){
                case WINDOW:
                case DOOR:
                    if (connectionRemote.getDoorState().getValue() == DoorStateType.DoorState.State.OPEN || 
                            connectionRemote.getWindowState().getValue() == WindowStateType.WindowState.State.OPEN) {
                        if (presenceInLocations(connectionRemote.getConfig().getConnectionConfig().getTileIdList())) {
                            return true;
                        }
                    }
                    break;
                case PASSAGE:
                    if (presenceInLocations(connectionRemote.getConfig().getConnectionConfig().getTileIdList())) {
                        return true;
                    }
                    break;
                case UNKNOWN:
                    break;
            }
        }
        return false;
    }
    
    private boolean presenceInLocations(List<String> unitIDs) throws CouldNotPerformException {
        for (String unitID : unitIDs) {
            try {
                if (unitID.equals(Registries.getLocationRegistry().getLocationConfigById(getConfig().getId()).getId())) {
                    continue;
                }
                LocationRemote location = Units.getUnit(unitID, true, LOCATION);
                if (location.getPresenceState().getValue() == PRESENT) {
                    return true;
                }
            } catch (InterruptedException ex) {
                throw new CouldNotPerformException("Could not get all connections!", ex);
            }
        }
        return false;
    }

    private void dimmLights() { 
        if (isDimmed) {
            return;
        }
        try { 
            // Blocking and trying to realloc all lights
            setBrightnessStateFuture = locationRemote.setBrightnessState(BrightnessState.newBuilder().setBrightness(0.5).build());
        } catch (CouldNotPerformException ex) {
            Logger.getLogger(PresenceLightAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        isDimmed = true;
    }
}
