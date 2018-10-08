package org.openbase.bco.dal.remote.action;

/*-
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

import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionDescriptionType;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionParameterType.ActionParameter;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.unit.scene.SceneDataType.SceneData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RemoteActionPool {

    public static final long ACTION_EXECUTION_TIMEOUT = 15000;

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

    public void initViaServiceStateDescription(final List<ServiceStateDescription> serviceStateDescriptions) throws CouldNotPerformException, InterruptedException {
        List<ActionParameter> actionParameters = new ArrayList<>();
        for (ServiceStateDescription serviceStateDescription : serviceStateDescriptions) {
            actionParameters.add(ActionParameter.newBuilder().setServiceStateDescription(serviceStateDescription).build());
        }
        init(actionParameters);
    }

    public void init(final List<ActionParameter> actionParameters) throws CouldNotPerformException, InterruptedException {

        MultiException.ExceptionStack exceptionStack = null;

        // todo: handle more robust by list diff and cancel removed actions

        synchronized (actionListSync) {
            remoteActionList.clear();
            RemoteAction action;
            for (ActionParameter actionParameter : actionParameters) {
                try {
                    action = new RemoteAction(unit, actionParameter);
                    remoteActionList.add(action);
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
        }

        try {
            MultiException.checkAndThrow(() -> "Could not fully init action remotes of " + this, exceptionStack);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
        }
    }

    public void execute(final ActionDescription causeActionDescription) throws CouldNotPerformException, InterruptedException {

        synchronized (actionListSync) {

            if (remoteActionList.isEmpty()) {
                LOGGER.warn("Remote action pool is empty, skip execution...");
            }

            for (final RemoteAction action : remoteActionList) {
                action.execute(causeActionDescription);
            }
        }

        MultiException.ExceptionStack exceptionStack = null;

        try {
            LOGGER.info("Waiting for action finalisation...");

            long checkStart = System.currentTimeMillis() + ACTION_EXECUTION_TIMEOUT;
            long timeout;
            for (final RemoteAction action : remoteActionList) {
                if (action.isDone()) {
                    continue;
                }
                LOGGER.info("Waiting for action [" + action.getActionDescription().getServiceStateDescription().getServiceAttributeType() + "]");
                try {
                    timeout = checkStart - System.currentTimeMillis();
                    if (timeout <= 0) {
                        throw new RejectedException("Rejected because of scene timeout.");
                    }
                    action.getActionFuture().get(timeout, TimeUnit.MILLISECONDS);
                } catch (ExecutionException | TimeoutException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
        } catch (CouldNotPerformException | CancellationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Remote pool action execution failed!", ex), LOGGER);
        } finally {
            for (final RemoteAction action : remoteActionList) {
                if (!action.getActionFuture().isDone()) {
                    action.cancel();
                }
            }
        }
    }

    public void stop() {
        for (final RemoteAction action : remoteActionList) {
            if (action.isRunning()) {
                action.cancel();
            }
        }
    }
}
