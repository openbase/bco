/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.agent.preset;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.remote.unit.DALRemoteService;
import de.citec.dal.remote.unit.UnitRemoteFactory;
import de.citec.dal.remote.unit.UnitRemoteFactoryInterface;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import rst.configuration.EntryType;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class PowerStateSynchroniserAgent extends AbstractAgent {

    public static final String SOURCE_KEY = "SOURCE";
    public static final String TARGET_KEY = "TARGET";
    public static final String SOURCE_BEHAVIOUR_KEY = "SOURCE_BEHAVIOUR";
    public static final String TARGET_BEHAVIOUR_KEY = "TARGET_BEHAVIOUR";

    public enum PowerStateSyncBehaviour {

        ON,
        OFF,
        LAST_STATE;
    }

    private PowerState.State sourceLatestPowerState, targetLatestPowerState;
    private DALRemoteService sourceRemote, targetRemote;
    private PowerStateSyncBehaviour sourceBehaviour, targetBehaviour;
    private UnitRemoteFactoryInterface factory;

    public PowerStateSynchroniserAgent(AgentConfig agentConfig) throws InstantiationException, CouldNotPerformException, InterruptedException {
        super(agentConfig);
        factory = UnitRemoteFactory.getInstance();
        logger.info("Creating PowerStateSynchroniserAgent");

        DeviceRegistryRemote deviceRegistryRemote = new DeviceRegistryRemote();
        deviceRegistryRemote.init();
        deviceRegistryRemote.activate();

        for (EntryType.Entry entry : agentConfig.getMetaConfig().getEntryList()) {
            switch (entry.getKey()) {
                case SOURCE_KEY:
                    sourceRemote = factory.createAndInitUnitRemote(deviceRegistryRemote.getUnitConfigById(entry.getValue()));
                    break;
                case TARGET_KEY:
                    targetRemote = factory.createAndInitUnitRemote(deviceRegistryRemote.getUnitConfigById(entry.getValue()));
                    break;
                case SOURCE_BEHAVIOUR_KEY:
                    this.sourceBehaviour = PowerStateSyncBehaviour.valueOf(entry.getValue());
                    break;
                case TARGET_BEHAVIOUR_KEY:
                    this.targetBehaviour = PowerStateSyncBehaviour.valueOf(entry.getValue());
                    break;
                default:
                    logger.debug("Unknown meta config key [" + entry.getKey() + "] with value [" + entry.getValue() + "]");
            }
        }

        deviceRegistryRemote.shutdown();

        logger.info("Initializing observers");
        initObserver();
    }

    private void initObserver() {
        sourceRemote.addObserver(new Observer<GeneratedMessage>() {

            @Override
            public void update(Observable<GeneratedMessage> source, GeneratedMessage data) throws Exception {
                sourceLatestPowerState = invokeGetPowerState(data).getValue();
                logger.info("Recieved new value ["+sourceLatestPowerState+"] for source");
                if (sourceLatestPowerState == PowerState.State.OFF) {
                    if (targetLatestPowerState != PowerState.State.OFF) {
                        invokeSetPower(targetRemote, PowerState.State.OFF);
                    }
                } else if (sourceLatestPowerState == PowerState.State.ON) {
                    switch (targetBehaviour) {
                        case OFF:
                            if (targetLatestPowerState != PowerState.State.OFF) {
                                invokeSetPower(targetRemote, PowerState.State.OFF);
                            }
                            break;
                        case ON:
                            if (targetLatestPowerState != PowerState.State.ON) {
                                invokeSetPower(targetRemote, PowerState.State.ON);
                            }
                            break;
                        case LAST_STATE:
                            break;
                    }
                }
            }
        });

        targetRemote.addObserver(new Observer<GeneratedMessage>() {

            @Override
            public void update(Observable<GeneratedMessage> source, GeneratedMessage data) throws Exception {
                targetLatestPowerState = invokeGetPowerState(data).getValue();
                logger.info("Recieved new value ["+targetLatestPowerState+"] for target");
                if (targetLatestPowerState == PowerState.State.ON) {
                    if (sourceLatestPowerState != PowerState.State.ON) {
                        invokeSetPower(sourceRemote, PowerState.State.ON);
                    }
                } else if (targetLatestPowerState == PowerState.State.OFF) {
                    switch (sourceBehaviour) {
                        case OFF:
                            if (sourceLatestPowerState != PowerState.State.OFF) {
                                invokeSetPower(sourceRemote, PowerState.State.OFF);
                            }
                            break;
                        case ON:
                            if (sourceLatestPowerState != PowerState.State.ON) {
                                invokeSetPower(sourceRemote, PowerState.State.ON);
                            }
                            break;
                        case LAST_STATE:
                            break;
                    }
                }
            }
        });
    }

    private void invokeSetPower(DALRemoteService remote, PowerState.State powerState) {
        try {
            Method method = remote.getClass().getMethod("setPower", PowerState.State.class);
            method.invoke(remote, powerState);
        } catch (NoSuchMethodException ex) {
            logger.error("Remote [" + remote.getClass().getSimpleName() + "] has no set Power method");
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            logger.error("Could not invoke setPower method on remote [" + remote.getClass().getSimpleName() + "] with value [" + powerState + "]");
        }
    }

    private PowerState invokeGetPowerState(GeneratedMessage message) throws CouldNotPerformException {
        try {
            Method method = message.getClass().getMethod("getPowerState");
            return (PowerState) method.invoke(message);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not get powerState from message [" + message + "]", ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getClass().getSimpleName() + "]");
        logger.info("Source [" + sourceRemote.getId() + "], behaviour [" + sourceBehaviour + "]");
        logger.info("Source [" + targetRemote.getId() + "], behaviour [" + targetBehaviour + "]");
        sourceRemote.activate();
        targetRemote.activate();
        sourceRemote.requestStatus();
        targetRemote.requestStatus();
        sourceLatestPowerState = invokeGetPowerState(sourceRemote.getData()).getValue();
        targetLatestPowerState = invokeGetPowerState(targetRemote.getData()).getValue();
        super.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getClass().getSimpleName() + "]");
        sourceRemote.deactivate();
        targetRemote.deactivate();
        super.deactivate();
    }

    public DALRemoteService getSourceRemote() {
        return sourceRemote;
    }

    public DALRemoteService getTargetRemote() {
        return targetRemote;
    }

    public PowerStateSyncBehaviour getSourceBehaviour() {
        return sourceBehaviour;
    }

    public PowerStateSyncBehaviour getTargetBehaviour() {
        return targetBehaviour;
    }
}
