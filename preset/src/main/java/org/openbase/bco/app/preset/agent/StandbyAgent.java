package org.openbase.bco.app.preset.agent;

/*
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 - 2019 openbase.org
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

import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.control.layer.unit.agent.AbstractAgentController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ShutdownInProgressException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.schedule.Timeout;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.PresenceStateType;
import org.openbase.type.domotic.state.StandbyStateType.StandbyState.State;
import org.openbase.type.domotic.unit.location.LocationDataType;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;

import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class StandbyAgent extends AbstractAgentController {

    /**
     * 15 min default standby timeout
     */
    public static final long TIMEOUT = 60000 * 15;

    private LocationRemote locationRemote;
    private final Timeout timeout;
    private final SyncObject standbySync = new SyncObject("StandbySync");
    private final Observer<DataProvider<LocationData>, LocationData> locationDataObserver;


    public StandbyAgent() throws CouldNotPerformException {
        this.timeout = new Timeout(TIMEOUT) {

            @Override
            public void expired() {
                try {
                    locationRemote.setStandbyState(State.STANDBY).get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        };

        this.locationDataObserver = (source, data) -> triggerPresenceChange(data);
    }

    @Override
    protected ActionDescription execute(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);
        locationRemote.addDataObserver(locationDataObserver);
        if(locationRemote.isDataAvailable()) {
            triggerPresenceChange(locationRemote.getData());
        }
        return activationState.getResponsibleAction();
    }

    @Override
    protected void stop(final ActivationState activationState) throws InterruptedException, CouldNotPerformException {
        if (locationRemote != null) {
            locationRemote.removeDataObserver(locationDataObserver);
            locationRemote = null;
        }
        timeout.cancel();
        super.stop(activationState);
    }

    public void triggerPresenceChange(LocationDataType.LocationData data) throws InterruptedException {
        synchronized (standbySync) {
            if (data.getPresenceState().getValue().equals(PresenceStateType.PresenceState.State.PRESENT) && timeout.isActive()) {
                timeout.cancel();
                try {
                    locationRemote.setStandbyState(State.RUNNING).get();
                } catch (ExecutionException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not notify motion state change!", ex), logger);
                }
            } else if (data.getPresenceState().getValue().equals(PresenceStateType.PresenceState.State.ABSENT) && !timeout.isActive()) {
                try {
                    timeout.start();
                } catch (ShutdownInProgressException ex) {
                    // ignore change
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not schedule presence timeout!", ex), logger);
                }
            }
        }
    }
}
