package org.openbase.bco.dal.control.layer.unit.scene;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import org.openbase.bco.authentication.lib.AuthPair;
import org.openbase.bco.authentication.lib.AuthenticationBaseData;
import org.openbase.bco.dal.control.layer.unit.AbstractBaseUnitController;
import org.openbase.bco.dal.control.layer.unit.AbstractExecutableBaseUnitController.ActivationStateOperationServiceImpl;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.ServiceProvider;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.service.operation.ActivationStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.scene.SceneController;
import org.openbase.bco.dal.lib.state.States;
import org.openbase.bco.dal.lib.state.States.Activation;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.action.RemoteActionPool;
import org.openbase.bco.dal.remote.layer.unit.ButtonRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.MultiFuture;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState.MapFieldEntry;
import org.openbase.type.domotic.state.ButtonStateType.ButtonState;
import org.openbase.type.domotic.state.ButtonStateType.ButtonState.State;
import org.openbase.type.domotic.state.DoorStateType.DoorState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.dal.ButtonDataType.ButtonData;
import org.openbase.type.domotic.unit.scene.SceneDataType.SceneData;
import org.openbase.type.domotic.unit.scene.SceneDataType.SceneData.Builder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UnitConfig
 */
public class SceneControllerImpl extends AbstractBaseUnitController<SceneData, Builder> implements SceneController {


    public static final long ACTION_EXECUTION_TIMEOUT = 15000;

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescriptionType.ActionDescription.getDefaultInstance()));
    }

    private final Object buttonObserverLock = new SyncObject("ButtonObserverLock");
    private final Object requiredActionPoolObserverLock = new SyncObject("RequiredActionPoolObserverLock");
    private final Set<ButtonRemote> buttonRemoteSet;
    private final Observer<DataProvider<ButtonData>, ButtonData> buttonObserver;
    private final RemoteActionPool requiredActionPool;
    private final RemoteActionPool optionalActionPool;
    private final ActivationStateOperationServiceImpl activationStateOperationService;
    private final Observer<RemoteAction, ActionDescription> requiredActionPoolObserver;

    private final Observer<ServiceStateProvider<Message>, Message> activationStateObserver = new Observer<ServiceStateProvider<Message>, Message>() {
        @Override
        public void update(ServiceStateProvider<Message> source, Message data) throws Exception {

            // filter non active state
            if(!SceneControllerImpl.this.getActivationState().getValue().equals(Activation.ACTIVE)) {
                return;
            }

            // trigger initial validation
            for (RemoteAction remoteAction : requiredActionPool.getRemoteActionList()) {
                if (remoteAction.isSubmissionDone()) {
                    try {
                        requiredActionPoolObserver.update(remoteAction, remoteAction.getActionDescription());
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistory("Could not validate scene state!", ex, logger);
                    }
                }
            }
        }
    };

    public SceneControllerImpl() throws org.openbase.jul.exception.InstantiationException {
        super(SceneData.newBuilder());
        try {
            this.buttonRemoteSet = new HashSet<>();
            this.requiredActionPool = new RemoteActionPool(this);
            this.optionalActionPool = new RemoteActionPool(this);
            this.buttonObserver = (final DataProvider<ButtonData> source, ButtonData data) -> {

                // skip initial button state synchronization during system startup
                if (data.getButtonStateLast().getValue().equals(State.UNKNOWN)) {
                    return;
                }

                if (data.getButtonState().getValue().equals(ButtonState.State.PRESSED)) {
                    ActivationState.Builder activationState = ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE);
                    activationState = TimestampProcessor.updateTimestampWithCurrentTime(activationState, logger);
                    ActionParameter.Builder actionParameter = ActionDescriptionProcessor.generateDefaultActionParameter(activationState.build(), ServiceType.ACTIVATION_STATE_SERVICE, this);
                    actionParameter.setCause(data.getButtonState().getResponsibleAction());
                    applyAction(actionParameter);
                }

                if (data.getButtonState().getValue().equals(State.RELEASED)) {
                    ActivationState.Builder activationState = ActivationState.newBuilder().setValue(ActivationState.State.INACTIVE);
                    activationState = TimestampProcessor.updateTimestampWithCurrentTime(activationState, logger);
                    ActionParameter.Builder actionParameter = ActionDescriptionProcessor.generateDefaultActionParameter(activationState.build(), ServiceType.ACTIVATION_STATE_SERVICE, this);
                    actionParameter.setCause(data.getButtonState().getResponsibleAction());
                    applyAction(actionParameter);
                }
            };

            this.requiredActionPoolObserver = new Observer<RemoteAction, ActionDescription>() {
                @Override
                public void update(RemoteAction source, ActionDescription data) throws Exception {
                    synchronized (requiredActionPoolObserverLock) {
                        if (getActivationState().getValue() == ActivationState.State.ACTIVE && (!source.isValid() || !source.getActionState().equals(ActionState.State.EXECUTING))) {
                            logger.info("Deactivate scene {} because at least one required action {} is not executing.", SceneControllerImpl.this, source);
                            requiredActionPool.removeActionDescriptionObserver(requiredActionPoolObserver);
                            try {
                                applyServiceState(Activation.INACTIVE, ServiceType.ACTIVATION_STATE_SERVICE);
                            } catch (CouldNotPerformException ex) {
                                throw new CouldNotPerformException("Could not deactivate scene state!", ex);
                            }
                        }
                    }
                }
            };
            this.activationStateOperationService = new ActivationStateOperationServiceImpl();
            registerOperationService(ServiceType.ACTIVATION_STATE_SERVICE, activationStateOperationService);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        try {
            Registries.waitForData();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init(config);
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        config = super.applyConfigUpdate(config);

        try {
            synchronized (buttonObserverLock) {
                for (final ButtonRemote button : buttonRemoteSet) {
                    try {
                        logger.debug("update: remove " + LabelProcessor.getBestMatch(getConfig().getLabel()) + " for button  " + button.getLabel());
                    } catch (NotAvailableException ex) {
                        Logger.getLogger(SceneControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    button.removeDataObserver(buttonObserver);
                }

                buttonRemoteSet.clear();
                ButtonRemote buttonRemote;

                for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType(LabelProcessor.getBestMatch(config.getLabel()), UnitType.BUTTON)) {
                    try {
                        buttonRemote = Units.getUnit(unitConfig, false, Units.BUTTON);
                        buttonRemoteSet.add(buttonRemote);
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not register remote for Button[" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "]!", ex), logger);
                    }
                }
                if (isActive()) {
                    for (final ButtonRemote button : buttonRemoteSet) {
                        try {
                            logger.debug("update: register " + LabelProcessor.getBestMatch(getConfig().getLabel()) + " for button  " + button.getLabel());
                        } catch (NotAvailableException ex) {
                            Logger.getLogger(SceneControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        button.addDataObserver(buttonObserver);
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not init all related button remotes.", ex), logger);
        }

        final ActionParameter actionParameterPrototype = ActionParameter.newBuilder().setInterruptible(true).setSchedulable(true).setExecutionTimePeriod(Long.MAX_VALUE).build();
        requiredActionPool.initViaServiceStateDescription(config.getSceneConfig().getRequiredServiceStateDescriptionList(), actionParameterPrototype, () -> getActivationState().getValue() == ActivationState.State.ACTIVE);
        optionalActionPool.initViaServiceStateDescription(config.getSceneConfig().getOptionalServiceStateDescriptionList(), actionParameterPrototype, () -> getActivationState().getValue() == ActivationState.State.ACTIVE);
        return config;
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        synchronized (buttonObserverLock) {
            buttonRemoteSet.stream().forEach((button) -> {
                button.addDataObserver(buttonObserver);
            });
        }

        // make sure all required actions are validated as soon as the scene gets active.
        this.addServiceStateObserver(ServiceTempus.CURRENT, ServiceType.ACTIVATION_STATE_SERVICE, activationStateObserver);
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        // deregister action state observation.
        this.removeServiceStateObserver(ServiceTempus.CURRENT, ServiceType.ACTIVATION_STATE_SERVICE, activationStateObserver);

        synchronized (buttonObserverLock) {
            buttonRemoteSet.stream().forEach((button) -> {
                button.removeDataObserver(buttonObserver);
            });
        }
        super.deactivate();
    }

    @Override
    protected Future<ActionDescription> internalApplyActionAuthenticated(final AuthenticatedValue authenticatedValue, ActionDescription.Builder actionDescriptionBuilder, final AuthenticationBaseData authenticationBaseData, final AuthPair authPair) {

        // todo: remove this method to avoid bypassing the action scheduling of this unit after openbase/bco.dal#159 has been solved.

        try {
            // verify that the service type matches
            if (actionDescriptionBuilder.getServiceStateDescription().getServiceType() != ServiceType.ACTIVATION_STATE_SERVICE) {
                throw new NotAvailableException("Service[" + actionDescriptionBuilder.getServiceStateDescription().getServiceType().name() + "] is not available for scenes!");
            }

            // mark this action as intermediary because its bypassing the action scheduling and therefore not directly requestable via the scene unit.
            actionDescriptionBuilder.setIntermediary(true);

            // verify and prepare action description and retrieve the service state
            final ActivationState.Builder activationStateBuilder = ((ActivationState.Builder) ActionDescriptionProcessor.verifyActionDescription(actionDescriptionBuilder, this, true));

            // needs to be set before calling "setActivationState" because cause is used
            activationStateBuilder.setResponsibleAction(actionDescriptionBuilder);

            return FutureProcessor.postProcess((result, time, timeUnit) -> {

                // update builder with updated action impact
                final ActionDescription.Builder actionDescriptionBuilderNew = result.toBuilder();

                // needs to be updated again to update the action impact in the responsible action as well.
                // This has be done before calling "applyDataUpdate" because action is recovered via the responsible action.
                activationStateBuilder.setResponsibleAction(actionDescriptionBuilderNew);

                // publish new state as requested
                try (ClosableDataBuilder<Builder> dataBuilder = getDataBuilder(this)) {
                    dataBuilder.getInternalBuilder().setActivationStateRequested(activationStateBuilder);
                }

                // update transition id
                updateTransactionId();

                // update the internal data model
                applyDataUpdate(activationStateBuilder, ServiceType.ACTIVATION_STATE_SERVICE);

                // return the generated action description
                return actionDescriptionBuilderNew.build();

            }, activationStateOperationService.setActivationState(activationStateBuilder.build()));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    private void stop() {

        // cancel required actions
        final Map<RemoteAction, Future<ActionDescription>> remoteActionActionDescriptionFutureMap = requiredActionPool.cancel();

        // cancel optional actions
        remoteActionActionDescriptionFutureMap.putAll(optionalActionPool.cancel());

        // print if something went wrong
        try {
            RemoteActionPool.observeCancelation(remoteActionActionDescriptionFutureMap, this, 5, TimeUnit.SECONDS);
        } catch (MultiException ex) {
            if(!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory(ex, logger);
            }
        }
    }

    public class ActivationStateOperationServiceImpl implements ActivationStateOperationService {


        /**
         * Sets the activation state of the scene
         *
         * @param activationState the state to apply which offers the action description in its responsible action field.
         *
         * @return the responsible action of the given {@code activationState} with an updated list of impact actions.
         */
        @Override
        public Future<ActionDescription> setActivationState(final ActivationState activationState) {

            final ActionDescription.Builder responsibleActionBuilder = activationState.getResponsibleAction().toBuilder();

            switch (activationState.getValue()) {
                case DEACTIVE:
                    stop();
                    break;
                case ACTIVE:
                    final MultiFuture<ActionDescription> requiredActionsFuture = requiredActionPool.execute(responsibleActionBuilder);
                    final MultiFuture<ActionDescription> optionalActionsFuture = optionalActionPool.execute(responsibleActionBuilder);

                    final List<ActionDescription> actionList = new ArrayList<>();
                    final long checkStart = System.currentTimeMillis() + ACTION_EXECUTION_TIMEOUT;
                    long timeout;
                    try {
                        // wait for all required actions with a timeout, it at least one fails cancel all actions and leave with an exception
                        try {
                            for (final Future<ActionDescription> actionFuture : requiredActionsFuture.getFutureList()) {
                                timeout = checkStart - System.currentTimeMillis();
                                if (timeout <= 0) {
                                    throw new CouldNotPerformException("Timeout on submitting required actions.");
                                }

                                actionList.add(actionFuture.get(timeout, TimeUnit.MILLISECONDS));
                            }
                        } catch (TimeoutException | ExecutionException | CouldNotPerformException ex) {
                            // stop disabled, so scene can be reactivated because it will never be active but still possilble required actions are executed.
                            // stop();
                            return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("At least one required action could not be executed", ex));
                        }

                        // wait for all optional actions with a timeout, if one fails ignore its result and cancel the according action
                        for (final Future<ActionDescription> actionFuture : optionalActionsFuture.getFutureList()) {
                            try {
                                timeout = checkStart - System.currentTimeMillis();
                                if (timeout <= 0) {
                                    // if the timeout is exhausted cancel all actions that are not yet done
                                    if (actionFuture.isDone()) {
                                        actionList.add(actionFuture.get());
                                    } else {
                                        actionFuture.cancel(true);
                                    }
                                    continue;
                                }

                                actionList.add(actionFuture.get(timeout, TimeUnit.MILLISECONDS));
                            } catch (TimeoutException | ExecutionException ex) {
                                actionFuture.cancel(true);
                            }
                        }
                    } catch (InterruptedException ex) {
                        stop();
                        Thread.currentThread().interrupt();
                        return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Scene execution interrupted", ex));
                    }

                    // add all action impacts
                    for (final ActionDescription actionDescription : actionList) {
                        ActionDescriptionProcessor.updateActionImpacts(responsibleActionBuilder, actionDescription);
                    }

                    // register an observer which will deactivate the scene if one required action is now longer running
                    requiredActionPool.addActionDescriptionObserver(requiredActionPoolObserver);
                    break;
                default:
                    return FutureProcessor.canceledFuture(ActionDescription.class, new EnumNotSupportedException(activationState.getValue(), this));
            }

            return FutureProcessor.completedFuture(responsibleActionBuilder.build());
        }

        @Override
        public ServiceProvider getServiceProvider() {
            return SceneControllerImpl.this;
        }
    }
}
