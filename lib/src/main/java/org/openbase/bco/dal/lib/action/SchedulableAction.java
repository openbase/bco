package org.openbase.bco.dal.lib.action;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.iface.Executable;
import org.openbase.jul.iface.Initializable;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionReferenceType.ActionReference;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface SchedulableAction extends Action, Executable<ActionDescription>, Initializable<ActionDescription> {

    /**
     * Mark this action as currently scheduled.
     */
    void schedule();

    /**
     * Reject this action.
     */
    void reject();

    /**
     * Finish this action.
     */
    void finish();

    /**
     * Abort this action.
     *
     * @param forceReject forces an rejection after abortion.
     */
    Future<ActionDescription> abort(boolean forceReject);

    /**
     * Abort this action.
     */
    default Future<ActionDescription> abort() {
        return abort(false);
    }

    /**
     * {@inheritDoc}
     *
     * @param timeUnit {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    default long getExecutionTimePeriod(final TimeUnit timeUnit) {
        // overwritten to remove the could not perform exception.
        return timeUnit.convert(getActionDescription().getExecutionTimePeriod(), TimeUnit.MICROSECONDS);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    ActionDescription getActionDescription();

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    default State getActionState() {
        return getActionDescription().getActionState().getValue();
    }

    @Override
    default String getId() {
        return getActionDescription().getActionId();
    }

    /**
     * Method can be used to auto extend this action if the flag is set and the action was initiated by a human.
     *
     * @throws VerificationFailedException is thrown if the action is not compatible to be extended.
     */
    void autoExtendWithLowPriority() throws VerificationFailedException;

    /**
     * Method detects if this action or any cause is intended to be automatically continued with low priority in case this human action gets invalid.
     *
     * @return true if the action should be extended, otherwise fales.
     */
    default boolean isAutoContinueWithLowPriorityIntended() {
        final ActionDescription actionDescription = getActionDescription();
        if (actionDescription.getAutoContinueWithLowPriority()) {
            return true;
        }

        for (ActionReference cause : actionDescription.getActionCauseList()) {
            if (cause.getAutoContinueWithLowPriority()) {
                return true;
            }
        }
        return false;
    }
}
