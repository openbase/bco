package org.openbase.bco.dal.remote.action;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Initializable;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionParameterType.ActionParameter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class RemoteAction implements Action, Initializable<ActionParameter> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteAction.class);
    private final SyncObject executionSync = new SyncObject(RemoteAction.class);
    private final Unit<?> executorUnit;
    private ActionParameter.Builder actionParameterBuilder;
    private Future<ActionDescription> actionFuture;
    private Unit<?> targetUnit;

    public RemoteAction(final Unit executorUnit) {
        this.executorUnit = executorUnit;
    }

    @Override
    public void init(final ActionParameter actionParameter) throws InitializationException, InterruptedException {
        this.actionParameterBuilder = actionParameter.toBuilder();
        try {
            // setup initiator
            this.actionParameterBuilder.getActionInitiatorBuilder().setInitiatorId(executorUnit.getId());

            // prepare target unit
            this.targetUnit = Units.getUnit(actionParameter.getServiceStateDescription().getUnitId(), false);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public Future<ActionDescription> execute(final ActionDescription causeActionDescription) throws CouldNotPerformException {
        synchronized (executionSync) {
            actionParameterBuilder.setCause(causeActionDescription);
            return execute();
        }
    }

    @Override
    public Future<ActionDescription> execute() throws CouldNotPerformException {
        synchronized (executionSync) {
            actionFuture = targetUnit.applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(actionParameterBuilder).build());
            actionFuture.notifyAll();
            return actionFuture;
        }
    }

    public void waitForFinalization() throws CouldNotPerformException, InterruptedException {
        Future currentExecution;
        synchronized (executionSync) {
            if (actionFuture == null) {
                throw new InvalidStateException("No execution running!");
            }
            currentExecution = actionFuture;
        }

        try {
            currentExecution.get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not wait for execution!", ex);
        }
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     */
    @Override
    public ActionDescription getActionDescription() throws NotAvailableException {
        try {
            if (actionFuture == null || !actionFuture.isDone()) {
                throw new NotAvailableException("ActionFuture");
            }
            return actionFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new NotAvailableException("ActionDescription");
        }
    }

    @Override
    public long getCreationTime() throws NotAvailableException {
        return getActionDescription().getTimestamp().getTime();
    }

    @Override
    public Future<ActionDescription> cancel() {
        try {
            if(!actionFuture.isDone()) {
                actionFuture.cancel(true);
            }
            return targetUnit.cancelAction(getActionDescription());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ex);
        }
    }

    @Override
    public void schedule() {
        // todo: remove from interface
    }

    @Override
    public void waitUntilFinish() throws InterruptedException {
        return;
        // todo redefine
        // semantic changed on remote interface.
        // wait for action not wait for execution

//        synchronized (executionSync) {
//            if(actionFuture == null) {
//                actionFuture.wait();
//            }
//            try {
//                actionFuture.get();
//            } catch (ExecutionException e) {
//                // failed but still finished
//            }
//
//
//            if()
//            // wait until done
//        }
    }

    public Future<ActionDescription> getActionFuture() throws NotAvailableException {
        if(actionFuture == null) {
            throw new NotAvailableException("Future<ActionDescription>");
        }
        return actionFuture;
    }

    @Override
    public String toString() {
        return Action.toString(this);
    }
}
