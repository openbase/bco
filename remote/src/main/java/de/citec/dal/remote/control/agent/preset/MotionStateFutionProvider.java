package de.citec.dal.remote.control.agent.preset;

import de.citec.dal.hal.provider.MotionProvider;
import de.citec.dal.hal.service.ServiceType;
import de.citec.dal.remote.unit.MotionSensorRemote;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.iface.Activatable;
import de.citec.jul.pattern.Observable;
import de.citec.jul.schedule.Timeout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.service.ServiceConfigType;
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
public class MotionStateFutionProvider extends Observable<MotionState> implements MotionProvider, Activatable {

    /**
     * Default 3 minute window of no movement unit the state switches to NO_MOTION.
     */
    public static final long MOTION_TIMEOUT = 10000;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private MotionStateType.MotionState.Builder motionState;
    private final Timeout motionTimeout;
    private final List<MotionSensorRemote> motionSensorList;

    public MotionStateFutionProvider(Collection<UnitConfig> motionUnitConfigs) throws InstantiationException {
        this(motionUnitConfigs, MOTION_TIMEOUT);
    }

    public MotionStateFutionProvider(final Collection<UnitConfig> motionUnitConfigs, final long motionTimeout) throws InstantiationException {
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

    @Override
    public ServiceType getServiceType() {
        return ServiceType.MOTION;
    }

    @Override
    public ServiceConfigType.ServiceConfig getServiceConfig() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
