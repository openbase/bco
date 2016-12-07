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
import com.google.protobuf.GeneratedMessage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactory;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactoryImpl;
import org.openbase.bco.manager.agent.core.AbstractAgent;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PowerStateSynchroniserAgent extends AbstractAgent {

    public static final String SOURCE_KEY = "SOURCE";
    public static final String TARGET_KEY = "TARGET";
    public static final String SOURCE_BEHAVIOUR_KEY = "SOURCE_BEHAVIOUR";
    public static final String TARGET_BEHAVIOUR_KEY = "TARGET_BEHAVIOUR";
    private final Object AGENT_LOCK = new SyncObject("PowerStateLock");
    private static final PowerState ON = PowerState.newBuilder().setValue(PowerState.State.ON).build();
    private static final PowerState OFF = PowerState.newBuilder().setValue(PowerState.State.OFF).build();

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
     * OFF when all targets are off. ON when at least one target is one.
     */
    private final List<UnitRemote> targetRemotes = new ArrayList<>();
    private final UnitRemoteFactory factory;
    private final Observer<GeneratedMessage> sourceObserver, targetObserver;
    private PowerState.State targetLatestPowerState;
    private UnitRemote sourceRemote;
    private PowerStateSyncBehaviour sourceBehaviour, targetBehaviour;
    private UnitRegistry unitRegistry;

    public PowerStateSynchroniserAgent() throws InstantiationException, CouldNotPerformException {
        super();
        this.factory = UnitRemoteFactoryImpl.getInstance();

        // initialize observer
        sourceObserver = (final Observable<GeneratedMessage> source, GeneratedMessage data) -> {
            handleSourcePowerStateUpdate(invokeGetPowerState(data).getValue(), source);
        };
        targetObserver = (final Observable<GeneratedMessage> source, GeneratedMessage data) -> {
            handleTargetPowerStateUpdate(invokeGetPowerState(data).getValue(), source);
        };
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            logger.debug("Creating PowerStateSynchroniserAgent[" + config.getLabel() + "]");
            CachedUnitRegistryRemote.waitForData();
            unitRegistry = CachedUnitRegistryRemote.getRegistry();

            MetaConfigVariableProvider configVariableProvider = new MetaConfigVariableProvider("PowerStateSynchroniserAgent", config.getMetaConfig());

            sourceRemote = factory.newInitializedInstance(unitRegistry.getUnitConfigById(configVariableProvider.getValue(SOURCE_KEY)));
            int i = 1;
            String unitId;
            try {
                while (!(unitId = configVariableProvider.getValue(TARGET_KEY + "_" + i)).isEmpty()) {
                    logger.info("Found target id [" + unitId + "] with key [" + TARGET_KEY + "_" + i + "]");
                    targetRemotes.add(factory.newInitializedInstance(unitRegistry.getUnitConfigById(unitId)));
                    i++;
                }
            } catch (NotAvailableException ex) {
                i--;
                logger.info("Found [" + i + "] target/s");
            }
            sourceBehaviour = PowerStateSyncBehaviour.valueOf(configVariableProvider.getValue(SOURCE_BEHAVIOUR_KEY));
            targetBehaviour = PowerStateSyncBehaviour.valueOf(configVariableProvider.getValue(TARGET_BEHAVIOUR_KEY));

            logger.info("Initializing observers");
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void handleTargetPowerStateUpdate(final PowerState.State targetPowerState, final Object target) {
        synchronized (AGENT_LOCK) {
            try {
                logger.info("Received new Value[" + targetPowerState + "] for Target[" + target + "]");
                if (!updateLatestTargetPowerState(targetPowerState, target)) {
                    return;
                }
                if (targetLatestPowerState == PowerState.State.ON) {
                    if (sourceLatestPowerState != PowerState.State.ON) {
                        invokeSetPower(sourceRemote, ON);
                    }
                } else if (targetLatestPowerState == PowerState.State.OFF) {
                    switch (sourceBehaviour) {
                        case OFF:
                            if (sourceLatestPowerState != PowerState.State.OFF) {
                                invokeSetPower(sourceRemote, OFF);
                            }
                            break;
                        case ON:
                            if (sourceLatestPowerState != PowerState.State.ON) {
                                invokeSetPower(sourceRemote, ON);
                            }
                            break;
                        case LAST_STATE:
                            break;
                    }
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not handle target power state update!", ex, logger);
            }
        }
    }

    private void handleSourcePowerStateUpdate(final PowerState.State sourcePowerState, final Object target) {
//        try {
        synchronized (AGENT_LOCK) {
            this.sourceLatestPowerState = sourcePowerState;
            logger.info("Handle new Value[" + sourcePowerState + "] for Source[" + target + "]");
            if (sourceLatestPowerState == PowerState.State.OFF) {
                if (targetLatestPowerState != PowerState.State.OFF) {
                    targetRemotes.stream().forEach((targetRemote) -> {
                        invokeSetPower(targetRemote, OFF);
                    });
                }
            } else if (sourceLatestPowerState == PowerState.State.ON) {
                switch (targetBehaviour) {
                    case OFF:
                        if (targetLatestPowerState != PowerState.State.OFF) {
                            targetRemotes.stream().forEach((targetRemote) -> {
                                invokeSetPower(targetRemote, OFF);
                            });
                        }
                        break;
                    case ON:
                        if (targetLatestPowerState != PowerState.State.ON) {
                            targetRemotes.stream().forEach((targetRemote) -> {
                                invokeSetPower(targetRemote, ON);
                            });
                        }
                        break;
                    case LAST_STATE:
                        break;
                }
            }
        }
//        } catch (CouldNotPerformException ex) {
//            ExceptionPrinter.printHistory("Could not handle source power state update!", ex, logger);
//        }

    }

    /**
     *
     * @param targetPowerState
     * @return if the latest target power state has changed
     * @throws CouldNotPerformException
     */
    private boolean updateLatestTargetPowerState(final PowerState.State targetPowerState, final Object source) throws CouldNotPerformException {
        logger.info("Received new Value[" + targetPowerState + "] for Source[" + source + "]");
        if (targetLatestPowerState == PowerState.State.UNKNOWN) {
            targetLatestPowerState = targetPowerState;
            return true;
        }
        if (targetLatestPowerState == PowerState.State.OFF && targetPowerState == PowerState.State.ON) {
            targetLatestPowerState = PowerState.State.ON;
            return true;
        }

        if (targetLatestPowerState == PowerState.State.ON && targetPowerState == PowerState.State.OFF) {
            targetLatestPowerState = PowerState.State.OFF;
            for (UnitRemote targetRemote : targetRemotes) {
                if (invokeGetPowerState(targetRemote.getData()).getValue() == PowerState.State.ON) {
                    targetLatestPowerState = PowerState.State.ON;
                    break;
                }
            }
            return targetLatestPowerState == PowerState.State.OFF;
        }

        return false;
    }

    private void invokeSetPower(final UnitRemote remote, final PowerState powerState) {
        logger.info("Switch " + remote + " to " + powerState.getValue().name());
        try {
            Method method = remote.getClass().getMethod("setPowerState", PowerState.class);
            method.invoke(remote, powerState);
        } catch (NoSuchMethodException ex) {
            ExceptionPrinter.printHistory("Remote [" + remote.getClass().getSimpleName() + "] has no set Power method!", ex, logger);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ExceptionPrinter.printHistory("Could not invoke setPower method on remote [" + remote.getClass().getSimpleName() + "] with value [" + powerState + "]", ex, logger);
        }
    }

    private PowerState invokeGetPowerState(final Object message) throws CouldNotPerformException {
        try {
            Method method = message.getClass().getMethod("getPowerState");
            return (PowerState) method.invoke(message);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new CouldNotPerformException("Could not get powerState from message [" + message + "]", ex);
        }
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.info("Executing PowerStateSynchroniser agent");
        sourceRemote.activate();
        sourceRemote.waitForData();
        String targetIds = "";
        targetLatestPowerState = PowerState.State.UNKNOWN;
        for (UnitRemote targetRemote : targetRemotes) {
            targetRemote.activate();
            targetRemote.waitForData();
            targetIds += "[" + targetRemote.getLabel() + "]";
            if ((targetLatestPowerState == PowerState.State.OFF || targetLatestPowerState == PowerState.State.UNKNOWN) && invokeGetPowerState(targetRemote.getData()).getValue() == PowerState.State.ON) {
                targetLatestPowerState = PowerState.State.ON;
            } else if (targetLatestPowerState == PowerState.State.UNKNOWN && invokeGetPowerState(targetRemote.getData()).getValue() == PowerState.State.OFF) {
                targetLatestPowerState = PowerState.State.OFF;
            }
            targetRemote.addDataObserver(targetObserver);
            handleTargetPowerStateUpdate(invokeGetPowerState(targetRemote.getData()).getValue(), targetRemote);
        }

        // add data observer after all target remotes have been activated
        // else setPowerState could be called on a target remote without being active
        sourceRemote.addDataObserver(sourceObserver);
        handleSourcePowerStateUpdate(invokeGetPowerState(sourceRemote.getData()).getValue(), sourceRemote);

        logger.info("Source [" + sourceRemote.getLabel() + "] behaviour [" + sourceBehaviour + "]");
        logger.info("Targets [" + targetIds + "] behaviour [" + targetBehaviour + "]");
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        logger.info("Stopping PowerStateSynchroniserAgent...");
        sourceRemote.removeDataObserver(sourceObserver);
        for (UnitRemote targetRemote : targetRemotes) {
            targetRemote.removeDataObserver(sourceObserver);
            targetRemote.deactivate();
        }
        sourceRemote.deactivate();
    }

    public UnitRemote getSourceRemote() {
        return sourceRemote;
    }

    public List<UnitRemote> getTargetRemotes() {
        return targetRemotes;
    }

    public PowerStateSyncBehaviour getSourceBehaviour() {
        return sourceBehaviour;
    }

    public PowerStateSyncBehaviour getTargetBehaviour() {
        return targetBehaviour;
    }
}
