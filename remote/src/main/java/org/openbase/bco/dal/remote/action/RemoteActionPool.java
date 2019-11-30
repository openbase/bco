package org.openbase.bco.dal.remote.action;

/*-
 * #%L
 * BCO DAL Remote
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

import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.exception.TimeoutException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.MultiFuture;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescriptionOrBuilder;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.unit.scene.SceneDataType.SceneData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RemoteActionPool {

    public static final long ACTION_EXECUTION_TIMEOUT = 3000;

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescriptionType.ActionDescription.getDefaultInstance()));
    }

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

        // todo: handle more robust by list diff and cancel removed actions

        synchronized (actionListSync) {
            remoteActionList.clear();
            RemoteAction action;
            for (ActionParameter actionParameter : actionParameters) {
                try {
                    action = new RemoteAction(unit, actionParameter, autoExtendCheckCallback);
                    remoteActionList.add(action);
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
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
     * @return a future object referring the submission state.
     */
    public MultiFuture<ActionDescription> execute() {
        return execute(null, false);
    }

    /**
     * Executes all previously registered actions within this pool while the given {@code causeActionDescription} is used as cause for each action.
     *
     * @param causeActionDescription refers the causing action of this execution.
     *
     * @return a future object referring the submission state.
     */
    public MultiFuture<ActionDescription> execute(final ActionDescriptionOrBuilder causeActionDescription) {
        return execute(causeActionDescription, false);
    }

    /**
     * Executes all previously registered actions within this pool while the given {@code causeActionDescription} is used as cause for each action.
     *
     * @param causeActionDescription       refers the causing action of this execution.
     * @param cancelSubmissionAfterTimeout If true, the submission of each action is verified and canceled after a timeout of {@code RemoteActionPool.ACTION_EXECUTION_TIMEOUT}.
     *
     * @return a future object referring the submission state.
     */
    public MultiFuture<ActionDescription> execute(final ActionDescriptionOrBuilder causeActionDescription, final boolean cancelSubmissionAfterTimeout) {
        final List<Future<ActionDescription>> submissionFutureList = new ArrayList<>();

        synchronized (actionListSync) {
            if (remoteActionList.isEmpty()) {
                LOGGER.debug("Remote action pool is empty, skip execution...");
            }

            for (final RemoteAction action : remoteActionList) {
                submissionFutureList.add(action.execute(causeActionDescription, true));
            }
        }

        // handle auto cancellation after timeout
        if (cancelSubmissionAfterTimeout) {
            GlobalCachedExecutorService.submit(() -> {
                try {
                    LOGGER.info("Waiting for action submission...");

                    long checkStart = System.currentTimeMillis() + ACTION_EXECUTION_TIMEOUT;
                    long timeout;
                    for (final RemoteAction action : remoteActionList) {
                        if (action.isDone()) {
                            continue;
                        }
                        LOGGER.info("Waiting for action submission[" + action + "]");
                        try {
                            timeout = checkStart - System.currentTimeMillis();
                            if (timeout <= 0) {
                                throw new RejectedException("Rejected because of scene timeout.");
                            }

                            action.waitForSubmission(timeout, TimeUnit.MILLISECONDS);
                        } catch (TimeoutException ex) {
                            // will be handled anyway
                        }
                    }
                } catch (CouldNotPerformException | CancellationException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Remote pool action execution failed!", ex), LOGGER);
                } finally {
                    // cancel all actions which could not be submitted
                    for (final RemoteAction action : remoteActionList) {
                        if (!action.isSubmissionDone()) {
                            action.cancel();
                        }
                    }
                }
                return null;
            });
        }

        return new MultiFuture<>(submissionFutureList);
    }

    public List<RemoteAction> getRemoteActionList() {
        return Collections.unmodifiableList(remoteActionList);
    }

    public void stop() {
        for (final RemoteAction action : remoteActionList) {
            if (action.isRunning()) {
                action.cancel();
            }
        }
    }

    public void waitUntilDone() throws CouldNotPerformException, InterruptedException {
        for (final RemoteAction action : remoteActionList) {
            action.waitUntilDone();
        }
    }

    public void waitForSubmission() throws CouldNotPerformException, InterruptedException {
        for (final RemoteAction action : remoteActionList) {
            action.waitForSubmission();
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
}
