package org.openbase.bco.dal.remote.detector;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.openbase.bco.dal.lib.layer.unit.location.Location;
import org.openbase.bco.dal.remote.layer.unit.CustomUnitPool;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.Timeout;
import org.openbase.type.domotic.state.ButtonStateType.ButtonState;
import org.openbase.type.domotic.state.ButtonStateType.ButtonStateOrBuilder;
import org.openbase.type.domotic.state.DoorStateType.DoorState;
import org.openbase.type.domotic.state.WindowStateType.WindowState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.connection.ConnectionConfigType.ConnectionConfig.ConnectionType;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import org.openbase.type.domotic.unit.location.TileConfigType.TileConfig.TileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.state.MotionStateType.MotionState;
import org.openbase.type.domotic.state.MotionStateType.MotionStateOrBuilder;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState.State;
import org.openbase.type.domotic.state.PresenceStateType.PresenceStateOrBuilder;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PresenceDetector implements Manageable<Location>, DataProvider<PresenceState> {

    /**
     * Default 3 minute window of no movement unit the state switches to
     * NO_MOTION.
     */
    public static final long PRESENCE_TIMEOUT = JPService.testMode() ? 50 : 60000;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final PresenceState.Builder presenceStateBuilder;
    private final Timeout presenceTimeout;
    private final Observer<DataProvider<LocationData>, LocationData> locationDataObserver;
    private Location location;
    private final ObservableImpl<DataProvider<PresenceState>, PresenceState> presenceStateObservable;
    private final CustomUnitPool buttonUnitPool;
    private final CustomUnitPool connectionUnitPool;

    private boolean active;

    public PresenceDetector() throws InstantiationException {
        try {
            this.presenceStateBuilder = PresenceState.newBuilder();
            this.active = false;
            this.presenceStateObservable = new ObservableImpl<>(this);
            this.presenceTimeout = new Timeout(PRESENCE_TIMEOUT) {

                @Override
                public void expired() {
                    try {
                        // if motion is still detected just restart the timeout.
                        if (location.getData().getMotionState().getValue() == MotionState.State.MOTION) {
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
                    } catch (ShutdownInProgressException ex) {
                        // skip update on shutdown
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not notify absent by timer!", ex), logger);
                    }
                }
            };

            locationDataObserver = (DataProvider<LocationData> source, LocationData data) -> {
                updateMotionState(data.getMotionState());
            };

            this.buttonUnitPool = new CustomUnitPool();
            this.connectionUnitPool = new CustomUnitPool();

            this.buttonUnitPool.addObserver((source, data) -> PresenceDetector.this.updateButtonState((ButtonState) data));

            this.connectionUnitPool.addObserver((source, data) -> {
                switch (source.getServiceType()) {
                    case WINDOW_STATE_SERVICE:
                        updateWindowState((WindowState) data);
                        break;
                    case DOOR_STATE_SERVICE:
                        updateDoorState((DoorState) data);
                        break;
                    case PASSAGE_STATE_SERVICE:
                        // just ignore passage states.
                        break;

                    default:
                        logger.warn("Invalid connection service update received: " + source.getServiceType().name() + " from " + source + " pool:" + connectionUnitPool.isActive());
                }
            });

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final Location location) throws InitializationException, InterruptedException {
        try {
            this.location = location;
            buttonUnitPool.init(
                    unitConfig -> unitConfig.getUnitType() != UnitType.BUTTON,
                    unitConfig -> {
                        try {
                            return !unitConfig.getPlacementConfig().getLocationId().equals(location.getId());
                        } catch (NotAvailableException ex) {
                            ExceptionPrinter.printHistory("Could not resolve location id within button filter operation.", ex, logger);
                            return true;
                        }
                    });


            if ((location.getConfig().getLocationConfig().getLocationType() == LocationType.TILE)) {
                connectionUnitPool.init(
                    unitConfig -> unitConfig.getUnitType() != UnitType.CONNECTION,
                    unitConfig -> {
                        try {
                            return !unitConfig.getConnectionConfig().getTileIdList().contains(location.getId());
                        } catch (NotAvailableException ex) {
                            ExceptionPrinter.printHistory("Could not resolve location id within connection filter operation.", ex, logger);
                            return true;
                        }
                    },
                    unitConfig -> {
                        try {
                            return location.getConfig().getLocationConfig().getTileConfig().getTileType() == TileType.OUTDOOR && unitConfig.getConnectionConfig().getConnectionType() == ConnectionType.WINDOW;
                        } catch (NotAvailableException ex) {
                            ExceptionPrinter.printHistory("Could not resolve location id within connection filter operation.", ex, logger);
                            return true;
                        }
                    });
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void init(final Location location, final long motionTimeout) throws InitializationException, InterruptedException {
        init(location);
        presenceTimeout.setDefaultWaitTime(motionTimeout);
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {

        if (locationDataObserver == null) {
            throw new NotInitializedException(this);
        }
        active = true;
        location.addDataObserver(locationDataObserver);

        buttonUnitPool.activate();

        if ((location.getConfig().getLocationConfig().getLocationType() == LocationType.TILE)) {
            connectionUnitPool.activate();
        }

        // start initial timeout
        presenceTimeout.start();
        updateMotionState(location.getData().getMotionState());
    }


    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;
        presenceTimeout.cancel();
        if (location != null) {
            // can be null if never initialized or initialization failed
            location.removeDataObserver(locationDataObserver);
        }
        buttonUnitPool.deactivate();
        if ((location.getConfig().getLocationConfig().getLocationType() == LocationType.TILE)) {
            connectionUnitPool.deactivate();
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
        buttonUnitPool.shutdown();
        connectionUnitPool.shutdown();
    }

    private synchronized void updatePresenceState(final PresenceStateOrBuilder presenceState) throws CouldNotPerformException {

        // update timestamp and reset timer
        if (presenceState.getValue() == PresenceState.State.PRESENT && presenceStateBuilder.getTimestamp().getTime() != presenceState.getTimestamp().getTime()) {
            presenceTimeout.restart();
            presenceStateBuilder.getTimestampBuilder().setTime(Math.max(presenceStateBuilder.getTimestamp().getTime(), presenceState.getTimestamp().getTime()));
        }

        // filter non state changes
        if (presenceStateBuilder.getValue() == presenceState.getValue()) {
            return;
        }

        // update value
        TimestampProcessor.updateTimestampWithCurrentTime(presenceStateBuilder, logger);
        presenceStateBuilder.setValue(presenceState.getValue());

        // notify
        try {
            presenceStateObservable.notifyObservers(this.presenceStateBuilder.build());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update MotionState!", ex), logger, LogLevel.ERROR);
        }
    }

    private void updateMotionState(final MotionStateOrBuilder motionState) throws CouldNotPerformException {

        // Filter rush motion predictions.
        if (motionState.getValue() == MotionState.State.NO_MOTION) {
            return;
        }

        if (motionState.getValue() == MotionState.State.MOTION) {
            updatePresenceState(TimestampProcessor.updateTimestampWithCurrentTime(PresenceState.newBuilder().setValue(State.PRESENT).setResponsibleAction(motionState.getResponsibleAction()).build()));
        }
    }

    private void updateButtonState(final ButtonStateOrBuilder buttonState) throws CouldNotPerformException {
        switch (buttonState.getValue()) {
            case PRESSED:
            case RELEASED:
            case DOUBLE_PRESSED:
                // note: currently disabled because some hardware setups offer a fix link between lights and buttons. Therefore, any light actions are resulting in a PRESENT state which then triggers further actions.
                // updatePresenceState(TimestampProcessor.updateTimestampWithCurrentTime(PresenceState.newBuilder().setValue(State.PRESENT).setResponsibleAction(buttonState.getResponsibleAction()).build()));
            case UNKNOWN:
            default:
                // ignore non presence prove
                return;
        }
    }

    private void updateDoorState(final DoorState doorState) throws CouldNotPerformException {
        switch (doorState.getValue()) {
            case OPEN:
                updatePresenceState(TimestampProcessor.updateTimestampWithCurrentTime(PresenceState.newBuilder().setValue(State.PRESENT).setResponsibleAction(doorState.getResponsibleAction()).build()));
            case CLOSED:
            case UNKNOWN:
            default:
                // ignore non presence prove
                return;
        }
    }

    private void updateWindowState(final WindowState windowState) throws CouldNotPerformException {
        switch (windowState.getValue()) {
            case OPEN:
            case TILTED:
            case CLOSED:
                updatePresenceState(TimestampProcessor.updateTimestampWithCurrentTime(PresenceState.newBuilder().setValue(State.PRESENT).setResponsibleAction(windowState.getResponsibleAction()).build()));
            case UNKNOWN:
            default:
                // ignore non presence prove
                return;
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
    public Future<PresenceState> getDataFuture() {
        return presenceStateObservable.getValueFuture();
    }

    @Override
    public void addDataObserver(final Observer<DataProvider<PresenceState>, PresenceState> observer) {
        presenceStateObservable.addObserver(observer);
    }

    @Override
    public void removeDataObserver(final Observer<DataProvider<PresenceState>, PresenceState> observer) {
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
