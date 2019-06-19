package org.openbase.bco.dal.control.layer.unit;

/*-
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UnitUserCreationPlugin;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.processing.ProtoBufJSonProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.pattern.Observer;
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
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    protected abstract ActionParameter.Builder getActionParameterTemplate(final UnitConfig config) throws InterruptedException, CouldNotPerformException;

    private AuthToken requestAuthToken(final UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        try {
            UnitConfig userUnitConfig = null;
            for (final UnitConfig config : Registries.getUnitRegistry().getUnitConfigs(UnitType.USER)) {
                MetaConfigPool metaConfigPool = new MetaConfigPool();
                metaConfigPool.register(new MetaConfigVariableProvider(UnitConfigProcessor.getDefaultAlias(config, config.getId()), config.getMetaConfig()));
                try {
                    String unitId = metaConfigPool.getValue(UnitUserCreationPlugin.UNIT_ID_KEY);
                    if (unitId.equalsIgnoreCase(unitConfig.getId())) {
                        userUnitConfig = config;
                        break;
                    }
                } catch (NotAvailableException ex) {
                    // do nothing
                }
            }

            if (userUnitConfig == null) {
                throw new NotAvailableException("User for " + this + " " + UnitConfigProcessor.getDefaultAlias(unitConfig, unitConfig.getId()));
            }

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

    protected ActionParameter.Builder generateAction(final UnitType unitType, final ServiceType serviceType, final Message.Builder serviceArgumentBuilder) throws CouldNotPerformException {
        final Message serviceArgument = serviceArgumentBuilder.build();
        try {
            return defaultActionParameter.toBuilder()
                    .setServiceStateDescription(ServiceStateDescription.newBuilder()
                            .setServiceType(serviceType)
                            .setUnitType(unitType)
                            .setServiceStateClassName(protoBufJSonProcessor.getServiceStateClassName(serviceArgument))
                            .setServiceState(protoBufJSonProcessor.serialize(serviceArgument)));
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate action!", ex);
        }
    }

    protected ActionParameter getDefaultActionParameter() {
        return defaultActionParameter;
    }

    protected ActionParameter getDefaultActionParameter(final long executionTimePeriod) {
        return defaultActionParameter.toBuilder().setExecutionTimePeriod(executionTimePeriod).build();
    }

    protected AuthToken getToken() {
        return authToken;
    }

    /**
     * Method creates a new {@code RemoteAction} which observes the action represented by the {@code futureAction}.
     * Additionally, the auth token of this controller is passed to the remote action and the action auto extension routine is enabled.
     *
     * @param futureAction used to identify the action to observe.
     *
     * @return a ready to use action remote instance.
     */
    protected RemoteAction observe(final Future<ActionDescription> futureAction) {

        // generate remote action
        final RemoteAction remoteAction = new RemoteAction(futureAction, getToken(), () -> isEnabled() && isActive() && getActivationState().getValue() == State.ACTIVE);

        // cleanup done actions
        for (RemoteAction action : new ArrayList<>(observedTaskList)) {
            if (action.isDone()) {
                observedTaskList.remove(action);
            }
        }

        // register new action
        observedTaskList.add(remoteAction);

        return remoteAction;
    }


    @Override
    protected void stop(ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        final ArrayList<Future<ActionDescription>> cancelTaskList = new ArrayList<>();
        for (RemoteAction remoteAction : observedTaskList) {
            final Future<ActionDescription> cancel = remoteAction.cancel();
            if (cancel == null) {
                new FatalImplementationErrorException("null task in observer list", this);
                continue;
            }
            cancelTaskList.add(cancel);
        }

        for (Future<ActionDescription> cancelTask : cancelTaskList) {
            try {
                cancelTask.get(10, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException ex) {
                ExceptionPrinter.printHistory("Could not cancel action!", ex, logger);
            }
            observedTaskList.remove(cancelTask);
        }
    }
}
