package org.openbase.bco.dal.control.layer.unit.scene;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.*;
import org.openbase.bco.dal.lib.layer.service.operation.ActivationStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.lib.layer.unit.scene.SceneController;
import org.openbase.bco.dal.lib.state.States.Activation;
import org.openbase.bco.dal.remote.action.Actions;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.action.RemoteActionPool;
import org.openbase.bco.dal.remote.layer.unit.ButtonRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.CloseableWriteLockWrapper;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.schedule.TimeoutSplitter;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionReferenceType.ActionReference;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.ButtonStateType.ButtonState;
import org.openbase.type.domotic.state.ButtonStateType.ButtonState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.dal.ButtonDataType.ButtonData;
import org.openbase.type.domotic.unit.scene.SceneDataType.SceneData;
import org.openbase.type.domotic.unit.scene.SceneDataType.SceneData.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * UnitConfig
 */
public class SceneControllerImpl extends AbstractBaseUnitController<SceneData, Builder> implements SceneController {

    // Action registration timeout in ms.
    public static final long ACTION_REGISTRATION_TIMEOUT = 15000;

    private final Object buttonObserverLock = new SyncObject("ButtonObserverLock");
    private final Set<ButtonRemote> buttonRemoteSet;
    private final Observer<DataProvider<ButtonData>, ButtonData> buttonObserver;
    private final RemoteActionPool requiredActionPool;
    private final RemoteActionPool optionalActionPool;
    private final ActivationStateOperationServiceImpl activationStateOperationService;

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

                // apply action
                if ((data.getButtonState().getValue().equals(ButtonState.State.PRESSED) && data.getButtonStateLast().getValue() != ButtonState.State.PRESSED) ||
                        (data.getButtonState().getValue().equals(State.RELEASED) && data.getButtonStateLast().getValue() != State.RELEASED)) {

                    ActivationState.Builder activationState;

                    // toggle state
                    if (getActivationState().getValue() != ActivationState.State.ACTIVE) {
                        activationState = Activation.ACTIVE.toBuilder();
                    } else {
                        activationState = Activation.INACTIVE.toBuilder();
                    }

                    activationState = TimestampProcessor.updateTimestampWithCurrentTime(activationState, logger);
                    ActionParameter.Builder actionParameter = ActionDescriptionProcessor.generateDefaultActionParameter(activationState.build(), ServiceType.ACTIVATION_STATE_SERVICE, this);
                    actionParameter.setCause(data.getButtonState().getResponsibleAction());
                    applyAction(actionParameter);
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

    final List<ActionDescription> impactActionList = new ArrayList<>();

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        try (final CloseableWriteLockWrapper ignored = getManageWriteLockInterruptible(this)) {
            config = super.applyConfigUpdate(config);

            try {
                synchronized (buttonObserverLock) {
                    for (final ButtonRemote button : buttonRemoteSet) {
                        logger.debug("update: remove " + LabelProcessor.getBestMatch(getConfig().getLabel(), "?") + " for button  " + button.getLabel());
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

                    // clear old impact
                    impactActionList.clear();

                    // register required action impact
                    for (ServiceStateDescription serviceStateDescription : config.getSceneConfig().getRequiredServiceStateDescriptionList()) {
                        impactActionList.addAll(ServiceStateProcessor.computeActionImpact(serviceStateDescription));
                    }

                    // register optional action impact
                    for (ServiceStateDescription serviceStateDescription : config.getSceneConfig().getOptionalServiceStateDescriptionList()) {
                        impactActionList.addAll(ServiceStateProcessor.computeActionImpact(serviceStateDescription));
                    }

                    if (isActive()) {
                        for (final ButtonRemote button : buttonRemoteSet) {
                            logger.debug("update: register " + LabelProcessor.getBestMatch(getConfig().getLabel(), "?") + " for button  " + button.getLabel());
                            button.addDataObserver(buttonObserver);
                        }
                    }
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not init all related button remotes.", ex), logger);
            }

            final ActionParameter actionParameterPrototype = ActionParameter.newBuilder()
                    .setInterruptible(true)
                    .setSchedulable(true)
                    .setExecutionTimePeriod(Long.MAX_VALUE).build();
            requiredActionPool.initViaServiceStateDescription(config.getSceneConfig().getRequiredServiceStateDescriptionList(), actionParameterPrototype, () -> getActivationState().getValue() == ActivationState.State.ACTIVE);
            optionalActionPool.initViaServiceStateDescription(config.getSceneConfig().getOptionalServiceStateDescriptionList(), actionParameterPrototype, () -> getActivationState().getValue() == ActivationState.State.ACTIVE);
            return config;
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        synchronized (buttonObserverLock) {
            buttonRemoteSet.stream().forEach((button) -> {
                button.addDataObserver(buttonObserver);
            });
        }

        // trigger startup reschedule to apply termination state
        reschedule();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        synchronized (buttonObserverLock) {
            buttonRemoteSet.stream().forEach((button) -> {
                button.removeDataObserver(buttonObserver);
            });
        }
        super.deactivate();
    }

    @Override
    protected Future<ActionDescription> internalApplyActionAuthenticated(final AuthenticatedValue authenticatedValue, ActionDescription.Builder actionDescriptionBuilder, final AuthenticationBaseData authenticationBaseData, final AuthPair authPair) {

        // register action impact
        for (ActionDescription action : impactActionList) {
            ActionDescriptionProcessor.updateActionImpacts(actionDescriptionBuilder, action);
        }

        // forward to action scheduling
        return super.internalApplyActionAuthenticated(authenticatedValue, actionDescriptionBuilder, authenticationBaseData, authPair);
    }

    private void stop() {

        // cancel required actions
        final Map<RemoteAction, Future<ActionDescription>> remoteActionActionDescriptionFutureMap = requiredActionPool.cancel();

        // cancel optional actions
        remoteActionActionDescriptionFutureMap.putAll(optionalActionPool.cancel());

        // print if something went wrong
        try {
            RemoteActionPool.observeCancellation(remoteActionActionDescriptionFutureMap, this, 10, TimeUnit.SECONDS);
        } catch (MultiException ex) {
            if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory(ex, logger);
            }
        }
    }

    public class ActivationStateOperationServiceImpl implements ActivationStateOperationService {

        private RequiredActionObserver actionObserver = null;

        /**
         * Sets the activation state of the scene
         *
         * @param activationState the state to apply which offers the action description in its responsible action field.
         *
         * @return the responsible action of the given {@code activationState} with an updated list of impact actions.
         */
        @Override
        public synchronized Future<ActionDescription> setActivationState(final ActivationState activationState) {
            final ActionDescription.Builder responsibleActionBuilder = activationState.getResponsibleAction().toBuilder();


            // shutdown all existing action observer to not let old observation interfere with new activations.
            if(actionObserver != null) {
                actionObserver.shutdown();
            }

            // mark scene action as not replaceable since scene takes care of managing this actions.
            responsibleActionBuilder.setReplaceable(false);

            try {
                logger.trace("inform about " + activationState.getValue().name());
                applyServiceState(activationState, ServiceType.ACTIVATION_STATE_SERVICE);
            } catch (CouldNotPerformException ex) {
                return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not " + StringProcessor.transformUpperCaseToPascalCase(activationState.getValue().name()) + " " + this, ex));
            }

            switch (activationState.getValue()) {
                case INACTIVE:
                    stop();
                    break;
                case ACTIVE:

                    // execute actions
                    final List<RemoteAction> requiredActionList = requiredActionPool.execute(responsibleActionBuilder);
                    final List<RemoteAction> optionalActionList = optionalActionPool.execute(responsibleActionBuilder);

                    // preparations
                    final List<ActionReference> requiredActionImpactList = new ArrayList<>();
                    final TimeoutSplitter timeout = new TimeoutSplitter(ACTION_REGISTRATION_TIMEOUT, TimeUnit.MILLISECONDS);

                    try {
                        // wait for all required actions with a timeout, in case one fails cancel all actions and cancel the activation
                        for (final RemoteAction requiredAction : requiredActionList) {
                            try {
                                // wait for registration
                                if (!requiredAction.isRegistrationDone()) {
                                    requiredAction.waitForRegistration(timeout.getTime(), TimeUnit.MILLISECONDS);
                                }

                                // create a copy of the impact of all required actions
                                requiredActionImpactList.addAll(requiredAction.getActionImpact(true));

                            } catch (org.openbase.jul.exception.TimeoutException ex) {
                                // if the timeout is exhausted then just continue since we want to keep on trying as long as the scene is active.
                                continue;
                            } catch (CancellationException | CouldNotPerformException ex) {
                                ExceptionPrinter.printHistory("Required " + requiredAction + " of " + getLabel("?") + " could not be executed!", ex, logger, LogLevel.DEBUG);
                                return FutureProcessor.canceledFuture(ActionDescription.class, new RejectedException("Required action " + requiredAction + " could not be executed", ex));
                            }
                        }

                        // wait for all optional actions with a timeout, if one fails ignore its result and cancel the according action
                        for (final RemoteAction optionalAction : optionalActionList) {
                            try {
                                optionalAction.waitForRegistration(timeout.getTime(), TimeUnit.MILLISECONDS);
                            } catch (org.openbase.jul.exception.TimeoutException ex) {
                                // if the timeout is exhausted than just continue since we want to keep on trying as long as the scene is active.
                                continue;
                            } catch (CancellationException | CouldNotPerformException ex) {
                                ExceptionPrinter.printHistory("Optional " + optionalAction + " of " + getLabel("?") + " could not be executed!", ex, logger, LogLevel.TRACE);
                            }
                        }

                        // register an observer which will deactivate the scene if one required action is now longer running
                        try {
                            actionObserver = new RequiredActionObserver(requiredActionImpactList, getActionById(responsibleActionBuilder.getActionId(), "SceneController"));
                        } catch (NotAvailableException ex) {
                            new FatalImplementationErrorException("Action is not available even when just created!", this, ex);
                        }

                    } catch (InterruptedException ex) {
                        stop();
                        Thread.currentThread().interrupt();
                        return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Scene execution interrupted", ex));
                    }
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

    public static class RequiredServiceDescription {
        private final Message serviceState;
        private final ServiceType serviceType;

        public RequiredServiceDescription(Message serviceState, ServiceType serviceType) {
            this.serviceState = serviceState;
            this.serviceType = serviceType;
        }

        public Message getServiceState() {
            return serviceState;
        }

        public ServiceType getServiceType() {
            return serviceType;
        }
    }

    class RequiredActionObserver implements Observer<ServiceStateProvider<Message>, Message>, Shutdownable {

        private final Logger LOGGER = LoggerFactory.getLogger(RequiredActionObserver.class);

        private static final ServiceJSonProcessor JSON_PROCESSOR = new ServiceJSonProcessor();

        private final HashMap<UnitRemote<?>, RequiredServiceDescription> unitAndRequiredServiceStateMap;
        private final Action responsibleAction;

        private boolean destroy = false;

        private RequiredActionObserver(final List<ActionReference> requiredActionImpact, final Action responsibleAction) {
            this.responsibleAction = responsibleAction;
            this.unitAndRequiredServiceStateMap = new HashMap<>();

            // load units to observe
            for (ActionReference actionReference : requiredActionImpact) {
                try {
                    // unpack service state
                    final Message requiredServiceState = JSON_PROCESSOR.deserialize(actionReference.getServiceStateDescription().getServiceState(), actionReference.getServiceStateDescription().getServiceStateClassName());

                    // load unit
                    unitAndRequiredServiceStateMap.put(Units.getUnit(actionReference.getServiceStateDescription().getUnitId(), false), new RequiredServiceDescription(requiredServiceState, actionReference.getServiceStateDescription().getServiceType()));
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not observe action impact!", ex, LOGGER, LogLevel.WARN);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // register observation
            for (Entry<UnitRemote<?>, RequiredServiceDescription> unitActionReferenceEntry : unitAndRequiredServiceStateMap.entrySet()) {
                try {
                    unitActionReferenceEntry.getKey().addServiceStateObserver(ServiceTempus.CURRENT, unitActionReferenceEntry.getValue().getServiceType(), this);
                    unitActionReferenceEntry.getKey().addServiceStateObserver(ServiceTempus.REQUESTED, unitActionReferenceEntry.getValue().getServiceType(), this);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not observe service state of action impact!", ex, LOGGER, LogLevel.WARN);
                }
            }

            // wait at least some time until all actions are executing.
            final TimeoutSplitter timeout = new TimeoutSplitter(5, TimeUnit.SECONDS);
            for (ActionReference actionReference : requiredActionImpact) {
                try {
                    new RemoteAction(actionReference).waitForActionState(ActionState.State.EXECUTING, timeout.getTime(), timeout.getTimeUnit());
                } catch (CouldNotPerformException e) {
                    // ignore if this action can not be checks since the validation will at least check its state.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // then start the initial verification.
            verifyAllStates();
        }

        private void verifyAllStates() {
            try {
                logger.trace(() -> "verify "+unitAndRequiredServiceStateMap.entrySet().size()+ " states of "+ getLabel("?"));
                for (Entry<UnitRemote<? extends Message>, RequiredServiceDescription> unitActionReferenceEntry : unitAndRequiredServiceStateMap.entrySet()) {
                    try {
                        // skip unit in case its offline, since then the verification is automatically
                        // performed when its back online but those unnecessary timeouts are avoided.
                        if (unitActionReferenceEntry.getKey().isConnected()) {
                            continue;
                        }
                        verifyState(unitActionReferenceEntry.getKey(), unitActionReferenceEntry.getKey().getServiceState(unitActionReferenceEntry.getValue().getServiceType()));
                    } catch (NotAvailableException ex) {
                        ExceptionPrinter.printHistory("Could not inform about unsatisfied state!", ex, LOGGER, LogLevel.DEBUG);
                    }
                }
            } catch (VerificationFailedException ex) {
                unsatisfiedState();
            }
        }

        private void verifyState(final ServiceProvider<? extends Message> unit, final Message serviceState) throws VerificationFailedException {

            // skip verification on destroyed required action observer!
            if(destroy) {
                return;
            }

            if (!responsibleAction.isValid()) {
                throw new VerificationFailedException("The activation of " + getLabel("?") + " is not valid anymore.");
            }

            // skip in case no service state was delivered
            if(serviceState.toString().isBlank()) {
                return;
            }

            if (!Services.equalServiceStates(unitAndRequiredServiceStateMap.get(unit).getServiceState(), serviceState)) {
                logger.trace(() -> unitAndRequiredServiceStateMap.get(unit).getServiceState() + " is not equals " + serviceState.toString().substring(0, 20) + " and will cancel: " + SceneControllerImpl.this.getLabel("?"));
                if(Actions.validateInitialAction(serviceState)) {
                    throw new VerificationFailedException("State of " + unit + "not meet!");
                }
            }
        }

        private void unsatisfiedState() {
            shutdown();
            try {
                if (responsibleAction.isValid()) {
                    responsibleAction.cancel();
                }
            } catch (Exception ex) {
                ExceptionPrinter.printHistory("Could not inform about unsatisfied state!", ex, LOGGER, LogLevel.WARN);
            }
        }

        @Override
        public void update(ServiceStateProvider<Message> serviceStateProvider, Message serviceState) throws Exception {
            try {
                verifyState(serviceStateProvider.getServiceProvider(), serviceState);
            } catch (VerificationFailedException ex) {
                unsatisfiedState();
            }
        }

        @Override
        public void shutdown() {
            destroy = true;
            // deregister observation
            for (Entry<UnitRemote<?>, RequiredServiceDescription> unitActionReferenceEntry : unitAndRequiredServiceStateMap.entrySet()) {
                unitActionReferenceEntry.getKey().removeServiceStateObserver(ServiceTempus.CURRENT, unitActionReferenceEntry.getValue().getServiceType(), this);
                unitActionReferenceEntry.getKey().removeServiceStateObserver(ServiceTempus.REQUESTED, unitActionReferenceEntry.getValue().getServiceType(), this);
            }
        }
    }
}
