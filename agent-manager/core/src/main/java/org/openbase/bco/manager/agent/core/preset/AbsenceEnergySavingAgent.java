/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

import java.util.concurrent.Future;
import org.openbase.bco.dal.remote.unit.UnitGroupRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationDataType;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class AbsenceEnergySavingAgent extends AbstractAgentController {

    private LocationRemote locationRemote;
    private boolean present = true;
    private Future<Void> setLightPowerStateFuture;
    private Future<Void> setMultimediaPowerStateFuture;
    private final Observer<LocationDataType.LocationData> locationObserver;

    public AbsenceEnergySavingAgent() throws InstantiationException {
        super(AbsenceEnergySavingAgent.class);
        
        locationObserver = (final Observable<LocationDataType.LocationData> source, LocationDataType.LocationData data) -> {
            if (data.getPresenceState().getValue().equals(PresenceState.State.PRESENT) && !present) {
                if (setLightPowerStateFuture != null && !setLightPowerStateFuture.isDone()) {
                    setLightPowerStateFuture.cancel(true);
                }
                if (setMultimediaPowerStateFuture != null && !setMultimediaPowerStateFuture.isDone()) {
                    setMultimediaPowerStateFuture.cancel(true);
                }
                present = true;
            } else if (!(data.getPresenceState().getValue().equals(PresenceState.State.PRESENT)) && present) {
                present = false;
                
                switchlightsOff();
                switchMultimediaOff();
            }
        };
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getConfig().getLabel() + "]");
        locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);

        /** Add trigger here and replace dataObserver */
        locationRemote.addDataObserver(locationObserver);
        locationRemote.waitForData();
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getConfig().getLabel() + "]");
        locationRemote.removeDataObserver(locationObserver);
    }

    private void switchlightsOff() {                   
        try { 
            setLightPowerStateFuture = locationRemote.setPowerState(PowerState.newBuilder().setValue(PowerState.State.OFF).build(), UnitType.LIGHT); 
            // TODO: Blocking setPowerState function that is trying to realloc all lights as long as jobs not cancelled. 
        } catch (CouldNotPerformException ex) {
            logger.error("Could not set Powerstate of Lights.");
            // TODO: Propper Ex handling
        }
    }
    
    private void switchMultimediaOff() {   
        
        UnitGroupRemote multimediaGroup;
        try {
            multimediaGroup = Units.getUnitByLabel(locationRemote.getLabel().concat("MultimediaGroup"), true, Units.UNITGROUP);
            setMultimediaPowerStateFuture = multimediaGroup.setPowerState(PowerState.newBuilder().setValue(PowerState.State.OFF).build());
        } catch (NotAvailableException ex) {
            logger.info("MultimediaGroup not available.");
        } catch (InterruptedException ex) {
            logger.error("Could not get MultimediaGroup!");
        } catch (CouldNotPerformException ex) {
            logger.error("Could not set Powerstate of MultimediaGroup.");
        }
    }
}
