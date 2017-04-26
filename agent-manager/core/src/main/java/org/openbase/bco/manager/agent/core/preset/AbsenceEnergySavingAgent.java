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

import com.google.protobuf.GeneratedMessage;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.Observable;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.location.LocationDataType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.LightRemote;
import org.openbase.bco.dal.remote.unit.Units;
import static rst.domotic.state.PresenceStateType.PresenceState.State.PRESENT;


/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class AbsenceEnergySavingAgent extends AbstractAgentController {

    private LocationRemote locationRemote;
    private boolean present = true;
    private Future<Void> setLightPowerStateFuture;
    private Future<Void> setMultimediaPowerStateFuture;

    public AbsenceEnergySavingAgent() throws InstantiationException {
        super(AbsenceEnergySavingAgent.class);
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getConfig().getLabel() + "]");
        locationRemote = new LocationRemote();
        Registries.getLocationRegistry().waitForData();
        locationRemote.init(Registries.getLocationRegistry().getLocationConfigById(getConfig().getId()));

        /** Add trigger here and replace dataObserver */
        locationRemote.addDataObserver((Observable<LocationDataType.LocationData> source, LocationDataType.LocationData data) -> {
            if (data.getPresenceState().getValue() == PRESENT && !present) {
                if (setLightPowerStateFuture != null && !setLightPowerStateFuture.isDone()) {
                    setLightPowerStateFuture.cancel(true);
                }
                if (setMultimediaPowerStateFuture != null && !setMultimediaPowerStateFuture.isDone()) {
                    setMultimediaPowerStateFuture.cancel(true);
                }
                present = true;
            } else if (!(data.getPresenceState().getValue() == PRESENT) && present) {
                present = false;
                
                switchlightsOff();
            }
        });
        locationRemote.activate();
        super.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getClass().getSimpleName() + "]");
        locationRemote.deactivate();
        super.deactivate();
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        locationRemote.activate();
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        locationRemote.deactivate();
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
        try {
            UnitRemote<? extends GeneratedMessage> multimediaGroup = Units.getUnitByLabel(locationRemote.getLabel().concat("MultimediaGroup"), true);
            setMultimediaPowerStateFuture = ((LightRemote) multimediaGroup).setPowerState(PowerState.newBuilder().setValue(PowerState.State.OFF).build());
            // TODO: get correct Type of Remote for Group.
        } catch (CouldNotPerformException | InterruptedException ex) {
            logger.error("Could not set Powerstate of MultimediaGroup.");
            // TODO: Propper Ex handling
        } 
    }
}
