package org.openbase.bco.dal.control.action;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.openbase.bco.dal.control.layer.unit.AbstractUnitController;
import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.action.SchedulableAction;
import org.openbase.bco.dal.lib.jp.JPProviderControlMode;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import rst.domotic.state.ActionStateType.ActionState;
import rst.domotic.state.ActionStateType.ActionState.State;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class ActionImpl implements SchedulableAction {

    public static final String INITIATOR_KEY = "$INITIATOR";
    public static final String SERVICE_TYPE_KEY = "$SERVICE_TYPE";
    public static final String UNIT_LABEL_KEY = "$UNIT_LABEL";
    public static final String SERVICE_ATTRIBUTE_KEY = "SERVICE_ATTRIBUTE";
    public static final String GENERIC_ACTION_LABEL = UNIT_LABEL_KEY + "[" + SERVICE_ATTRIBUTE_KEY + "]";

    /**
     * Timeout how long it is waited on execution failure until a rescheduling process is triggered.
     */
    private static final long EXECUTION_FAILURE_TIMEOUT = TimeUnit.SECONDS.toMillis(15);

    public static final Map<String, String> GENERIC_ACTION_DESCRIPTION_MAP = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionImpl.class);

    static {
        GENERIC_ACTION_DESCRIPTION_MAP.put("en", INITIATOR_KEY + " changed " + SERVICE_TYPE_KEY + " of " + UNIT_LABEL_KEY + " to " + SERVICE_ATTRIBUTE_KEY + ".");
        GENERIC_ACTION_DESCRIPTION_MAP.put("de", INITIATOR_KEY + " hat " + SERVICE_TYPE_KEY + "  von " + UNIT_LABEL_KEY + " zu " + SERVICE_ATTRIBUTE_KEY + " geändert.");
    }

    protected final AbstractUnitController<?, ?> unit;
    private final SyncObject executionSync = new SyncObject(ActionImpl.class);
    private final ServiceJSonProcessor serviceJSonProcessor;
    protected ActionDescription.Builder actionDescriptionBuilder;
    private Message serviceState;
    private ServiceDescription serviceDescription;
    private Future<ActionDescription> actionTask;

    public ActionImpl(final ActionDescription actionDescription, final AbstractUnitController<?, ?> unit) throws InstantiationException {
        try {
            this.unit = unit;
            this.serviceJSonProcessor = new ServiceJSonProcessor();
            this.init(actionDescription);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final ActionDescription actionDescription) throws InitializationException {
        LOGGER.info("================================================================================");
        try {
            actionDescriptionBuilder = actionDescription.toBuilder();

            // verify and prepare action description
            serviceState = ActionDescriptionProcessor.verifyActionDescription(actionDescriptionBuilder, unit, true);

            // since its an action it has to be an operation service pattern
            serviceDescription = ServiceDescription.newBuilder().setServiceType(actionDescriptionBuilder.getServiceStateDescription().getServiceType()).setPattern(ServicePattern.OPERATION).build();

            // mark action as initialized.
            updateActionState(State.INITIALIZED);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * returns if there is still an task operating this action.
     *
     * @return true if action is still in progress.
     */
    private boolean isExecuting() {
        synchronized (executionSync) {
            return actionTask != null && !actionTask.isDone() || getActionState() == State.EXECUTING;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActionDescription> execute() {
        synchronized (executionSync) {
            if (isExecuting()) {
                return actionTask;
            }

            actionTask = GlobalCachedExecutorService.submit(() -> {
                try {
//                    synchronized (executionSync) {
                    // Initiate
                    updateActionState(ActionState.State.INITIATING);

                    try {
                        while (!Thread.interrupted()) {
                            try {
                                boolean hasOperationService = false;
                                for (ServiceDescription description : unit.getUnitTemplate().getServiceDescriptionList()) {
                                    if (description.getServiceType() == serviceDescription.getServiceType() && description.getPattern() == ServicePattern.OPERATION) {
                                        hasOperationService = true;
                                        break;
                                    }
                                }

                                // only update requested state if it is an operation state, else throw an exception if not in provider control mode
                                if (!hasOperationService) {
                                    if (!JPService.getProperty(JPProviderControlMode.class).getValue()) {
                                        throw new NotAvailableException("Operation service " + serviceDescription.getServiceType().name() + " of unit " + unit);
                                    }
                                } else {
                                    setRequestedState();
                                }

                                // Execute
                                updateActionState(ActionState.State.EXECUTING);

                                LOGGER.debug("Wait for execution...");
                                waitForExecution(unit.performOperationService(serviceState, serviceDescription.getServiceType()));
                                LOGGER.debug("Execution finished!");
                                break;
                            } catch (CouldNotPerformException | JPNotAvailableException ex) {
                                updateActionState(ActionState.State.EXECUTION_FAILED);
                                ExceptionPrinter.printHistory("Action execution failed", ex, LOGGER, LogLevel.WARN);
                                Thread.sleep(EXECUTION_FAILURE_TIMEOUT);
                            }
                        }
                        return getActionDescription();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
//                    }
                } finally {
                    synchronized (executionSync) {
                        actionTask = null;
                        executionSync.notifyAll();
                    }
                }
            });
            return actionTask;
        }
    }

    public void waitUntilDone() throws InterruptedException {
        synchronized (executionSync) {
            while (!isDone()) {
                executionSync.wait();
            }
        }
    }

    public void waitUntilExecuted() throws InterruptedException {
        synchronized (executionSync) {
            while (isExecuting()) {
                executionSync.wait();
            }
        }
    }

    private void setRequestedState() throws CouldNotPerformException {
        try (ClosableDataBuilder dataBuilder = unit.getDataBuilder(this)) {

            // set the responsible action for the service attribute
            Message.Builder serviceStateBuilder = serviceState.toBuilder();
            Descriptors.FieldDescriptor fieldDescriptor = ProtoBufFieldProcessor.getFieldDescriptor(serviceStateBuilder, Service.RESPONSIBLE_ACTION_FIELD_NAME);
            serviceStateBuilder.setField(fieldDescriptor, actionDescriptionBuilder.build());

            // set the updated service attribute as requested state in the unit data builder
            Services.invokeServiceMethod(serviceDescription.getServiceType(), serviceDescription.getPattern(), ServiceTempus.REQUESTED, dataBuilder.getInternalBuilder(), serviceStateBuilder);
        }
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     */
    @Override
    public ActionDescription getActionDescription() {
        return actionDescriptionBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<ActionDescription> cancel() {
        // if action not executing, set to canceled and finish
        if (!isExecuting()) {
            updateActionState(State.CANCELED);
            return CompletableFuture.completedFuture(getActionDescription());
        }

        // action is currently executing, so set to canceling, wait till its done, set to canceled and trigger reschedule
        updateActionState(State.CANCELING);
        return GlobalCachedExecutorService.submit(() -> {
            if (actionTask != null && !actionTask.isDone()) {
                actionTask.cancel(true);
                waitUntilExecuted();
            }
            updateActionState(State.CANCELED);
            unit.reschedule();
            return null;
        });
    }

    @Override
    public Future<ActionDescription> abort() {
        if (!isExecuting()) {
            // this should never happen since a task should be executing before it is aborted
            LOGGER.error("Aborted action was not executing before");
            return CompletableFuture.completedFuture(getActionDescription());
        }

        updateActionState(State.ABORTING);
        return GlobalCachedExecutorService.submit(() -> {
            if (actionTask != null && !actionTask.isDone()) {
                actionTask.cancel(true);
                waitUntilExecuted();
            }
            updateActionState(State.SCHEDULED);
            // rescheduling is not necessary because aborting is only done when rescheduling
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void schedule() {
        updateActionState(State.SCHEDULED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reject() {
        updateActionState(State.REJECTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finish() {
        updateActionState(State.FINISHED);
    }

    private void updateActionState(ActionState.State state) {
        LOGGER.info(this + " State[" + state.name() + "]");

        synchronized (executionSync) {
            actionDescriptionBuilder.setActionState(ActionState.newBuilder().setValue(state));
            try {
                ServiceStateProcessor.updateLatestValueOccurrence(state.getValueDescriptor(), TimestampProcessor.getCurrentTimestamp(), actionDescriptionBuilder.getActionStateBuilder());
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER);
            }

            executionSync.notifyAll();
        }

        // make sure that state changes to finishing states, scheduled and executing always trigger a notification
        if (isNotifiedActionState(state)) {
            unit.notifyScheduledActionList();
        }
    }

    private void waitForExecution(final Future result) throws CouldNotPerformException, InterruptedException {
        //TODO this is a problem if the internal task is not returned as a completable future such as for the multi activity in users
        if (getActionDescription().getExecutionTimePeriod() == 0) {
            return;
        }
        try {
            result.get(getExecutionTime(), TimeUnit.MILLISECONDS);
            if (isValid()) {
                Thread.sleep(getExecutionTime());
            }
        } catch (CancellationException | ExecutionException | TimeoutException ex) {
            throw new CouldNotPerformException("Action execution aborted!", ex);
        } finally {
            if (!result.isDone()) {
                result.cancel(true);
            }
        }
    }

    @Override
    public String toString() {
        return Action.toString(this);
    }
}
