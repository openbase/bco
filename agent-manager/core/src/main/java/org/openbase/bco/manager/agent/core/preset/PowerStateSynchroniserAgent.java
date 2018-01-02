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
import java.util.ArrayList;
import java.util.List;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

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
    private PowerState.State latestPowerStateSource;
    private PowerState.State latestPowerStateTarget;
    private final Observer<GeneratedMessage> sourceObserver, sourceRequestObserver, targetObserver, targetRequestObserer;
    private final List<UnitRemote> targetRemotes = new ArrayList<>();
    private UnitRemote sourceRemote;
    private PowerStateSyncBehaviour sourceBehaviour, targetBehaviour;
    
    public PowerStateSynchroniserAgent() throws InstantiationException, CouldNotPerformException {
        super(PowerStateSynchroniserAgent.class);

        // initialize observer
        sourceObserver = (final Observable<GeneratedMessage> source, GeneratedMessage data) -> {
            try {
                handleSourcePowerStateUpdate(((PowerState) data).getValue(), source);
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(ex, logger);
            }
        };
        sourceRequestObserver = (final Observable<GeneratedMessage> source, GeneratedMessage data) -> {
            try {
                handleSourcePowerStateRequest((PowerState) data);
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
                handleTargetPowerStateRequest((PowerState) data);
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(ex, logger);
            }
        };
    }
    
    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            logger.debug("Initializing PowerStateSynchroniserAgent[" + config.getLabel() + "]");
            Registries.getUnitRegistry().waitForData();
            
            MetaConfigVariableProvider configVariableProvider = new MetaConfigVariableProvider("PowerStateSynchroniserAgent", config.getMetaConfig());
            
            UnitConfig sourceUnitConfig = Registries.getUnitRegistry().getUnitConfigById(configVariableProvider.getValue(SOURCE_KEY));
            if (sourceUnitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                throw new NotAvailableException("Source[" + ScopeGenerator.generateStringRep(sourceUnitConfig.getScope()) + "] is not enabled");
            }
            sourceRemote = Units.getUnit(sourceUnitConfig, false);
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
            
            try {
                sourceBehaviour = PowerStateSyncBehaviour.valueOf(configVariableProvider.getValue(SOURCE_BEHAVIOUR_KEY));
            } catch (NotAvailableException ex) {
                sourceBehaviour = DEFAULT_SOURCE_BEHAVIOR;
            }
            try {
                targetBehaviour = PowerStateSyncBehaviour.valueOf(configVariableProvider.getValue(TARGET_BEHAVIOUR_KEY));
            } catch (NotAvailableException ex) {
                targetBehaviour = DEFAULT_TARGET_BEHAVIOR;
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * Handle a new requested power state for a target remote.
     *
     * @param powerState The requested power state for the target remote.
     */
    private void handleTargetPowerStateRequest(final PowerState powerState) {
        synchronized (AGENT_LOCK) {
            try {
                // if on is requested on a target and the source is not yet on turn it on
                if (powerState.getValue() == PowerState.State.ON && latestPowerStateTarget == PowerState.State.OFF) {
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
        synchronized (AGENT_LOCK) {
            try {
                // update the accumulated latest power state for all targets
                if (updateLatestTargetPowerState(targetPowerState)) {
                    // the state changed to off
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
                }
                
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not handle target power state update!", ex, logger);
            }
        }
    }

    /**
     * Method accumulates the target power states and returns if this requires a change for the source.
     *
     *
     * @param targetPowerState The update of the power state for one target remote.
     * @return If the latest target power state has changed to off.
     * @throws CouldNotPerformException If calling getPowerState on a target remote fails.1
     */
    private boolean updateLatestTargetPowerState(final PowerState.State targetPowerState) throws CouldNotPerformException {
        if (latestPowerStateTarget == PowerState.State.UNKNOWN) {
            latestPowerStateTarget = targetPowerState;
            return latestPowerStateTarget == PowerState.State.OFF;
        }
        
        if (latestPowerStateTarget == PowerState.State.OFF && targetPowerState == PowerState.State.ON) {
            latestPowerStateTarget = PowerState.State.ON;
            return false;
        }
        
        if (latestPowerStateTarget == PowerState.State.ON && targetPowerState == PowerState.State.OFF) {
            latestPowerStateTarget = PowerState.State.OFF;
            for (UnitRemote targetRemote : targetRemotes) {
                if (getPowerState(targetRemote).getValue() == PowerState.State.ON) {
                    latestPowerStateTarget = PowerState.State.ON;
                    break;
                }
            }
            return latestPowerStateTarget == PowerState.State.OFF;
        }
        
        return false;
    }

    /**
     * Handle a new requested power state for the source remote.
     *
     * @param powerState The requested power state for the source remote.
     */
    private void handleSourcePowerStateRequest(final PowerState powerState) {
        synchronized (AGENT_LOCK) {
            try {
                // if off is requested on the source and at least one target is currently on turn it off
                if (powerState.getValue() == PowerState.State.OFF && latestPowerStateSource != PowerState.State.OFF) {
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
    
    private void handleSourcePowerStateUpdate(final PowerState.State sourcePowerState, final Object target) {
        logger.debug("Handle new Value[" + sourcePowerState + "] for Source[" + target + "]");
        synchronized (AGENT_LOCK) {
            try {
                latestPowerStateSource = sourcePowerState;
                if (latestPowerStateSource == PowerState.State.ON) {
                    switch (targetBehaviour) {
                        case OFF:
                            if (latestPowerStateTarget != PowerState.State.OFF) {
                                for (UnitRemote targetRemote : targetRemotes) {
                                    setPowerState(targetRemote, OFF);
                                }
                            }
                            break;
                        case ON:
                            if (latestPowerStateTarget != PowerState.State.ON) {
                                for (UnitRemote targetRemote : targetRemotes) {
                                    setPowerState(targetRemote, ON);
                                }
                            }
                            break;
                        case LAST_STATE:
                            break;
                    }
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not handle source power state change!", ex, logger);
            }
        }
    }
    
    private void setPowerState(final UnitRemote remote, final PowerState powerState) throws CouldNotPerformException {
        Services.invokeOperationServiceMethod(ServiceType.POWER_STATE_SERVICE, remote, powerState);
    }
    
    private PowerState getPowerState(final Object object) throws CouldNotPerformException {
        return (PowerState) Services.invokeProviderServiceMethod(ServiceType.POWER_STATE_SERVICE, object);
    }
    
    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.debug("Executing PowerStateSynchroniser agent");
        sourceRemote.waitForData();
        String targetIds = "";
        latestPowerStateTarget = PowerState.State.UNKNOWN;
        for (UnitRemote targetRemote : targetRemotes) {
            targetRemote.waitForData();
            targetIds += "[" + targetRemote.getLabel() + "]";
            if ((latestPowerStateTarget == PowerState.State.OFF || latestPowerStateTarget == PowerState.State.UNKNOWN) && getPowerState(targetRemote.getData()).getValue() == PowerState.State.ON) {
                latestPowerStateTarget = PowerState.State.ON;
            } else if (latestPowerStateTarget == PowerState.State.UNKNOWN && getPowerState(targetRemote.getData()).getValue() == PowerState.State.OFF) {
                latestPowerStateTarget = PowerState.State.OFF;
            }
            targetRemote.addServiceStateObserver(ServiceTempus.REQUESTED, ServiceType.POWER_STATE_SERVICE, targetRequestObserer);
            targetRemote.addServiceStateObserver(ServiceTempus.CURRENT, ServiceType.POWER_STATE_SERVICE, targetObserver);
            handleTargetPowerStateUpdate(getPowerState(targetRemote.getData()).getValue());
        }

        // add data observer after all target remotes have been activated
        // else setPowerState could be called on a target remote without being active
        sourceRemote.addServiceStateObserver(ServiceTempus.REQUESTED, ServiceType.POWER_STATE_SERVICE, sourceRequestObserver);
        sourceRemote.addServiceStateObserver(ServiceTempus.CURRENT, ServiceType.POWER_STATE_SERVICE, sourceObserver);
        handleSourcePowerStateUpdate(getPowerState(sourceRemote.getData()).getValue(), sourceRemote);
        
        logger.debug("Source [" + sourceRemote.getLabel() + "] behaviour [" + sourceBehaviour + "]");
        logger.debug("Targets [" + targetIds + "] behaviour [" + targetBehaviour + "]");
    }
    
    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        logger.debug("Stopping PowerStateSynchroniserAgent...");
        sourceRemote.removeDataObserver(sourceObserver);
        targetRemotes.forEach((targetRemote) -> {
            targetRemote.removeDataObserver(sourceObserver);
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
