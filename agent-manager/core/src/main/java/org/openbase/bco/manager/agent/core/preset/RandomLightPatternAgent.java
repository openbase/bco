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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PresenceStateType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationDataType;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class RandomLightPatternAgent extends AbstractAgentController {

    private LocationRemote locationRemote;
    private boolean present = false;
    private Thread thread;
    private final Observer<LocationDataType.LocationData> locationObserver;

    public RandomLightPatternAgent() throws InstantiationException {
        super(RandomLightPatternAgent.class);

        locationObserver = (final Observable<LocationDataType.LocationData> source, LocationDataType.LocationData data) -> {
            if (data.getPresenceState().getValue().equals(PresenceStateType.PresenceState.State.PRESENT) && !present) {
                stopRandomLightPattern();
                present = true;
            } else if (!(data.getPresenceState().getValue().equals(PresenceStateType.PresenceState.State.PRESENT)) && present) {
                present = false;
                makeRandomLightPattern();
            }
        };
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getConfig().getLabel() + "]");
        locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), true, Units.LOCATION);

        locationRemote.addDataObserver(locationObserver);
        locationRemote.waitForData();
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getClass().getSimpleName() + "]");
        if (thread != null) {
            thread.interrupt();
        }
        locationRemote.removeDataObserver(locationObserver);
    }

    private void makeRandomLightPattern() throws CouldNotPerformException {
        thread = new PersonSimulator();
        thread.start();
    }

    private void stopRandomLightPattern() throws CouldNotPerformException {
        thread.interrupt();
    }

    private class PersonSimulator extends Thread {

        private Future<Void> setPowerStateOn;
        private Future<Void> setPowerStateOff;

        @Override
        public void run() {
            try {
                List<LocationRemote> childLocationList = locationRemote.getChildLocationList(true);
                LocationRemote currentLocation = childLocationList.get(ThreadLocalRandom.current().nextInt(childLocationList.size()));

                while (true) {
                    setPowerStateOn = currentLocation.setPowerState(PowerState.newBuilder().setValue(PowerState.State.ON).build(), UnitType.LIGHT);
                    setPowerStateOn.get();
                    // Todo: Assign time interval how long lights should be switched on.
                    setPowerStateOff = currentLocation.setPowerState(PowerState.newBuilder().setValue(PowerState.State.OFF).build(), UnitType.LIGHT);
                    setPowerStateOff.get();

                    List<LocationRemote> neighborLocationList = currentLocation.getNeighborLocationList(true);
                    currentLocation = neighborLocationList.get(ThreadLocalRandom.current().nextInt(neighborLocationList.size()));
                }
            } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
                Logger.getLogger(RandomLightPatternAgent.class.getName()).log(Level.SEVERE, null, ex);
                interrupt();
            }
        }

        @Override
        public void interrupt() {
            setPowerStateOn.cancel(true);
            setPowerStateOff.cancel(true);

            super.interrupt();
        }
    }
}
