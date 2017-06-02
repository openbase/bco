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
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.DoorStateType;
import rst.domotic.state.PresenceStateType;
import rst.domotic.state.WindowStateType;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.connection.ConnectionConfigType.ConnectionConfig.ConnectionType;
import rst.domotic.unit.connection.ConnectionDataType.ConnectionData;
import rst.domotic.unit.location.LocationDataType;
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
    private final Observer<ConnectionData> connectionObserver;
    private final Observer<LocationDataType.LocationData> neighborObserver;
    private final Observer<LocationDataType.LocationData> passageNeighborObserver;

    public NearFieldLightAgent() throws InstantiationException {
        super(NearFieldLightAgent.class);
        
        connectionObserver = (final Observable<ConnectionData> source, ConnectionData data) -> {
            if ((data.getDoorState().getValue() == DoorStateType.DoorState.State.OPEN || 
                    data.getWindowState().getValue() == WindowStateType.WindowState.State.OPEN)) {
                if (presenceInOpenLocation()) {
                    dimmLights();
                }
            } else if (!presenceInOpenLocation() && setBrightnessStateFuture != null) {
                setBrightnessStateFuture.cancel(true);
                isDimmed = false;
            }
        };
        
        neighborObserver = (final Observable<LocationData> source, LocationData data) -> {
            if (data.getPresenceState().getValue().equals(PresenceStateType.PresenceState.State.PRESENT)) {
                if (presenceInOpenLocation()) {
                    dimmLights();
                }
//                for (ConnectionRemote relatedConnection : locationRemote.getRelatedConnectionRemoteList(neigborRemote.getId(), true)) {
//                    if (relatedConnection.getDoorState().getValue() == DoorStateType.DoorState.State.OPEN || 
//                            relatedConnection.getWindowState().getValue() == WindowStateType.WindowState.State.OPEN) {
//                        dimmLights();
//                        return;
//                    }
//                }                                
            } else if (!presenceInOpenLocation() && setBrightnessStateFuture != null) {
                setBrightnessStateFuture.cancel(true);
                isDimmed = false;
            }
        };
        
        passageNeighborObserver = (final Observable<LocationData> source, LocationData data) -> {
            if (data.getPresenceState().getValue().equals(PresenceStateType.PresenceState.State.PRESENT)) {
                dimmLights();                         
            } else if (!presenceInOpenLocation() && setBrightnessStateFuture != null) {
                setBrightnessStateFuture.cancel(true);
                isDimmed = false;
            }
        };
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        try {
            super.init(config);
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), true, Units.LOCATION);
            connectionRemotes = locationRemote.getConnectionList(true);
            neighborRemotes = locationRemote.getNeighborLocationList(true);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        } 
    }
    
    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getConfig().getLabel() + "]");
        
        for (ConnectionRemote connectionRemote : connectionRemotes) {                   
            if (connectionRemote.getConfig().getConnectionConfig().getType().equals(ConnectionType.WINDOW) || 
                    connectionRemote.getConfig().getConnectionConfig().getType().equals(ConnectionType.DOOR)) {
                connectionRemote.addDataObserver(connectionObserver);
            }
        }
        
        for (LocationRemote neigborRemote : neighborRemotes) {
            boolean passage = false;
            for (ConnectionRemote relatedConnection : locationRemote.getRelatedConnectionRemoteList(neigborRemote.getId(), true)) {
                if (relatedConnection.getConfig().getConnectionConfig().getType().equals(ConnectionType.PASSAGE)) {
                    passage = true;
                }
            } 
            
            if (passage) {
                neigborRemote.addDataObserver(passageNeighborObserver);
            } else {
                neigborRemote.addDataObserver(neighborObserver);
            }
        }
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getConfig().getLabel() + "]");
        // TODO: How to deactivate all the listeners?
        for (ConnectionRemote connectionRemote : connectionRemotes) {                   
            if (connectionRemote.getConfig().getConnectionConfig().getType().equals(ConnectionType.WINDOW) || 
                    connectionRemote.getConfig().getConnectionConfig().getType().equals(ConnectionType.DOOR)) {
                connectionRemote.removeDataObserver(connectionObserver);
            }
        }
        
        for (LocationRemote neigborRemote : neighborRemotes) {
            boolean passage = false;
            for (ConnectionRemote relatedConnection : locationRemote.getRelatedConnectionRemoteList(neigborRemote.getId(), true)) {
                if (relatedConnection.getConfig().getConnectionConfig().getType().equals(ConnectionType.PASSAGE)) {
                    passage = true;
                }
            } 
            
            if (passage) {
                neigborRemote.removeDataObserver(passageNeighborObserver);
            } else {
                neigborRemote.removeDataObserver(neighborObserver);
            }
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
                LocationRemote location = Units.getUnit(unitID, true, Units.LOCATION);
                if (location.getPresenceState().getValue().equals(PresenceStateType.PresenceState.State.PRESENT)) {
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
