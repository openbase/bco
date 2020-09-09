package org.openbase.bco.app.preset.agent;

/*
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.bco.dal.control.layer.unit.agent.AbstractAgentController;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.provider.BrightnessStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.ColorStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.CloseableWriteLockWrapper;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState.State;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Agent that synchronizes the behavior of different units with a power source.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PowerStateSynchroniserAgent extends AbstractAgentController {

    public static final String SOURCE_KEY = "SOURCE";
    public static final String TARGET_KEY = "TARGET";

    private final Object AGENT_LOCK = new SyncObject("PowerStateSynchroniserAgentLock");
    private final List<UnitRemote> targetRemotes = new ArrayList<>();
    private final Map<ServiceType, Observer<ServiceStateProvider<Message>, Message>> serviceTypeRequestedObserverMap;
    private final Map<ServiceType, Observer<ServiceStateProvider<Message>, Message>> serviceTypeCurrentObserverMap;

    private String sourceId;
    private RemoteAction remoteAction;

    public PowerStateSynchroniserAgent() throws CouldNotPerformException {

        serviceTypeRequestedObserverMap = new HashMap<>();
        serviceTypeCurrentObserverMap = new HashMap<>();

        final Observer<ServiceStateProvider<Message>, Message> requestedPowerStateObserver = (final ServiceStateProvider<Message> source, final Message data) -> handleRequestedPowerStateUpdate((PowerState) data);
        final Observer<ServiceStateProvider<Message>, Message> requestedBrightnessStateObserver = (final ServiceStateProvider<Message> source, final Message data) -> handleRequestedPowerStateUpdate(BrightnessStateProviderService.toPowerState((BrightnessState) data));
        final Observer<ServiceStateProvider<Message>, Message> requestedColorStateObserver = (final ServiceStateProvider<Message> source, final Message data) -> handleRequestedPowerStateUpdate(ColorStateProviderService.toPowerState((ColorState) data));

        final Observer<ServiceStateProvider<Message>, Message> currentPowerStateObserver = (final ServiceStateProvider<Message> source, final Message data) -> handleCurrentPowerStateUpdate((PowerState) data);
        final Observer<ServiceStateProvider<Message>, Message> currentBrightnessStateObserver = (final ServiceStateProvider<Message> source, final Message data) -> handleCurrentPowerStateUpdate(BrightnessStateProviderService.toPowerState((BrightnessState) data));
        final Observer<ServiceStateProvider<Message>, Message> currentColorStateObserver = (final ServiceStateProvider<Message> source, final Message data) -> handleCurrentPowerStateUpdate(ColorStateProviderService.toPowerState((ColorState) data));

        serviceTypeRequestedObserverMap.put(ServiceType.POWER_STATE_SERVICE, requestedPowerStateObserver);
        serviceTypeRequestedObserverMap.put(ServiceType.BRIGHTNESS_STATE_SERVICE, requestedBrightnessStateObserver);
        serviceTypeRequestedObserverMap.put(ServiceType.COLOR_STATE_SERVICE, requestedColorStateObserver);

        serviceTypeCurrentObserverMap.put(ServiceType.POWER_STATE_SERVICE, currentPowerStateObserver);
        serviceTypeCurrentObserverMap.put(ServiceType.BRIGHTNESS_STATE_SERVICE, currentBrightnessStateObserver);
        serviceTypeCurrentObserverMap.put(ServiceType.COLOR_STATE_SERVICE, currentColorStateObserver);
    }

    private void handleRequestedPowerStateUpdate(final PowerState powerState) {
        try {
            logger.trace("handle requested state: {}", powerState.getValue().name());
            synchronized (AGENT_LOCK) {
                if (powerState.getValue() != PowerState.State.ON) {
                    // do nothing because target was not turned on
                    logger.trace("do nothing because target was not turned on");
                    return;
                }

                // create action, copy priority and execute with responsible action as cause
                final ActionParameter.Builder actionParameter = generateAction(UnitType.UNKNOWN, ServiceType.POWER_STATE_SERVICE, PowerState.newBuilder().setValue(PowerState.State.ON)).setPriority(powerState.getResponsibleAction().getPriority());
                actionParameter.getServiceStateDescriptionBuilder().setUnitId(sourceId);
                logger.trace("switch on by handleRequestedPowerStateUpdate");
                executeAction(actionParameter.build(), powerState.getResponsibleAction());
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }

    private void handleCurrentPowerStateUpdate(final PowerState powerState) {
        try {
            logger.trace("handle current state: {}", powerState.getValue().name());
            synchronized (AGENT_LOCK) {
                if (powerState.getValue() != PowerState.State.OFF) {
                    // do nothing because target was not turned off
                    logger.trace("do nothing because target was not turned off");
                    return;
                }

                // create action, set priority to low and execute with responsible action as cause
                final ActionParameter.Builder actionParameter = generateAction(UnitType.UNKNOWN, ServiceType.POWER_STATE_SERVICE, PowerState.newBuilder().setValue(PowerState.State.OFF)).setPriority(Priority.LOW);
                actionParameter.getServiceStateDescriptionBuilder().setUnitId(sourceId);
                logger.trace("switch off by handleCurrentPowerStateUpdate");
                executeAction(actionParameter.build(), powerState.getResponsibleAction());
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }

    private void executeAction(final ActionParameter actionParameter, final ActionDescription responsibleAction) throws InterruptedException, InstantiationException {
        remoteAction = new RemoteAction(this, actionParameter, getToken());
        if (responsibleAction.getActionId().isEmpty()) {
            remoteAction.execute();
        } else {
            remoteAction.execute(responsibleAction);
        }
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        try (final CloseableWriteLockWrapper ignored = getManageWriteLockInterruptible(this)) {
            UnitConfig unitConfig = super.applyConfigUpdate(config);

            // save if the agent is active before this update
            final ActivationState previousActivationState = getActivationState();

            // deactivate before applying update if active
            if (previousActivationState.getValue() == State.ACTIVE) {
                stop(ActivationState.newBuilder().setValue(State.INACTIVE).build());
            }

            try {
                logger.trace("ApplyConfigUpdate for PowerStateSynchroniserAgent[{}]", LabelProcessor.getBestMatch(config.getLabel()));
                Registries.waitForData();

                MetaConfigVariableProvider configVariableProvider = new MetaConfigVariableProvider("PowerStateSynchroniserAgent", config.getMetaConfig());

                // get source remote
                UnitConfig sourceUnitConfig = Registries.getUnitRegistry().getUnitConfigById(configVariableProvider.getValue(SOURCE_KEY));
                if (sourceUnitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    throw new NotAvailableException("Source[" + ScopeProcessor.generateStringRep(sourceUnitConfig.getScope()) + "] is not enabled");
                }
                sourceId = sourceUnitConfig.getId();

                // get target remotes
                targetRemotes.clear();
                int i = 1;
                String unitId;
                try {
                    while (!(unitId = configVariableProvider.getValue(TARGET_KEY + "_" + i)).isEmpty()) {
                        i++;
                        logger.trace("Found target id [" + unitId + "] with key [" + TARGET_KEY + "_" + i + "]");
                        UnitConfig targetUnitConfig = CachedUnitRegistryRemote.getRegistry().getUnitConfigById(unitId);
                        if (targetUnitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                            logger.warn("TargetUnit[" + ScopeProcessor.generateStringRep(targetUnitConfig.getScope()) + "] "
                                    + "of powerStateSynchroniserAgent[" + ScopeProcessor.generateStringRep(config.getScope()) + "] is disabled and therefore skipped!");
                            continue;
                        }
                        targetRemotes.add(Units.getUnit(unitId, false));
                    }
                } catch (NotAvailableException ex) {
                    i--;
                    logger.trace("Found [" + i + "] target/s");
                }
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not apply config update for PowerStateSynchroniser[" + LabelProcessor.getBestMatch(config.getLabel()) + "]", ex);
            }


            // reactivate if active before
            if (previousActivationState.getValue() == State.ACTIVE) {
                execute(previousActivationState);
            }

            return unitConfig;
        }
    }

    @Override
    protected ActionDescription execute(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        logger.trace("Executing PowerStateSynchroniser agent");

        PowerState powerState = null;
        for (final UnitRemote targetRemote : targetRemotes) {
            try {
                for (final Entry<ServiceType, Observer<ServiceStateProvider<Message>, Message>> entry : serviceTypeRequestedObserverMap.entrySet()) {
                    if (!targetRemote.getAvailableServiceTypes().contains(entry.getKey())) {
                        continue;
                    }
                    // register observer
                    targetRemote.addServiceStateObserver(ServiceTempus.REQUESTED, entry.getKey(), entry.getValue());
                    targetRemote.addServiceStateObserver(ServiceTempus.CURRENT, entry.getKey(), entry.getValue());
                    targetRemote.addServiceStateObserver(ServiceTempus.CURRENT, entry.getKey(), serviceTypeCurrentObserverMap.get(entry.getKey()));
                }
            } catch (NotAvailableException ex) {
                logger.warn("Could not add observers to remote " + targetRemote, ex);
            }

            if (targetRemote.isDataAvailable()) {
                PowerState tmpPowerState = (PowerState) Services.invokeProviderServiceMethod(ServiceType.POWER_STATE_SERVICE, targetRemote);
                if (powerState == null || powerState.getValue() != PowerState.State.ON) {
                    powerState = tmpPowerState;
                }
            }
        }

        if (powerState != null) {
            if (powerState.getValue() == PowerState.State.ON) {
                handleRequestedPowerStateUpdate(powerState);
            } else {
                handleCurrentPowerStateUpdate(powerState);
            }
        }

        return activationState.getResponsibleAction();
    }

    @Override
    protected void stop(final ActivationState activationState) throws InterruptedException, CouldNotPerformException {
        try {
            logger.trace("Stopping PowerStateSynchroniserAgent[" + getLabel() + "]");
        } catch (NotAvailableException ex) {
            logger.trace("Stopping PowerStateSynchroniserAgent");
        }


        targetRemotes.forEach((targetRemote) -> {
            try {
                for (final Entry<ServiceType, Observer<ServiceStateProvider<Message>, Message>> entry : serviceTypeRequestedObserverMap.entrySet()) {
                    if (!targetRemote.getAvailableServiceTypes().contains(entry.getKey())) {
                        continue;
                    }
                    targetRemote.removeServiceStateObserver(ServiceTempus.REQUESTED, entry.getKey(), entry.getValue());
                    targetRemote.removeServiceStateObserver(ServiceTempus.CURRENT, entry.getKey(), entry.getValue());
                    targetRemote.removeServiceStateObserver(ServiceTempus.CURRENT, entry.getKey(), serviceTypeCurrentObserverMap.get(entry.getKey()));
                }
            } catch (NotAvailableException ex) {
                logger.warn("Could not remove observers from remote " + targetRemote, ex);
            }
        });

        if (remoteAction != null) {
            remoteAction.cancel();
            remoteAction = null;
        }
        super.stop(activationState);
    }
}
