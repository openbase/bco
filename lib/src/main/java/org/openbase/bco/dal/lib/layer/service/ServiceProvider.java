
package org.openbase.bco.dal.lib.layer.service;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Identifiable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameterOrBuilder;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface ServiceProvider<ST extends Message> extends Identifiable<String> {

    /**
     * Method applies the action on this instance.
     *
     * @param actionDescription the description of the action.
     *
     * @return a future which gives feedback about the action execution state.
     */
    Future<ActionDescription> applyAction(final ActionDescription actionDescription);

    @RPCMethod
    Future<AuthenticatedValue> applyActionAuthenticated(final AuthenticatedValue authenticatedValue);

    /**
     * Method applies the action on this instance.
     *
     * @param actionDescriptionBuilder the description builder of the action.
     *
     * @return a future which gives feedback about the action execution state.
     */
    default Future<ActionDescription> applyAction(final ActionDescription.Builder actionDescriptionBuilder) {
        return applyAction(actionDescriptionBuilder.build());
    }

    /**
     * Method applies the action on this instance.
     *
     * @param actionParameter the needed parameters to generate a new action.
     *
     * @return a future which gives feedback about the action execution state.
     */
    @RPCMethod(legacy = true)
    default Future<ActionDescription> applyAction(final ActionParameterOrBuilder actionParameter) {
        try {
            final ActionDescription.Builder actionDescriptionBuilder = ActionDescriptionProcessor.generateActionDescriptionBuilder(actionParameter);
            if (actionDescriptionBuilder.getServiceStateDescriptionBuilder().getUnitId().isEmpty()) {
                actionDescriptionBuilder.getServiceStateDescriptionBuilder().setUnitId(getId());
            }
            if (SessionManager.getInstance().isLoggedIn() && (actionParameter.hasAuthToken())) {
                final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(actionDescriptionBuilder.build(), actionParameter.getAuthToken());
                final Future<AuthenticatedValue> future = applyActionAuthenticated(authenticatedValue);
                return new AuthenticatedValueFuture<>(future, ActionDescription.class, authenticatedValue.getTicketAuthenticatorWrapper(), SessionManager.getInstance());
            } else {
                return applyAction(actionDescriptionBuilder.build());
            }
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    /**
     * Method enables the registration of an observer which is notified in case the service state changes.
     *
     * @param serviceType the type of service to observe.
     * @param observer    the observer to inform about changes.
     */
    void addServiceStateObserver(final ServiceType serviceType, final Observer<ServiceStateProvider<ST>, ST> observer) throws CouldNotPerformException;

    /**
     * Method remove an already registered service state observer.
     * The call just returns without any action in case the given observer was never registered.
     *
     * @param serviceType the service type where the observer was registered on.
     * @param observer    the observer to remove.
     */
    void removeServiceStateObserver(final ServiceType serviceType, final Observer<ServiceStateProvider<ST>, ST> observer);

    /**
     * Method returns the current service state of the referred {@code service type}.
     *
     * @param serviceType the type to define the service.
     *
     * @return the current service state.
     *
     * @throws NotAvailableException is thrown if the current service state is unknown.
     */
    ST getServiceState(final ServiceType serviceType) throws NotAvailableException;
}
