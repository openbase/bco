/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.agent.preset;

import de.citec.dal.remote.unit.DALRemoteService;
import de.citec.dal.remote.unit.MotionSensorRemote;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.schedule.Timeout;
import de.citec.lm.remote.LocationRegistryRemote;
import java.util.ArrayList;
import java.util.List;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.state.MotionStateType;
import rst.homeautomation.state.MotionStateType.MotionState;
import rst.homeautomation.state.MotionStateType.MotionStateOrBuilder;
import rst.homeautomation.unit.MotionSensorType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class PersonLightProviderAgent extends AbstractAgent {

    public static final double MINIMUM_LIGHT_THRESHOLD = 100;
    public static final String LOCATION_ID = "Küche";
    public static final long MOTION_TIMEOUT = 900;

    private MotionStateType.MotionState.Builder motionState;
    private final Timeout motionTimeout;

    public PersonLightProviderAgent(AgentConfig agentConfig) throws InstantiationException, CouldNotPerformException, InterruptedException {
        super(agentConfig);
        this.motionTimeout = new Timeout(MOTION_TIMEOUT) {

            @Override
            public void expired() {
                updateMotionState(MotionState.newBuilder().setValue(MotionState.State.NO_MOVEMENT));
            }
        };

//        DeviceRegistryRemote deviceRegistryRemote = new DeviceRegistryRemote();
//        deviceRegistryRemote.init();
//        deviceRegistryRemote.activate();
//        deviceRegistryRemote.shutdown();
        logger.info("Initializing observers");
//        initObserver();

        LocationRegistryRemote locationRegistryRemote = new LocationRegistryRemote();
        locationRegistryRemote.init();
        locationRegistryRemote.activate();

        List<MotionSensorRemote> motionSensorList = new ArrayList<>();
        MotionSensorRemote motionSensorRemote;

        for (UnitConfig unitConfig : locationRegistryRemote.getUnitConfigs(UnitTemplateType.UnitTemplate.UnitType.MOTION_SENSOR, LOCATION_ID)) {
            motionSensorRemote = new MotionSensorRemote();
            motionSensorRemote.init(unitConfig);
            motionSensorList.add(motionSensorRemote);
            motionSensorRemote.addObserver((Observable<MotionSensorType.MotionSensor> source, MotionSensorType.MotionSensor data) -> {
                updateMotionState(data.getMotionState());
            });
        }
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
        notifyMotionStateChanged(motionState);
    }

    private void notifyMotionStateChanged(final MotionStateOrBuilder motionState) {
        if (motionState.getValue() == MotionState.State.MOVEMENT) {

        }
    }

//
//    private void initObserver() {
//        sourceRemote.addObserver(new Observer<GeneratedMessage>() {
//
//            @Override
//            public void update(Observable<GeneratedMessage> source, GeneratedMessage data) throws Exception {
//                sourceLatestPowerState = invokeGetPowerState(data).getValue();
//                logger.info("Recieved new value ["+sourceLatestPowerState+"] for source");
//                if (sourceLatestPowerState == PowerState.State.OFF) {
//                    if (targetLatestPowerState != PowerState.State.OFF) {
//                        invokeSetPower(targetRemote, PowerState.State.OFF);
//                    }
//                } else if (sourceLatestPowerState == PowerState.State.ON) {
//                    switch (targetBehaviour) {
//                        case OFF:
//                            if (targetLatestPowerState != PowerState.State.OFF) {
//                                invokeSetPower(targetRemote, PowerState.State.OFF);
//                            }
//                            break;
//                        case ON:
//                            if (targetLatestPowerState != PowerState.State.ON) {
//                                invokeSetPower(targetRemote, PowerState.State.ON);
//                            }
//                            break;
//                        case LAST_STATE:
//                            break;
//                    }
//                }
//            }
//        });
//
//        targetRemote.addObserver(new Observer<GeneratedMessage>() {
//
//            @Override
//            public void update(Observable<GeneratedMessage> source, GeneratedMessage data) throws Exception {
//                targetLatestPowerState = invokeGetPowerState(data).getValue();
//                logger.info("Recieved new value ["+targetLatestPowerState+"] for target");
//                if (targetLatestPowerState == PowerState.State.ON) {
//                    if (sourceLatestPowerState != PowerState.State.ON) {
//                        invokeSetPower(sourceRemote, PowerState.State.ON);
//                    }
//                } else if (targetLatestPowerState == PowerState.State.OFF) {
//                    switch (sourceBehaviour) {
//                        case OFF:
//                            if (sourceLatestPowerState != PowerState.State.OFF) {
//                                invokeSetPower(sourceRemote, PowerState.State.OFF);
//                            }
//                            break;
//                        case ON:
//                            if (sourceLatestPowerState != PowerState.State.ON) {
//                                invokeSetPower(sourceRemote, PowerState.State.ON);
//                            }
//                            break;
//                        case LAST_STATE:
//                            break;
//                    }
//                }
//            }
//        });
//    }
//
//    private void invokeSetPower(DALRemoteService remote, PowerState.State powerState) {
//        try {
//            Method method = remote.getClass().getMethod("setPower", PowerState.State.class);
//            method.invoke(remote, powerState);
//        } catch (NoSuchMethodException ex) {
//            logger.error("Remote [" + remote.getClass().getSimpleName() + "] has no set Power method");
//        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
//            logger.error("Could not invoke setPower method on remote [" + remote.getClass().getSimpleName() + "] with value [" + powerState + "]");
//        }
//    }
//
//    private PowerState invokeGetPowerState(GeneratedMessage message) throws CouldNotPerformException {
//        try {
//            Method method = message.getClass().getMethod("getPowerState");
//            return (PowerState) method.invoke(message);
//        } catch (Exception ex) {
//            throw new CouldNotPerformException("Could not get powerState from message [" + message + "]", ex);
//        }
//    }
    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
//        logger.info("Activating [" + getClass().getSimpleName() + "]");
//        logger.info("Source [" + sourceRemote.getId() + "], behaviour [" + sourceBehaviour + "]");
//        logger.info("Source [" + targetRemote.getId() + "], behaviour [" + targetBehaviour + "]");
//        sourceRemote.activate();
//        targetRemote.activate();
//        sourceRemote.requestStatus();
//        targetRemote.requestStatus();
//        sourceLatestPowerState = invokeGetPowerState(sourceRemote.getData()).getValue();
//        targetLatestPowerState = invokeGetPowerState(targetRemote.getData()).getValue();

        super.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
//        logger.info("Deactivating [" + getClass().getSimpleName() + "]");
//        sourceRemote.deactivate();
//        targetRemote.deactivate();
        super.deactivate();
    }
}
