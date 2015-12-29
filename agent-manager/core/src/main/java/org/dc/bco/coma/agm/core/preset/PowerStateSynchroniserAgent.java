/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.agm.core.preset;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.remote.unit.DALRemoteService;
import de.citec.dal.remote.unit.UnitRemoteFactory;
import de.citec.dal.remote.unit.UnitRemoteFactoryInterface;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.rst.processing.MetaConfigVariableProvider;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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

    private PowerState.State sourceLatestPowerState;
    /**
     * State that determines what the targets do if the source changes and vice
     * versa.
     *
     * OFF when all targets are off. 
     * ON when at least one target is one.
     */
    private PowerState.State targetLatestPowerState;
    private final List<DALRemoteService> targetRemotes = new ArrayList<>();
    private final DALRemoteService sourceRemote;
    private final PowerStateSyncBehaviour sourceBehaviour, targetBehaviour;
    private final UnitRemoteFactoryInterface factory;

    public PowerStateSynchroniserAgent(AgentConfig agentConfig) throws InstantiationException, CouldNotPerformException, InterruptedException {
        super(agentConfig);
        factory = UnitRemoteFactory.getInstance();
        logger.info("Creating PowerStateSynchroniserAgent");

        DeviceRegistryRemote deviceRegistryRemote = new DeviceRegistryRemote();
        deviceRegistryRemote.init();
        deviceRegistryRemote.activate();

        MetaConfigVariableProvider configVariableProvider = new MetaConfigVariableProvider("PowerStateSynchroniserAgent", agentConfig.getMetaConfig());

        sourceRemote = factory.createAndInitUnitRemote(deviceRegistryRemote.getUnitConfigById(configVariableProvider.getValue(SOURCE_KEY)));
        int i = 1;
        String unitId;
        try {
            while (!(unitId = configVariableProvider.getValue(TARGET_KEY + "_" + i)).isEmpty()) {
                logger.info("Found target id [" + unitId + "] with key [" + TARGET_KEY + "_" + i + "]");
                targetRemotes.add(factory.createAndInitUnitRemote(deviceRegistryRemote.getUnitConfigById(unitId)));
                i++;
            }
        } catch (NotAvailableException ex) {
            i--;
            logger.info("Found [" + i + "] target/s");
        }
        sourceBehaviour = PowerStateSyncBehaviour.valueOf(configVariableProvider.getValue(SOURCE_BEHAVIOUR_KEY));
        targetBehaviour = PowerStateSyncBehaviour.valueOf(configVariableProvider.getValue(TARGET_BEHAVIOUR_KEY));

        deviceRegistryRemote.shutdown();

        logger.info("Initializing observers");
        initObserver();
    }

    private void initObserver() {
        sourceRemote.addObserver(new Observer<GeneratedMessage>() {

            @Override
            public void update(Observable<GeneratedMessage> source, GeneratedMessage data) throws Exception {
                sourceLatestPowerState = invokeGetPowerState(data).getValue();
                logger.info("Recieved new value [" + sourceLatestPowerState + "] for source");
                if (sourceLatestPowerState == PowerState.State.OFF) {
                    if (targetLatestPowerState != PowerState.State.OFF) {
                        for (DALRemoteService targetRemote : targetRemotes) {
                            invokeSetPower(targetRemote, PowerState.State.OFF);
                        }
                    }
                } else if (sourceLatestPowerState == PowerState.State.ON) {
                    switch (targetBehaviour) {
                        case OFF:
                            if (targetLatestPowerState != PowerState.State.OFF) {
                                for (DALRemoteService targetRemote : targetRemotes) {
                                    invokeSetPower(targetRemote, PowerState.State.OFF);
                                }
                            }
                            break;
                        case ON:
                            if (targetLatestPowerState != PowerState.State.ON) {
                                for (DALRemoteService targetRemote : targetRemotes) {
                                    invokeSetPower(targetRemote, PowerState.State.ON);
                                }
                            }
                            break;
                        case LAST_STATE:
                            break;
                    }
                }
            }
        });

        for (DALRemoteService targetRemote : targetRemotes) {
            targetRemote.addObserver(new Observer<GeneratedMessage>() {

                @Override
                public void update(Observable<GeneratedMessage> source, GeneratedMessage data) throws Exception {
                    PowerState.State newPowerState = invokeGetPowerState(data).getValue();
                    logger.info("Recieved new value [" + targetLatestPowerState + "] for target [" + ((DALRemoteService) source).getId() + "]");
                    if (!updateLatestTargetPowerState(newPowerState)) {
                        return;
                    }
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
    }

    /**
     *
     * @param powerState
     * @return if the latest target power state has changed
     * @throws CouldNotPerformException
     */
    private boolean updateLatestTargetPowerState(PowerState.State powerState) throws CouldNotPerformException {
        if (targetLatestPowerState == PowerState.State.UNKNOWN) {
            targetLatestPowerState = powerState;
            return true;
        }
        if (targetLatestPowerState == PowerState.State.OFF && powerState == PowerState.State.ON) {
            targetLatestPowerState = PowerState.State.ON;
            return true;
        }

        if (targetLatestPowerState == PowerState.State.ON && powerState == PowerState.State.OFF) {
            targetLatestPowerState = PowerState.State.OFF;
            for (DALRemoteService targetRemote : targetRemotes) {
                if (invokeGetPowerState(targetRemote.getData()).getValue() == PowerState.State.ON) {
                    targetLatestPowerState = PowerState.State.ON;
                    break;
                }
            }
            return targetLatestPowerState == PowerState.State.OFF;
        }

        return false;
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
        sourceRemote.activate();
        String targetIds = "";
        targetLatestPowerState = PowerState.State.UNKNOWN;
        for (DALRemoteService targetRemote : targetRemotes) {
            targetRemote.activate();
            targetIds += "[" + targetRemote.getId() + "]";
            if ((targetLatestPowerState == PowerState.State.OFF || targetLatestPowerState == PowerState.State.UNKNOWN) && invokeGetPowerState(targetRemote.getData()).getValue() == PowerState.State.ON) {
                targetLatestPowerState = PowerState.State.ON;
            } else if (targetLatestPowerState == PowerState.State.UNKNOWN && invokeGetPowerState(targetRemote.getData()).getValue() == PowerState.State.OFF) {
                targetLatestPowerState = PowerState.State.OFF;
            }
        }
        logger.info("Source [" + sourceRemote.getId() + "], behaviour [" + sourceBehaviour + "]");
        logger.info("Targets [" + targetIds + "], behaviour [" + targetBehaviour + "]");
        sourceLatestPowerState = invokeGetPowerState(sourceRemote.getData()).getValue();
        super.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getClass().getSimpleName() + "]");
        sourceRemote.deactivate();
        for (DALRemoteService targetRemote : targetRemotes) {
            targetRemote.deactivate();
        }
        super.deactivate();
    }

    public DALRemoteService getSourceRemote() {
        return sourceRemote;
    }

    public List<DALRemoteService> getTargetRemotes() {
        return targetRemotes;
    }

    public PowerStateSyncBehaviour getSourceBehaviour() {
        return sourceBehaviour;
    }

    public PowerStateSyncBehaviour getTargetBehaviour() {
        return targetBehaviour;
    }
}
