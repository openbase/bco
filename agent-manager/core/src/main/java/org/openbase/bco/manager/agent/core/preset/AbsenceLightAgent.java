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
import java.util.logging.Level;
import java.util.logging.Logger;
import static rst.domotic.state.PresenceStateType.PresenceState.State.PRESENT;


/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class AbsenceLightAgent extends AbstractAgentController {

    private LocationRemote locationRemote;
    private boolean present = true;
    private Future<Void> setPowerStateFuture;

    public AbsenceLightAgent() throws InstantiationException {
        super(AbsenceLightAgent.class);
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
                if (setPowerStateFuture != null) {
                    setPowerStateFuture.cancel(true);
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
            setPowerStateFuture = locationRemote.setPowerState(PowerState.newBuilder().setValue(PowerState.State.OFF).build(), UnitType.LIGHT); 
            // TODO: Blocking setPowerState function that is trying to realloc all lights as long as jobs not cancelled. 
            // TODO: Add delay for 
        } catch (CouldNotPerformException ex) {
            Logger.getLogger(PresenceLightAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
