package org.openbase.bco.dal.control.layer.unit;

/*-
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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UnitUserCreationPlugin;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.processing.ProtoBufJSonProcessor;
import org.openbase.jul.pattern.Pair;
import org.openbase.jul.schedule.CloseableWriteLockWrapper;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.*;

public abstract class AbstractAuthorizedBaseUnitController<D extends AbstractMessage & Serializable, DB extends D.Builder<DB>> extends AbstractExecutableBaseUnitController<D, DB> {

    private ActionParameter defaultActionParameter;
    private AuthToken authToken = null;

    private ArrayList<RemoteAction> observedTaskList;

    private final static ProtoBufJSonProcessor protoBufJSonProcessor = new ProtoBufJSonProcessor();

    public AbstractAuthorizedBaseUnitController(DB builder) throws InstantiationException {
        super(builder);
        this.observedTaskList = new ArrayList<>();
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        try (final CloseableWriteLockWrapper ignored = getManageWriteLockInterruptible(this)) {
            // update default action parameter
            if (authToken == null) {
                authToken = requestAuthToken(config);
            }

            defaultActionParameter = getActionParameterTemplate(config)
                    .setAuthToken(authToken)
                    .setActionInitiator(ActionInitiator.newBuilder().setInitiatorId(config.getId()))
                    .build();

            return super.applyConfigUpdate(config);
        }
    }

    protected abstract ActionParameter.Builder getActionParameterTemplate(final UnitConfig config) throws InterruptedException, CouldNotPerformException;

    private AuthToken requestAuthToken(final UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        try {
            final UnitConfig userUnitConfig = UnitUserCreationPlugin.findUser(unitConfig.getId(), Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.USER));
            final AuthenticationToken authenticationToken = AuthenticationToken.newBuilder().setUserId(userUnitConfig.getId()).build();
            final SessionManager sessionManager = new SessionManager();
            sessionManager.loginUser(userUnitConfig.getId(), true);
            final AuthenticatedValue authenticatedValue = sessionManager.initializeRequest(authenticationToken, null);
            return AuthToken.newBuilder().setAuthenticationToken(new AuthenticatedValueFuture<>(
                    Registries.getUnitRegistry().requestAuthenticationTokenAuthenticated(authenticatedValue),
                    String.class,
                    authenticatedValue.getTicketAuthenticatorWrapper(),
                    sessionManager).get()).build();
        } catch (CouldNotPerformException | ExecutionException ex) {
            throw new CouldNotPerformException("Could not create authentication token for " + this + " " + UnitConfigProcessor.getDefaultAlias(unitConfig, unitConfig.getId()), ex);
        }
    }

    protected ActionParameter.Builder generateAction(final UnitType unitType, final ServiceType serviceType, final Message.Builder serviceStateBuilder) throws CouldNotPerformException {
        try {
            return defaultActionParameter.toBuilder()
                    .setServiceStateDescription(ServiceStateDescription.newBuilder()
                            .setServiceType(serviceType)
                            .setUnitType(unitType)
                            .setServiceStateClassName(serviceStateBuilder.build().getClass().getName())
                            .setServiceState(protoBufJSonProcessor.serialize(serviceStateBuilder)));
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate action!", ex);
        }
    }

    protected ActionParameter getDefaultActionParameter() {
        try {
            final Action currentAction = getCurrentAction();

            // ignore initiating action in case its outdated.
            if (currentAction.getLifetime() > TimeUnit.MINUTES.toMillis(30)) {
                return defaultActionParameter;
            }
            return defaultActionParameter.toBuilder().setCause(currentAction.getActionDescription()).setReplaceable(false).build();
        } catch (NotAvailableException e) {
            // cause not available
            return defaultActionParameter;
        }
    }

    protected ActionParameter getDefaultActionParameter(final long executionTimePeriod) {
        return getDefaultActionParameter().toBuilder().setExecutionTimePeriod(executionTimePeriod).build();
    }

    protected AuthToken getToken() {
        return authToken;
    }

    /**
     * Method creates a new {@code RemoteAction} which observes the action represented by the {@code futureAction}.
     * Additionally, the auth token of this controller is passed to the remote action and the action auto extension routine is enabled.
     *
     * @param futureAction used to identify the action to observe.
     * @return a ready to use action remote instance.
     */
    protected RemoteAction observe(final Future<ActionDescription> futureAction) {

        if (!isValid()) {
            new FatalImplementationErrorException(getLabel(getClass().getSimpleName()) + " observes action state even when not active!", this);
        }

        // generate remote action
        final RemoteAction remoteAction = new RemoteAction(futureAction, getToken(), () -> isValid());

        // cleanup done actions
        for (RemoteAction action : new ArrayList<>(observedTaskList)) {
            if (action.isDone()) {
                observedTaskList.remove(action);
            }
        }

        // register new action
        observedTaskList.add(remoteAction);

        // validate and initiate forced stop if instance generates to many messages.
        validateUnitOverload();

        return remoteAction;
    }

    protected boolean isValid() {
        try {
            return isEnabled() && isActive() && getActivationState().getValue() == State.ACTIVE;
        } catch (NotAvailableException e) {
            return false;
        }
    }


    public static long MAX_ACTIN_SUBMITTION_PER_MINUTE = 30;
    private double eventsPerHour = 0;
    private long lastActionTimestamp = System.currentTimeMillis();

    private synchronized void validateUnitOverload() {
        eventsPerHour -= (((double) (System.currentTimeMillis() - lastActionTimestamp)) / 1000d / 60d) * MAX_ACTIN_SUBMITTION_PER_MINUTE;
        eventsPerHour = Math.max(0, eventsPerHour);
        eventsPerHour++;
        lastActionTimestamp = System.currentTimeMillis();

        if (JPService.verboseMode()) {
            logger.info("Analyze " + this + " which currently generates " + eventsPerHour + " events per hour which is " + (int) (eventsPerHour / MAX_ACTIN_SUBMITTION_PER_MINUTE) * 100d + "% of the totally allowed ones.");
        }

        if (eventsPerHour > MAX_ACTIN_SUBMITTION_PER_MINUTE * 60) {
            logger.error(this + " generates to many actions and will be terminated!");
            try {
                applyServiceState(ActivationState.newBuilder().setValue(State.INACTIVE), ServiceType.ACTIVATION_STATE_SERVICE);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not force shutdown " + this, ex, logger);
            }
        }
    }

    @Override
    protected void stop(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {

        cancelAllObservedActions();

        // cleanup actions
        for (RemoteAction action : new ArrayList<>(observedTaskList)) {
            action.reset();
            observedTaskList.remove(action);
        }
    }

    protected void cancelAllObservedActions() throws InterruptedException {
        final ArrayList<Pair<RemoteAction, Future<ActionDescription>>> cancelTaskList = new ArrayList<>();
        for (final RemoteAction remoteAction : observedTaskList) {
            final Future<ActionDescription> cancel = remoteAction.cancel();
            if (cancel == null) {
                new FatalImplementationErrorException("null task in observer list", this);
                continue;
            }
            cancelTaskList.add(new Pair<>(remoteAction, cancel));
        }

        final long timeout = (isShutdownInProgress() ? 2 : 10);

        for (final Pair<RemoteAction, Future<ActionDescription>> remoteTaskPair : cancelTaskList) {
            try {
                remoteTaskPair.getValue().get(timeout, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException | CancellationException ex) {
                ExceptionPrinter.printHistory("Could not cancel " + remoteTaskPair.getKey() + "!", ex, logger, LogLevel.WARN);
            }
        }
    }
}
