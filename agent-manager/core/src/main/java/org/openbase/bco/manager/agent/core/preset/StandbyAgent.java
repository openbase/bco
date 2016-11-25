package org.openbase.bco.manager.agent.core.preset;

/*
 * #%L
 * COMA AgentManager Core
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import org.openbase.bco.dal.lib.detector.PresenseDetector;
import org.openbase.bco.manager.agent.core.AbstractAgent;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.schedule.Timeout;
import rst.domotic.action.SnapshotType.Snapshot;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class StandbyAgent extends AbstractAgent {

    public static final long TIEMOUT = 900000;
    private LocationRemote locationRemote;
    private PresenseDetector presenseDetector;
    private final Timeout timeout;
    private final SyncObject standbySync = new SyncObject("StandbySync");
    private boolean standby;

    private Snapshot snapshot;

    public StandbyAgent() throws InstantiationException, CouldNotPerformException, InterruptedException {
        super();

        this.standby = false;

        this.timeout = new Timeout(TIEMOUT) {

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
        locationRemote.init(CachedLocationRegistryRemote.getRegistry().getLocationConfigById(getConfig().getId()));
        locationRemote.activate();

        this.presenseDetector = new PresenseDetector();
//        presenseDetector.init(locationRemote);

//        this.presenseDetector.addObserver((Observable<MotionState> source, MotionState data) -> {
//            if (data.getValue().equals(MotionState.State.MOTION)) {
//                timeout.restart();
//                synchronized (standbySync) {
//                    if (standby) {
//                        try {
//                            wakeUp();
//                        } catch (CouldNotPerformException ex) {
//                            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not notify motion state change!", ex), logger);
//                        }
//E
//                    }
//                }
//            }
//        });
        super.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getClass().getSimpleName() + "]");
        locationRemote.deactivate();
//        presenseDetector.deactivate();
        super.deactivate();

    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
//        presenseDetector.activate();
        locationRemote.activate();
        timeout.start();
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        timeout.cancel();
//        presenseDetector.deactivate();
        locationRemote.deactivate();
    }

    private void standby() throws CouldNotPerformException, InterruptedException {
        synchronized (standbySync) {
            try {
                if (snapshot == null) {
                    return;
                }
                snapshot = locationRemote.recordSnapshot().get();
                standby = true;
            } catch (ExecutionException | CouldNotPerformException ex) {
                throw new CouldNotPerformException("Standby failed!", ex);
            }
        }
    }

    private void wakeUp() throws CouldNotPerformException, InterruptedException {
        synchronized (standbySync) {
            try {
                locationRemote.restoreSnapshot(snapshot).get();
                snapshot = null;
                standby = false;
            } catch (ExecutionException | CouldNotPerformException ex) {
                throw new CouldNotPerformException("WakeUp failed!", ex);
            }
        }
    }
}
