package org.openbase.bco.manager.agent.core.preset;

/*
 * #%L
 * BCO Manager Agent Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.service.ServiceStateObserver;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import rst.domotic.state.ActivationStateType.ActivationState.State;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

//import org.openbase.jul.exception.TimeoutException;

/**
 * Agent that synchronizes the behavior of different units with a power source.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PowerStateSynchroniserAgent extends AbstractAgentController {

    public static final String SOURCE_KEY = "SOURCE";
    public static final String TARGET_KEY = "TARGET";
    public static final String SOURCE_BEHAVIOUR_KEY = "SOURCE_BEHAVIOUR";
    public static final String TARGET_BEHAVIOUR_KEY = "TARGET_BEHAVIOUR";

    private static final PowerState ON = PowerState.newBuilder().setValue(PowerState.State.ON).build();
    private static final PowerState OFF = PowerState.newBuilder().setValue(PowerState.State.OFF).build();

    private static final PowerStateSyncBehaviour DEFAULT_SOURCE_BEHAVIOR = PowerStateSyncBehaviour.OFF;
    private static final PowerStateSyncBehaviour DEFAULT_TARGET_BEHAVIOR = PowerStateSyncBehaviour.ON;

    /**
     * Behavior that determines what the targets do if the source changes and vice
     * versa. The behavior is only valid for targets if the source changes to on
     * and only valid for the source if all targets turned off.
     */
    public enum PowerStateSyncBehaviour {

        /**
         * For source: If all targets are off the source stays or is turned on.
         * For targets: If the source is turned on all targets are turned or remain on.
         */
        ON,
        /**
         * For source: If all targets are off the source stays or is turned off.
         * For targets: If the source is turned on all targets are turned or remain off.
         */
        OFF,
        /**
         * For source: If all targets are of the source stays in its current state.
         * For targets: If the source is turned on all targets remain in their current state.
         */
        LAST_STATE;
    }

    private final Object AGENT_LOCK = new SyncObject("PowerStateLock");
    private PowerState.State latestPowerStateSource = PowerState.State.UNKNOWN;
    private PowerState.State latestPowerStateTarget = PowerState.State.UNKNOWN;
    private final Observer<GeneratedMessage> sourceObserver, sourceRequestObserver, targetObserver, targetRequestObserer;
    private final List<UnitRemote> targetRemotes = new ArrayList<>();
    private UnitRemote sourceRemote;
    private PowerStateSyncBehaviour sourceBehaviour, targetBehaviour;
    private final Map<UnitRemote, ServiceStateObserver> unitRemoteColorObserverMap = new HashMap<>();

    public PowerStateSynchroniserAgent() throws CouldNotPerformException {
        super(PowerStateSynchroniserAgent.class);

        // initialize observer
        sourceObserver = (final Observable<GeneratedMessage> source, GeneratedMessage data) -> {
            try {
                handleSourcePowerStateUpdate(((PowerState) data).getValue());
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(ex, logger);
            }
        };
        sourceRequestObserver = (final Observable<GeneratedMessage> source, GeneratedMessage data) -> {
            try {
                handleSourcePowerStateRequest(((PowerState) data).getValue());
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(ex, logger);
            }
        };
        targetObserver = (final Observable<GeneratedMessage> source, GeneratedMessage data) -> {
            try {
                handleTargetPowerStateUpdate(((PowerState) data).getValue());
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(ex, logger);
            }
        };
        targetRequestObserer = (final Observable<GeneratedMessage> source, GeneratedMessage data) -> {
            try {
                handleTargetPowerStateRequest(((PowerState) data).getValue());
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(ex, logger);
            }
        };
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        UnitConfig unitConfig = super.applyConfigUpdate(config);

        // save if the agent is active before this update
        boolean active = getActivationState().getValue() == State.ACTIVE;

        // deactivate before applying update if active
        if (active) {
            stop();
        }

        try {
            logger.info("ApplyConfigUpdate for PowerStateSynchroniserAgent[" + config.getLabel() + "]");
            Registries.getUnitRegistry().waitForData();

            MetaConfigVariableProvider configVariableProvider = new MetaConfigVariableProvider("PowerStateSynchroniserAgent", config.getMetaConfig());

            // get source remote
            UnitConfig sourceUnitConfig = Registries.getUnitRegistry().getUnitConfigById(configVariableProvider.getValue(SOURCE_KEY));
            if (sourceUnitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                throw new NotAvailableException("Source[" + ScopeGenerator.generateStringRep(sourceUnitConfig.getScope()) + "] is not enabled");
            }
            sourceRemote = Units.getUnit(sourceUnitConfig, false);

            // get target remotes
            targetRemotes.clear();
            int i = 1;
            String unitId;
            try {
                while (!(unitId = configVariableProvider.getValue(TARGET_KEY + "_" + i)).isEmpty()) {
                    i++;
                    logger.debug("Found target id [" + unitId + "] with key [" + TARGET_KEY + "_" + i + "]");
                    UnitConfig targetUnitConfig = CachedUnitRegistryRemote.getRegistry().getUnitConfigById(unitId);
                    if (targetUnitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                        logger.warn("TargetUnit[" + ScopeGenerator.generateStringRep(targetUnitConfig.getScope()) + "] "
                                + "of powerStateSynchroniserAgent[" + ScopeGenerator.generateStringRep(config.getScope()) + "] is disabled and therefore skipped!");
                        continue;
                    }
                    targetRemotes.add(Units.getUnit(unitId, false));
                }
            } catch (NotAvailableException ex) {
                i--;
                logger.debug("Found [" + i + "] target/s");
            }

            // get source behavior
            try {
                sourceBehaviour = PowerStateSyncBehaviour.valueOf(configVariableProvider.getValue(SOURCE_BEHAVIOUR_KEY));
            } catch (NotAvailableException ex) {
                sourceBehaviour = DEFAULT_SOURCE_BEHAVIOR;
            }

            // get target behavior
            try {
                targetBehaviour = PowerStateSyncBehaviour.valueOf(configVariableProvider.getValue(TARGET_BEHAVIOUR_KEY));
            } catch (NotAvailableException ex) {
                targetBehaviour = DEFAULT_TARGET_BEHAVIOR;
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply config update for PowerStateSynchroniser[" + config.getLabel() + "]", ex);
        }


        // reactivate if active before
        if (active) {
            execute();
        }

        return unitConfig;
    }

    /**
     * Handle a new requested power state for a target remote.
     *
     * @param powerState The requested power state for the target remote.
     */
    private void handleTargetPowerStateRequest(final PowerState.State powerState) {
        logger.debug("Handle new RequestedValue[" + powerState + "] for target");
        synchronized (AGENT_LOCK) {
            try {
                // if on is requested on a target and the source is not yet on turn it on
                if (powerState == PowerState.State.ON && latestPowerStateTarget == PowerState.State.OFF) {
                    if (getPowerState(sourceRemote).getValue() != PowerState.State.ON) {
                        setPowerState(sourceRemote, ON);
                    }
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not handle target power state request!", ex, logger);
            }
        }
    }

    private void handleTargetPowerStateUpdate(final PowerState.State targetPowerState) {
        logger.debug("Handle new Value[" + targetPowerState + "] for target");
        synchronized (AGENT_LOCK) {
            try {
                // update the accumulated latest power state for all targets
                if (updateLatestTargetPowerState(targetPowerState)) {
                    // the accumulated state has changed
                    switch (latestPowerStateTarget) {
                        case OFF:
                            switch (sourceBehaviour) {
                                case OFF:
                                    if (latestPowerStateSource != PowerState.State.OFF) {
                                        setPowerState(sourceRemote, OFF);
                                    }
                                    break;
                                case ON:
                                    if (latestPowerStateSource != PowerState.State.ON) {
                                        setPowerState(sourceRemote, ON);
                                    }
                                    break;
                                case LAST_STATE:
                                    break;
                            }
                            break;
                        case ON:
                            if (latestPowerStateSource != PowerState.State.ON) {
                                setPowerState(sourceRemote, ON);
                            }
                            break;
                    }
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not handle target power state update!", ex, logger);
            }
        }
    }

    private void handleTargetColorStateUpdate(final ColorState colorState, final UnitRemote colorableLightRemote) throws CouldNotPerformException {
        synchronized (AGENT_LOCK) {
            // if the color state changes turn source on before
            if (getPowerState(sourceRemote).getValue() != PowerState.State.ON) {
                final Future future = setPowerState(sourceRemote, ON);
                GlobalCachedExecutorService.submit(() -> {
                    try {
                        // wait for source remote to go on
                        future.get(5, TimeUnit.SECONDS);

                        // apply colorState again
                        Services.invokeOperationServiceMethod(ServiceType.COLOR_STATE_SERVICE, colorableLightRemote, colorState);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not set color state after turning on source", ex), logger);
                    }

                });
                setPowerState(sourceRemote, ON);
            }
        }
    }

    /**
     * Method accumulates the target power states and returns if it has changed.
     *
     * @param targetPowerState The update of the power state for one target remote.
     * @return If the latest target power state has changed to off.
     * @throws CouldNotPerformException If calling getPowerState on a target remote fails.1
     */
    private boolean updateLatestTargetPowerState(final PowerState.State targetPowerState) throws CouldNotPerformException {
        if (latestPowerStateTarget == PowerState.State.UNKNOWN) {
            latestPowerStateTarget = targetPowerState;
            // switch from unknown to on or off
            return true;
        }

        if (latestPowerStateTarget == PowerState.State.OFF && targetPowerState == PowerState.State.ON) {
            // switch from off to on
            latestPowerStateTarget = PowerState.State.ON;
            return true;
        }

        if (latestPowerStateTarget == PowerState.State.ON && targetPowerState == PowerState.State.OFF) {
            latestPowerStateTarget = PowerState.State.OFF;
            for (UnitRemote targetRemote : targetRemotes) {
                if (getPowerState(targetRemote).getValue() == PowerState.State.ON) {
                    latestPowerStateTarget = PowerState.State.ON;
                    break;
                }
            }
            // switch from on to off
            return latestPowerStateTarget == PowerState.State.OFF;
        }

        return false;
    }

    /**
     * Handle a new requested power state for the source remote.
     *
     * @param powerState The requested power state for the source remote.
     */
    private void handleSourcePowerStateRequest(final PowerState.State powerState) {
        logger.debug("Handle new RequestedValue[" + powerState + "] for Source");
        synchronized (AGENT_LOCK) {
            try {
                // if off is requested on the source and at least one target is currently on turn it off
                if (powerState == PowerState.State.OFF && latestPowerStateSource != PowerState.State.OFF) {
                    if (latestPowerStateTarget != PowerState.State.OFF) {
                        for (UnitRemote targetRemote : targetRemotes) {
                            setPowerState(targetRemote, OFF);
                        }
                    }
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not handle source power state request!", ex, logger);
            }
        }
    }

    private void handleSourcePowerStateUpdate(final PowerState.State sourcePowerState) {
        logger.debug("Handle new Value[" + sourcePowerState + "] for Source");
        synchronized (AGENT_LOCK) {
            try {
                // do nothing if not changed
                if (latestPowerStateSource == sourcePowerState) {
                    return;
                }

                latestPowerStateSource = sourcePowerState;
                switch (latestPowerStateSource) {
                    case ON:
                        switch (targetBehaviour) {
                            case OFF:
                                for (UnitRemote targetRemote : targetRemotes) {
                                    setPowerState(targetRemote, OFF);
                                }
                                break;
                            case ON:
                                for (UnitRemote targetRemote : targetRemotes) {
                                    setPowerState(targetRemote, ON);
                                }
                                break;
                            case LAST_STATE:
                                break;
                        }
                        break;
                    case OFF:
                        for (UnitRemote targetRemote : targetRemotes) {
                            setPowerState(targetRemote, OFF);
                        }
                        break;
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not handle source power state change!", ex, logger);
            }
        }
    }

    private Future setPowerState(final UnitRemote remote, final PowerState powerState) throws CouldNotPerformException {
        if (getPowerState(remote).getValue() == powerState.getValue()) {
            CompletableFuture completableFuture = new CompletableFuture();
            completableFuture.complete(null);
            return completableFuture;
        }
        return (Future) Services.invokeOperationServiceMethod(ServiceType.POWER_STATE_SERVICE, remote, powerState);
    }

    private PowerState getPowerState(final Object object) throws CouldNotPerformException {
        return (PowerState) Services.invokeProviderServiceMethod(ServiceType.POWER_STATE_SERVICE, object);
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.debug("Executing PowerStateSynchroniser agent");

        String targetIds = "";
        latestPowerStateTarget = PowerState.State.UNKNOWN;
        for (UnitRemote targetRemote : targetRemotes) {
            // add observer for requested and current power states
            targetRemote.addServiceStateObserver(ServiceTempus.REQUESTED, ServiceType.POWER_STATE_SERVICE, targetRequestObserer);
            targetRemote.addServiceStateObserver(ServiceTempus.CURRENT, ServiceType.POWER_STATE_SERVICE, targetObserver);

            // if colorable light create and add color state observer as well 
            if (targetRemote instanceof ColorableLightRemote) {
                unitRemoteColorObserverMap.put(targetRemote, new ServiceStateObserver(true) {
                    @Override
                    public void updateServiceData(Observable<Message> source, Message data) throws Exception {
                        handleTargetColorStateUpdate((ColorState) data, targetRemote);
                    }
                });
                targetRemote.addServiceStateObserver(ServiceTempus.CURRENT, ServiceType.COLOR_STATE_SERVICE, unitRemoteColorObserverMap.get(targetRemote));
                targetRemote.addServiceStateObserver(ServiceTempus.REQUESTED, ServiceType.COLOR_STATE_SERVICE, unitRemoteColorObserverMap.get(targetRemote));
            }

            // if data already available trigger observer and update latest state
            if (targetRemote.isDataAvailable()) {
                PowerState.State powerValue = getPowerState(targetRemote.getData()).getValue();

                if ((latestPowerStateTarget == PowerState.State.OFF || latestPowerStateTarget == PowerState.State.UNKNOWN) && powerValue == PowerState.State.ON) {
                    latestPowerStateTarget = PowerState.State.ON;
                } else if (latestPowerStateTarget == PowerState.State.UNKNOWN && powerValue == PowerState.State.OFF) {
                    latestPowerStateTarget = PowerState.State.OFF;
                }
                handleTargetPowerStateUpdate(getPowerState(targetRemote.getData()).getValue());
            }
        }

        // add data observer after all target remotes have been activated
        // else setPowerState could be called on a target remote without being active
        sourceRemote.addServiceStateObserver(ServiceTempus.REQUESTED, ServiceType.POWER_STATE_SERVICE, sourceRequestObserver);
        sourceRemote.addServiceStateObserver(ServiceTempus.CURRENT, ServiceType.POWER_STATE_SERVICE, sourceObserver);

        // if data available trigger update
        if (sourceRemote.isDataAvailable()) {
            handleSourcePowerStateUpdate(getPowerState(sourceRemote.getData()).getValue());
        }

        logger.debug("Source [" + sourceRemote.getLabel() + "] behaviour [" + sourceBehaviour + "]");
        logger.debug("Targets [" + targetIds + "] behaviour [" + targetBehaviour + "]");
    }

    @Override
    protected void stop() {
        try {
            logger.debug("Stopping PowerStateSynchroniserAgent[" + getLabel() + "]");
        } catch (NotAvailableException ex) {
            logger.debug("Stopping PowerStateSynchroniserAgent");
        }

        if (sourceRemote != null) {
            sourceRemote.removeServiceStateObserver(ServiceTempus.REQUESTED, ServiceType.POWER_STATE_SERVICE, sourceRequestObserver);
            sourceRemote.removeServiceStateObserver(ServiceTempus.CURRENT, ServiceType.POWER_STATE_SERVICE, sourceObserver);
        }

        for (UnitRemote unitRemote : unitRemoteColorObserverMap.keySet()) {
            unitRemote.removeServiceStateObserver(ServiceTempus.REQUESTED, ServiceType.COLOR_STATE_SERVICE, unitRemoteColorObserverMap.get(unitRemote));
        }
        unitRemoteColorObserverMap.clear();

        targetRemotes.forEach((targetRemote) -> {
            targetRemote.removeServiceStateObserver(ServiceTempus.REQUESTED, ServiceType.POWER_STATE_SERVICE, targetRequestObserer);
            targetRemote.removeServiceStateObserver(ServiceTempus.CURRENT, ServiceType.POWER_STATE_SERVICE, targetObserver);
        });
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
