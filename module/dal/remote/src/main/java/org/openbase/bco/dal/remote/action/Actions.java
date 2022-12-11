package org.openbase.bco.dal.remote.action;

/*-
 * #%L
 * BCO DAL Remote
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
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class Actions {

    public static RemoteAction waitForExecution(final Future<ActionDescription> actionFuture) throws CouldNotPerformException, InterruptedException {
        return waitForActionState(actionFuture, State.EXECUTING);
    }

    public static RemoteAction waitForExecution(final Future<ActionDescription> actionFuture, final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException, TimeoutException {
        return waitForActionState(actionFuture, State.EXECUTING, timeout, timeUnit);
    }

    public static RemoteAction waitForActionState(final Future<ActionDescription> actionFuture, final ActionState.State actionState) throws CouldNotPerformException, InterruptedException {
        final RemoteAction remoteAction = new RemoteAction(actionFuture);
        remoteAction.waitForActionState(actionState);
        return remoteAction;
    }

    public static RemoteAction waitForActionState(final Future<ActionDescription> actionFuture, final ActionState.State actionState, final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException, TimeoutException {
        final RemoteAction remoteAction = new RemoteAction(actionFuture);
        remoteAction.waitForActionState(actionState, timeout, timeUnit);
        return remoteAction;
    }

    public static boolean validateInitialAction(Message serviceState) {
        try {
            return validateInitialAction(ServiceStateProcessor.getResponsibleAction(serviceState));
        } catch (CouldNotPerformException e) {
            // skip validation is error case.
        }
        return false;
    }

    public static boolean validateInitialAction(ActionDescription actionDescription) {
        try {
            final RemoteAction initialActionOfIncomingServiceState =
                    new RemoteAction(ActionDescriptionProcessor.getInitialActionReference(actionDescription));
            initialActionOfIncomingServiceState.waitForRegistration();
            return initialActionOfIncomingServiceState.isValid();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (CouldNotPerformException e) {
            // skip if validation failed.
        }
        return false;
    }
}
