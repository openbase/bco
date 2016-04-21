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
import org.dc.jul.iface.Activatable;
import org.dc.jul.pattern.Observable;
import org.dc.jul.schedule.Timeout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.dc.bco.dal.lib.layer.service.provider.MotionProviderService;
import org.dc.bco.dal.remote.unit.MotionSensorRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.MotionStateType;
import rst.homeautomation.state.MotionStateType.MotionState;
import rst.homeautomation.state.MotionStateType.MotionStateOrBuilder;
import rst.homeautomation.unit.MotionSensorType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class MotionStateFutionProvider extends Observable<MotionState> implements MotionProviderService, Activatable {

    /**
     * Default 3 minute window of no movement unit the state switches to NO_MOTION.
     */
    public static final long MOTION_TIMEOUT = 10000;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private MotionStateType.MotionState.Builder motionState;
    private final Timeout motionTimeout;
    private final List<MotionSensorRemote> motionSensorList;

    public MotionStateFutionProvider(Collection<UnitConfig> motionUnitConfigs) throws InstantiationException, InterruptedException {
        this(motionUnitConfigs, MOTION_TIMEOUT);
    }

    public MotionStateFutionProvider(final Collection<UnitConfig> motionUnitConfigs, final long motionTimeout) throws InstantiationException, InterruptedException {
        try {
            this.motionSensorList = new ArrayList<>();
            this.motionState = MotionState.newBuilder();
            this.motionTimeout = new Timeout(motionTimeout) {

                @Override
                public void expired() {
                    updateMotionState(MotionStateType.MotionState.newBuilder().setValue(MotionStateType.MotionState.State.NO_MOVEMENT));
                }
            };

            MotionSensorRemote motionSensorRemote;
            for (UnitConfigType.UnitConfig unitConfig : motionUnitConfigs) {
                if (unitConfig.getType() != UnitTemplateType.UnitTemplate.UnitType.MOTION_SENSOR) {
                    logger.warn("Skip Unit[" + unitConfig.getId() + "] because its not of Type[" + UnitTemplateType.UnitTemplate.UnitType.MOTION_SENSOR + "]!");
                    continue;
                }

                motionSensorRemote = new MotionSensorRemote();
                motionSensorRemote.init(unitConfig);
                motionSensorList.add(motionSensorRemote);
                motionSensorRemote.addObserver((Observable<MotionSensorType.MotionSensor> source, MotionSensorType.MotionSensor data) -> {
                    updateMotionState(data.getMotionState());
                });

            }
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
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
