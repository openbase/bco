package org.openbase.bco.dal.remote.detector;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotInitializedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.MotionStateType.MotionState;
import rst.domotic.state.MotionStateType.MotionStateOrBuilder;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.PresenceStateType.PresenceStateOrBuilder;
import rst.domotic.unit.location.LocationDataType.LocationData;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PresenceDetector implements Manageable<DataProvider<LocationData>>, DataProvider<PresenceState> {

    /**
     * Default 3 minute window of no movement unit the state switches to
     * NO_MOTION.
     */
    public static final long PRESENCE_TIMEOUT = JPService.testMode() ? 50 : 60000 * 1;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final PresenceState.Builder presenceState;
    private final Timeout presenceTimeout;
    private final Observer<LocationData> locationDataObserver;
    private DataProvider<LocationData> locationDataProvider;
    private final ObservableImpl<PresenceState> presenceStateObservable;
    private boolean active;

    public PresenceDetector() {
        this.presenceState = PresenceState.newBuilder();
        this.active = false;
        this.presenceStateObservable = new ObservableImpl<>();
        this.presenceTimeout = new Timeout(PRESENCE_TIMEOUT) {

            @Override
            public void expired() {
                try {
                    // if motion is still detected just restart the timeout.
                    if (locationDataProvider.getData().getMotionState().getValue() == MotionState.State.MOTION) {
                        GlobalCachedExecutorService.submit(() -> {
                            try {
                                presenceTimeout.restart();
                            } catch (final CouldNotPerformException ex) {
                                ExceptionPrinter.printHistory("Could not setup presence timeout!", ex, logger);
                            }
                        });
                        return;
                    }
                    updatePresenceState(PresenceState.newBuilder().setValue(PresenceState.State.ABSENT));
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not notify absent by timer!", ex), logger);
                }
            }
        };

        locationDataObserver = (Observable<LocationData> source, LocationData data) -> {
            updateMotionState(data.getMotionState());
        };
    }

    @Override
    public void init(final DataProvider<LocationData> locationDataProvider) throws InitializationException, InterruptedException {
        this.locationDataProvider = locationDataProvider;
    }

    public void init(final DataProvider<LocationData> locationDataProvider, final long motionTimeout) throws InitializationException, InterruptedException {
        init(locationDataProvider);
        presenceTimeout.setDefaultWaitTime(motionTimeout);
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {

        if (locationDataObserver == null) {
            throw new NotInitializedException(this);
        }
        active = true;
        locationDataProvider.addDataObserver(locationDataObserver);

        // start initial timeout
        presenceTimeout.start();
        updateMotionState(locationDataProvider.getData().getMotionState());
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;
        presenceTimeout.cancel();
        if (locationDataProvider != null) {
            // can be null if never initialized or initialization failed
            locationDataProvider.removeDataObserver(locationDataObserver);
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void shutdown() {
        try {
            deactivate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }

    private synchronized void updatePresenceState(final PresenceStateOrBuilder presenceState) throws CouldNotPerformException {
        // TODO?
        // so wird das timeout durch das erste present setzten des detectors selbst nochmal restarted...
        // vorher nach motion state filtern (inklusive lastMotion) und wenn gleich das presence update skippen?
        // update Timestemp and reset timer
        if (presenceState.getValue() == PresenceState.State.PRESENT && this.presenceState.getLastPresence() != presenceState.getLastPresence()) {
            presenceTimeout.restart();
            this.presenceState.getLastPresenceBuilder().setTime(Math.max(this.presenceState.getLastPresence().getTime(), presenceState.getLastPresence().getTime()));
        }

        // filter non state changes
        if (this.presenceState.getValue() == presenceState.getValue()) {
            return;
        }

        // update value
        TimestampProcessor.updateTimestampWithCurrentTime(presenceState, logger);
        this.presenceState.setValue(presenceState.getValue());

        // notify
        try {
            presenceStateObservable.notifyObservers(this.presenceState.build());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update MotionState!", ex), logger, LogLevel.ERROR);
        }
    }

    private synchronized void updateMotionState(final MotionStateOrBuilder motionState) throws CouldNotPerformException {

        // Filter rush motion predictions.
        if (motionState.getValue() == MotionState.State.NO_MOTION) {
            return;
        }

        if (motionState.getValue() == MotionState.State.MOTION) {
            updatePresenceState(PresenceState.newBuilder().setValue(PresenceState.State.PRESENT).setLastPresence(motionState.getLastMotion()));
        }
    }

    @Override
    public boolean isDataAvailable() {
        return presenceStateObservable.isValueAvailable();
    }

    @Override
    public Class<PresenceState> getDataClass() {
        return PresenceState.class;
    }

    @Override
    public PresenceState getData() throws NotAvailableException {
        return presenceStateObservable.getValue();
    }

    @Override
    public CompletableFuture<PresenceState> getDataFuture() {
        return presenceStateObservable.getValueFuture();
    }

    @Override
    public void addDataObserver(final Observer<PresenceState> observer) {
        presenceStateObservable.addObserver(observer);
    }

    @Override
    public void removeDataObserver(final Observer<PresenceState> observer) {
        presenceStateObservable.removeObserver(observer);
    }

    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        presenceStateObservable.waitForValue();
    }

    @Override
    public void waitForData(final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        presenceStateObservable.waitForValue(timeout, timeUnit);
    }
}
