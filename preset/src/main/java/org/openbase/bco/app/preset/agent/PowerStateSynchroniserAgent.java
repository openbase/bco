package org.openbase.bco.app.preset.agent;

/*
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.openbase.bco.dal.control.layer.unit.agent.AbstractAgentController;
import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.dal.lib.action.ActionComparator;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.provider.PowerStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.lib.state.States;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.protobuf.processing.ProtoBufJSonProcessor;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.CloseableWriteLockWrapper;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.service.ServiceStateDescriptionType;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActionStateType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState.State;
import org.openbase.type.domotic.state.BrightnessStateType;
import org.openbase.type.domotic.state.ColorStateType;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.ArrayList;
import java.util.List;


/**
 * Agent that synchronizes the behavior of different units with a power source.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PowerStateSynchroniserAgent extends AbstractAgentController {

    public static final String SOURCE_KEY = "SOURCE";
    public static final String TARGET_KEY = "TARGET";

    private final ProtoBufJSonProcessor PROTO_BUF_JSON_PROCESSOR = new ProtoBufJSonProcessor();

    private final Object AGENT_LOCK = new SyncObject("PowerStateSynchroniserAgentLock");
    private final List<UnitRemote> targetRemotes = new ArrayList<>();
    private final Observer<DataProvider, Object> targetRemoteObserver;

    private String sourceId;
    private ActionComparator actionComparator;
    private RemoteAction sourceAction;

    public PowerStateSynchroniserAgent() throws CouldNotPerformException {
        this.targetRemoteObserver = (source, data) -> handleTargetUpdate();
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

                this.actionComparator = new ActionComparator(() -> getParentLocationRemote(false).getEmphasisState());

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

    private List<ActionDescription> filterActions(final List<ActionDescription> actionDescriptions, final PowerState powerState) throws CouldNotPerformException {
        final List<ActionDescription> result = new ArrayList<>();
        for (ActionDescription actionDescription : actionDescriptions) {
            ActionStateType.ActionState.State actionState = actionDescription.getActionState().getValue();
            if (actionState != ActionStateType.ActionState.State.SUBMISSION
                    && actionState != ActionStateType.ActionState.State.EXECUTING
                    && actionState != ActionStateType.ActionState.State.INITIATING) {
                continue;
            }


            switch (actionDescription.getServiceStateDescription().getServiceType()) {
                case POWER_STATE_SERVICE:
                    final PowerState actionPowerState = PROTO_BUF_JSON_PROCESSOR.deserialize(actionDescription.getServiceStateDescription().getServiceState(), PowerState.class);
                    if (actionPowerState.getValue() != powerState.getValue()) {
                        continue;
                    }
                    break;
                case BRIGHTNESS_STATE_SERVICE:
                    BrightnessStateType.BrightnessState actionBrightnessState = PROTO_BUF_JSON_PROCESSOR.deserialize(actionDescription.getServiceStateDescription().getServiceState(), BrightnessStateType.BrightnessState.class);
                    if (!PowerStateProviderService.isCompatible(powerState, actionBrightnessState)) {
                        continue;
                    }
                    break;
                case COLOR_STATE_SERVICE:
                    ColorStateType.ColorState actionColorState = PROTO_BUF_JSON_PROCESSOR.deserialize(actionDescription.getServiceStateDescription().getServiceState(), ColorStateType.ColorState.class);
                    if (!PowerStateProviderService.isCompatible(powerState, actionColorState)) {
                        continue;
                    }
                    break;
                default:
                    throw new CouldNotPerformException("Unexpected service type " + actionDescription.getServiceStateDescription().getServiceType().name());
            }

            result.add(actionDescription);
        }
        return result;
    }

    private void handleTargetUpdate() throws CouldNotPerformException, InterruptedException {
        synchronized (AGENT_LOCK) {
            final List<ActionDescription> allTargetAction = new ArrayList<>();

            logger.debug("Aggregate all target actions");
            for (UnitRemote targetRemote : targetRemotes) {

                // check if target unit
                if (!targetRemote.isDataAvailable()) {
                    continue;
                }
                final Message msg = (Message) targetRemote.getData();
                final Descriptors.FieldDescriptor actionFieldDescriptor = ProtoBufFieldProcessor.getFieldDescriptor(msg, Action.TYPE_FIELD_NAME_ACTION);
                allTargetAction.addAll((List<ActionDescription>) msg.getField(actionFieldDescriptor));
            }

            // we do not need to compute any aggregated states in case non are available.
            if (allTargetAction.isEmpty()) {
                return;
            }

            logger.debug("Filter for on actions");
            List<ActionDescription> actionDescriptions = filterActions(allTargetAction, States.Power.ON);

            ServiceStateDescriptionType.ServiceStateDescription.Builder builder1;
            if (actionDescriptions.isEmpty()) {
                logger.debug("No on actions, so filter for off...");
                actionDescriptions = filterActions(allTargetAction, States.Power.OFF);
                builder1 = ActionDescriptionProcessor.generateServiceStateDescription(States.Power.OFF, ServiceType.POWER_STATE_SERVICE);
            } else {
                builder1 = ActionDescriptionProcessor.generateServiceStateDescription(States.Power.ON, ServiceType.POWER_STATE_SERVICE);
            }
            builder1.setUnitId(sourceId);

            if (actionDescriptions.isEmpty()) {
                logger.error("No on or off actions... how can this happen?");
                //TODO: print a warning, this should not happen
                return;
            }

            logger.debug("Create remote actions");
            List<RemoteAction> remoteActions = new ArrayList<>();
            for (ActionDescription actionDescription : actionDescriptions) {
                remoteActions.add(new RemoteAction(actionDescription));
            }
            logger.debug("Sort remote actions");
            remoteActions.sort(actionComparator);

            final ActionDescription actionDescription = remoteActions.get(0).getActionDescription();
            logger.debug("On top is: {}", MultiLanguageTextProcessor.getBestMatch(actionDescription.getDescription()));
            ActionParameter.Builder builder = getDefaultActionParameter().toBuilder();
            builder.setCause(actionDescription);
            builder.setExecutionTimePeriod(actionDescription.getExecutionTimePeriod());
            builder.setPriority(actionDescription.getPriority());
            builder.setServiceStateDescription(builder1);

            this.sourceAction = new RemoteAction(Units.getUnit(sourceId, false).applyAction(builder), getToken(), () -> isValid());
        }
    }

    @Override
    protected ActionDescription execute(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        logger.trace("Executing PowerStateSynchroniser agent");

        for (UnitRemote targetRemote : targetRemotes) {
            targetRemote.addDataObserver(this.targetRemoteObserver);
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

        for (UnitRemote targetRemote : this.targetRemotes) {
            targetRemote.removeDataObserver(this.targetRemoteObserver);
        }

        if (this.sourceAction != null) {
            this.sourceAction.cancel();
        }
    }

}
