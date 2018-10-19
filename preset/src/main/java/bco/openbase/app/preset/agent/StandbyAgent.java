package bco.openbase.app.preset.agent;

/*
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 openbase.org
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
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.schedule.Timeout;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.PresenceStateType;
import rst.domotic.state.StandbyStateType.StandbyState.State;
import rst.domotic.unit.location.LocationDataType;
import rst.domotic.unit.location.LocationDataType.LocationData;

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
        super(StandbyAgent.class);
        this.timeout = new Timeout(TIMEOUT) {

            @Override
            public void expired() {
                try {
                    locationRemote.setStandbyState(State.STANDBY);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        };

        this.locationDataObserver = (source, data) -> triggerPresenceChange(data);
    }

    @Override
    protected void execute(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);
        locationRemote.addDataObserver(locationDataObserver);
//        locationRemote.waitForData();
        if(locationRemote.isDataAvailable()) {
            triggerPresenceChange(locationRemote.getData());
        }
    }

    @Override
    protected void stop(final ActivationState activationState) {
        if (locationRemote != null) {
            locationRemote.removeDataObserver(locationDataObserver);
            locationRemote = null;
        }
        timeout.cancel();
    }

    public void triggerPresenceChange(LocationDataType.LocationData data) throws InterruptedException {
        synchronized (standbySync) {
            if (data.getPresenceState().getValue().equals(PresenceStateType.PresenceState.State.PRESENT) && timeout.isActive()) {
                timeout.cancel();
                try {
                    locationRemote.setStandbyState(State.RUNNING);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not notify motion state change!", ex), logger);
                }
            } else if (data.getPresenceState().getValue().equals(PresenceStateType.PresenceState.State.ABSENT) && !timeout.isActive()) {
                try {
                    timeout.start();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not schedule presence timeout!", ex), logger);
                }
            }
        }
    }
}
