package org.dc.bco.manager.agent.core.preset;

/*
 * #%L
 * COMA AgentManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.pattern.ObservableImpl;
import org.dc.jul.schedule.Timeout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.dc.bco.dal.lib.layer.service.provider.MotionProvider;
import org.dc.bco.dal.remote.unit.MotionSensorRemote;
import org.dc.bco.manager.location.remote.LocationRemote;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.iface.Manageable;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.MotionStateType;
import rst.homeautomation.state.MotionStateType.MotionState;
import rst.homeautomation.state.MotionStateType.MotionStateOrBuilder;
import rst.homeautomation.unit.MotionSensorType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;
import rst.spatial.LocationDataType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class PresenseDetector extends ObservableImpl<MotionState> implements MotionProvider, Manageable<LocationRemote> {

    /**
     * Default 3 minute window of no movement unit the state switches to NO_MOTION.
     */
    public static final long MOTION_TIMEOUT = 10000;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<MotionSensorRemote> motionSensorList;
    private MotionStateType.MotionState.Builder motionState;
    private Timeout motionTimeout;

    public PresenseDetector() {
        this.motionSensorList = new ArrayList<>();
    }

    @Override
    public void init(final LocationRemote locationRemote) throws InitializationException, InterruptedException {
        init(locationRemote, MOTION_TIMEOUT);
    }
    
    public void init(final LocationRemote locationRemote, final long motionTimeout) throws InitializationException, InterruptedException {
        this.motionState = MotionState.newBuilder();
        this.motionTimeout = new Timeout(motionTimeout) {

            @Override
            public void expired() {
                updateMotionState(MotionStateType.MotionState.newBuilder().setValue(MotionStateType.MotionState.State.NO_MOVEMENT));
            }
        };

        locationRemote.addObserver(new Observer<LocationDataType.LocationData>() {

            @Override
            public void update(Observable<LocationDataType.LocationData> source, LocationDataType.LocationData data) throws Exception {
                updateMotionState(data.getMotionState());
            }
        });
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        for (MotionSensorRemote remote : motionSensorList) {
            remote.activate();
        }
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        for (MotionSensorRemote remote : motionSensorList) {
            remote.deactivate();
        }
    }

    @Override
    public boolean isActive() {
        return motionSensorList.stream().noneMatch((remote) -> (!remote.isActive()));
    }

    @Override
    public void shutdown() {
        try {
            deactivate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
        super.shutdown();
    }

    private synchronized void updateMotionState(final MotionStateOrBuilder motionState) {

        // Filter rush motion predictions.
        if (motionState.getValue() == MotionStateType.MotionState.State.NO_MOVEMENT && !motionTimeout.isExpired()) {
            return;
        }

        // Update Timestemp and reset timer
        if (motionState.getValue() == MotionStateType.MotionState.State.MOVEMENT) {
            motionTimeout.restart();
            this.motionState.getLastMovementBuilder().setTime(Math.max(this.motionState.getLastMovement().getTime(), motionState.getLastMovement().getTime()));
        }

        // Filter dublicated state notification
        if (this.motionState.getValue() == motionState.getValue()) {
            return;
        }

        this.motionState.setValue(motionState.getValue());
        try {
            notifyObservers(this, this.motionState.build());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update MotionState!", ex), logger, LogLevel.ERROR);
        }
    }

    @Override
    public MotionState getMotion() throws CouldNotPerformException {
        return this.motionState.build();
    }
}
