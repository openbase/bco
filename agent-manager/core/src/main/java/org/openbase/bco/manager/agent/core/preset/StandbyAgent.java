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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openbase.bco.manager.agent.core.AbstractAgent;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.schedule.Timeout;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.state.PowerStateType;
import rst.domotic.state.PresenceStateType;
import rst.domotic.unit.location.LocationDataType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class StandbyAgent extends AbstractAgent {

    /**
     * 15 min default standby timeout
     */
    public static final long TIMEOUT = 60000 * 15;
//    public static final long TIMEOUT = 10000;

    private LocationRemote locationRemote;
    private final Timeout timeout;
    private final SyncObject standbySync = new SyncObject("StandbySync");
    private boolean standby;

    private Snapshot snapshot;

    public StandbyAgent() throws InstantiationException, CouldNotPerformException, InterruptedException {
        super();

        this.standby = false;

        this.timeout = new Timeout(TIMEOUT) {

            @Override
            public void expired() throws InterruptedException {
                try {
                    standby();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        };
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getConfig().getLabel() + "]");
        locationRemote = new LocationRemote();
        CachedLocationRegistryRemote.waitForData();
        locationRemote.init(CachedLocationRegistryRemote.getRegistry().getLocationConfigById(getConfig().getPlacementConfig().getLocationId()));
        locationRemote.addDataObserver((Observable<LocationDataType.LocationData> source, LocationDataType.LocationData data) -> {
            triggerPresenceChange(data);
        });
        super.activate();

    }

    public void triggerPresenceChange(LocationDataType.LocationData data) throws InterruptedException {
        System.out.println("trigger: " + data.getPresenceState().getValue().name());
        synchronized (standbySync) {
            if (data.getPresenceState().getValue().equals(PresenceStateType.PresenceState.State.PRESENT)) {
                timeout.cancel();
                if (standby) {
                    try {
                        wakeUp();
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not notify motion state change!", ex), logger);
                    }
                }
            } else if (data.getPresenceState().getValue().equals(PresenceStateType.PresenceState.State.ABSENT)) {
                if (!timeout.isActive()) {
                    System.out.println("timeout start");
                    timeout.start();
                }
            }
        }
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
        triggerPresenceChange(locationRemote.getData());
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        timeout.cancel();
        locationRemote.deactivate();
    }

    private void standby() throws CouldNotPerformException, InterruptedException {
        System.out.println("try to standby");
        synchronized (standbySync) {
            if (standby) {
                return;
            }
            logger.info("Standby " + locationRemote.getLabel() + "...");
            try {
                try {
                    logger.info("Create snapshot of " + locationRemote.getLabel() + " state.");
                    snapshot = locationRemote.recordSnapshot().get(10, TimeUnit.SECONDS);
                } catch (ExecutionException | CouldNotPerformException | TimeoutException ex) {
                    ExceptionPrinter.printHistory("Could not create snapshot!", ex, logger);
                }
                standby = true;
                logger.info("Switch off all devices...");
                locationRemote.setPowerState(PowerStateType.PowerState.State.OFF);
                logger.info(locationRemote.getLabel() + " is now standby.");
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Standby failed!", ex);
            } finally {
            }
        }
    }

    private void wakeUp() throws CouldNotPerformException, InterruptedException {
        logger.info("Wake up " + locationRemote.getLabel() + "...");
        synchronized (standbySync) {
            standby = false;

            if (snapshot == null) {
                logger.info("skip wake up because no snapshot information available!");
                return;
            }

            try {
                logger.info("restore snapshot up");
                locationRemote.restoreSnapshot(snapshot).get();
                snapshot = null;

            } catch (ExecutionException | CouldNotPerformException ex) {
                throw new CouldNotPerformException("WakeUp failed!", ex);
            }
        }
    }
}
