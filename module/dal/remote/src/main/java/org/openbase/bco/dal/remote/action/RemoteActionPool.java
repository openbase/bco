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

import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.TimeoutException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.schedule.TimeoutSplitter;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescriptionOrBuilder;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RemoteActionPool {

    protected final Logger LOGGER = LoggerFactory.getLogger(RemoteActionPool.class);

    private final List<RemoteAction> remoteActionList;
    private final SyncObject actionListSync = new SyncObject("ActionListSync");
    private final Unit<?> unit;

    public RemoteActionPool(final Unit<?> unit) {
        this.remoteActionList = new ArrayList<>();
        this.unit = unit;
    }

    public void initViaServiceStateDescription(final List<ServiceStateDescription> serviceStateDescriptions, final ActionParameter actionParameterPrototype, final Callable<Boolean> autoExtendCheckCallback) throws CouldNotPerformException, InterruptedException {
        List<ActionParameter> actionParameters = new ArrayList<>();
        for (ServiceStateDescription serviceStateDescription : serviceStateDescriptions) {
            actionParameters.add(actionParameterPrototype.toBuilder().setServiceStateDescription(serviceStateDescription).build());
        }
        init(actionParameters, autoExtendCheckCallback);
    }

    public void init(final List<ActionParameter> actionParameters, Callable<Boolean> autoExtendCheckCallback) throws CouldNotPerformException, InterruptedException {

        MultiException.ExceptionStack exceptionStack = null;
        synchronized (actionListSync) {

            // rebuild list
            final List<RemoteAction> newRemoteActionList = new ArrayList<>();
            for (ActionParameter actionParameter : actionParameters) {
                try {
                    boolean newAction = true;

                    // lookup from existing remotes
                    for (RemoteAction remoteAction : new ArrayList<>(remoteActionList)) {

                        // if match with existing one then recover it
                        if (remoteAction.getActionParameter().getServiceStateDescription().equals(actionParameter.getServiceStateDescription())) {
                            newRemoteActionList.add(remoteAction);
                            remoteActionList.remove(remoteAction);
                            newAction = false;
                        }
                    }

                    // create new actions
                    if (newAction) {
                        newRemoteActionList.add(new RemoteAction(unit, actionParameter, autoExtendCheckCallback));
                    }
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }

            // cancel still ongoing actions that could not be remapped.
            for (RemoteAction remoteAction : remoteActionList) {
                remoteAction.cancel();
            }

            // update list
            remoteActionList.clear();
            remoteActionList.addAll(newRemoteActionList);
        }

        try {
            MultiException.checkAndThrow(() -> "Could not fully init action remotes of " + unit, exceptionStack);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
        }
    }

    /**
     * Executes all previously registered actions within this pool while the given {@code causeActionDescription} is used as cause for each action.
     *
     * @return the entire list of remote actions that has been executed.
     */
    public List<RemoteAction> execute() {
        return execute(null);
    }

    /**
     * Executes all previously registered actions within this pool while the given {@code causeActionDescription} is used as cause for each action.
     *
     * @param causeActionDescription refers the causing action of this execution.
     *
     * @return the entire list of remote actions that has been executed.
     */
    public List<RemoteAction> execute(final ActionDescriptionOrBuilder causeActionDescription) {

        synchronized (actionListSync) {
            if (remoteActionList.isEmpty()) {
                LOGGER.debug("Remote action pool is empty, skip execution...");
            }

            for (final RemoteAction action : remoteActionList) {
                action.execute(causeActionDescription, true);
            }

            return new ArrayList<>(remoteActionList);
        }
    }

    public List<RemoteAction> getRemoteActionList() {
        return Collections.unmodifiableList(remoteActionList);
    }

    /**
     * @deprecated Please use the cancel() method instead.
     */
    @Deprecated
    public void stop() {
        synchronized (actionListSync) {
            for (final RemoteAction action : remoteActionList) {
                if (action.isRunning()) {
                    action.cancel();
                }
            }
        }
    }

    public Map<RemoteAction, Future<ActionDescription>> cancel() {
        final HashMap<RemoteAction, Future<ActionDescription>> remoteActionActionDescriptionFutureMap = new HashMap<>();
        synchronized (actionListSync) {
            for (final RemoteAction action : remoteActionList) {
                if (action.isRunning()) {
                    remoteActionActionDescriptionFutureMap.put(action, action.cancel());
                }
            }
        }
        return remoteActionActionDescriptionFutureMap;
    }


    public void waitUntilDone() throws CouldNotPerformException, InterruptedException {
        for (final RemoteAction action : remoteActionList) {
            action.waitUntilDone();
        }
    }

    public void waitForRegistration() throws CouldNotPerformException, InterruptedException {
        for (final RemoteAction action : remoteActionList) {
            action.waitForRegistration();
        }
    }

    public void addActionDescriptionObserver(final Observer<RemoteAction, ActionDescription> observer) {
        for (final RemoteAction action : remoteActionList) {
            action.addActionDescriptionObserver(observer);
        }
    }

    public void removeActionDescriptionObserver(final Observer<RemoteAction, ActionDescription> observer) {
        for (final RemoteAction action : remoteActionList) {
            action.removeActionDescriptionObserver(observer);
        }
    }

    public static void observeCancellation(final Map<RemoteAction, Future<ActionDescription>> remoteActionActionDescriptionFutureMap, final Object responsibleInstance, final long timeout, final TimeUnit timeUnit) throws MultiException {

        final TimeoutSplitter timeoutSplitter = new TimeoutSplitter(timeout, timeUnit);

        // validate cancellation
        MultiException.ExceptionStack exceptionStack = null;
        for (Map.Entry<RemoteAction, Future<ActionDescription>> remoteActionFutureEntry : remoteActionActionDescriptionFutureMap.entrySet()) {

            try {
                remoteActionFutureEntry.getValue().get(timeoutSplitter.getTime(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException | TimeoutException | java.util.concurrent.TimeoutException ex) {
                exceptionStack = MultiException.push(responsibleInstance, ex, exceptionStack);
            }
        }

        // check if something went wrong
        MultiException.checkAndThrow(() -> "Could not cancel all action of " + responsibleInstance, exceptionStack);
    }
}
