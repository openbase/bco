package org.openbase.bco.dal.lib.action;

/*
 * #%L
 * BCO DAL Library
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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.AbstractUnitController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionDescriptionType.ActionDescription.Builder;
import rst.domotic.action.ActionDescriptionType.ActionDescriptionOrBuilder;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.ActionInitiatorType.ActionInitiator.Initiator;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import rst.domotic.state.ActionStateType.ActionState;
import rst.domotic.state.ActionStateType.ActionState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class ActionImpl implements Action {

    public static final String INITIATOR_KEY = "$INITIATOR";
    public static final String SERVICE_TYPE_KEY = "$SERVICE_TYPE";
    public static final String UNIT_LABEL_KEY = "$UNIT_LABEL";
    public static final String SERVICE_ATTRIBUTE_KEY = "SERVICE_ATTRIBUTE";
    public static final String GENERIC_ACTION_LABEL = UNIT_LABEL_KEY + "[" + SERVICE_ATTRIBUTE_KEY + "]";
    public static final String GENERIC_ACTION_DESCRIPTION = INITIATOR_KEY + " changed " + SERVICE_TYPE_KEY + " of unit " + UNIT_LABEL_KEY + " to " + SERVICE_ATTRIBUTE_KEY;

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionImpl.class);
    protected final AbstractUnitController unit;
    private final SyncObject executionSync = new SyncObject(ActionImpl.class);
    private final ServiceJSonProcessor serviceJSonProcessor;
    private final long creationTime;
    protected ActionDescription.Builder actionDescriptionBuilder;
    private Message serviceState;
    private ServiceDescription serviceDescription;
    private Future<ActionFuture> actionTask;

    public ActionImpl(final ActionDescription actionDescription, final AbstractUnitController unit) throws InstantiationException {
        try {
            this.creationTime = System.currentTimeMillis();
            this.unit = unit;
            this.serviceJSonProcessor = new ServiceJSonProcessor();
            this.init(actionDescription);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final ActionDescription actionDescription) throws InitializationException {
        try {
            // generate missing fields
            actionDescriptionBuilder = actionDescription.toBuilder();

            // update initiator type
            final UnitConfig initiatorUnitConfig = Registries.getUnitRegistry().getUnitConfigById(actionDescriptionBuilder.getInitiator().getUnitId());
            if ((initiatorUnitConfig.getUnitType() == UnitType.USER && initiatorUnitConfig.getUserConfig().getIsSystemUser())) {
                actionDescriptionBuilder.getInitiatorBuilder().setInitiator(Initiator.HUMAN);
            } else {
                actionDescriptionBuilder.getInitiatorBuilder().setInitiator(Initiator.SYSTEM);
            }

            // verify
            verifyActionDescription(actionDescriptionBuilder);

            // prepare
            actionDescriptionBuilder.setId(UUID.randomUUID().toString());
            LabelProcessor.addLabel(actionDescriptionBuilder.getLabelBuilder(), Locale.ENGLISH, GENERIC_ACTION_LABEL);
            serviceState = serviceJSonProcessor.deserialize(actionDescriptionBuilder.getServiceStateDescription().getServiceAttribute(), actionDescriptionBuilder.getServiceStateDescription().getServiceAttributeType());

            // verify service attribute
            serviceState = Services.verifyAndRevalidateServiceState(serviceState);

            // generate or update action description
            updateDescription(actionDescriptionBuilder, serviceState);

            // since its an action it has to be an operation service pattern
            serviceDescription = ServiceDescription.newBuilder().setServiceType(actionDescriptionBuilder.getServiceStateDescription().getServiceType()).setPattern(ServicePattern.OPERATION).build();

            // mark action as initialized.
            updateActionState(State.INITIALIZED);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void updateDescription(Builder actionDescriptionBuilder, Message serviceState) {
        String description = actionDescriptionBuilder.getDescription().isEmpty() ? GENERIC_ACTION_DESCRIPTION : actionDescriptionBuilder.getDescription();

        try {
            description = description.replace(UNIT_LABEL_KEY,
                    unit.getLabel());

            description = description.replace(SERVICE_TYPE_KEY,
                    StringProcessor.transformToCamelCase(actionDescriptionBuilder.getServiceStateDescription().getServiceType().name()));

            description = description.replace(INITIATOR_KEY,
                    LabelProcessor.getBestMatch(Registries.getUnitRegistry().getUnitConfigById(actionDescriptionBuilder.getInitiator().getUnitId()).getLabel()));

            description = description.replace(SERVICE_ATTRIBUTE_KEY,
                    StringProcessor.transformCollectionToString(Services.generateServiceStateStringRepresentation(serviceState, actionDescriptionBuilder.getServiceStateDescription().getServiceType()), " "));
            actionDescriptionBuilder.setDescription(StringProcessor.removeDoubleWhiteSpaces(description));
            LOGGER.warn("Generated action description: " + description);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not update action description!", ex, LOGGER);
        }
    }

    private void verifyActionDescription(final ActionDescriptionOrBuilder actionDescription) throws VerificationFailedException {
        try {
            if (actionDescription == null) {
                throw new NotAvailableException("ActionDescription");
            }

            if (!actionDescription.hasServiceStateDescription()) {
                throw new NotAvailableException("ActionDescription.ServiceStateDescription");
            }

            if (!actionDescription.getServiceStateDescription().hasUnitId() || actionDescription.getServiceStateDescription().getUnitId().isEmpty()) {
                throw new NotAvailableException("ActionDescription.ServiceStateDescription.UnitId");
            }

            if (!actionDescription.getServiceStateDescription().getUnitId().equals(unit.getId())) {
                throw new InvalidStateException("Referred unit is not compatible with the registered unit controller!");
            }
        } catch (CouldNotPerformException ex) {
            throw new VerificationFailedException("Given ActionDescription[" + actionDescription.getLabel() + "] is invalid!", ex);
        }
    }

    /**
     * returns if there is still an task operating this action.
     *
     * @return true if action is still in progress.
     */
    private boolean isExecuting() {
        synchronized (executionSync) {
            return actionTask != null && !actionTask.isDone();
        }
    }

    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActionFuture> execute() {
        synchronized (executionSync) {
            if (isExecuting()) {
                return actionTask;
            }

            actionTask = GlobalCachedExecutorService.submit(() -> {
                try {
                    synchronized (executionSync) {

                        LOGGER.info("================================================================================");
                        LOGGER.info(actionDescriptionBuilder.getDescription());

                        // Initiate
                        updateActionState(ActionState.State.INITIATING);

                        try {
                            try {

                                setRequestedState();

                                // Execute
                                updateActionState(ActionState.State.EXECUTING);

                                try {
                                    waitForExecution(unit.performOperationService(serviceState, serviceDescription.getServiceType()));
                                } catch (CouldNotPerformException ex) {
                                    if (ex.getCause() instanceof InterruptedException) {
                                        updateActionState(ActionState.State.ABORTED);
                                    } else {
                                        updateActionState(ActionState.State.EXECUTION_FAILED);
                                    }
                                    throw new ExecutionException(ex);
                                }
                                updateActionState(ActionState.State.FINISHING);
                                return getActionFuture();
                            } catch (final CancellationException ex) {
                                updateActionState(ActionState.State.REJECTED);
                                throw ex;
                            }
                        } catch (CouldNotPerformException ex) {
                            updateActionState(ActionState.State.ABORTED);
                            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
                        }
                    }
                } catch (CouldNotPerformException ex) {
                    throw new CouldNotPerformException("Could not execute action!", ex);
                } catch (InterruptedException ex) {
                    updateActionState(ActionState.State.ABORTED);
                    throw ex;
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

    public void waitUntilFinish() throws InterruptedException {
        synchronized (executionSync) {
            if(!isExecuting()) {
                return;
            }
            executionSync.wait();
        }
    }

    @Override
    public ActionFuture getActionFuture() {
        final ActionFuture.Builder actionFuture = ActionFuture.newBuilder();
        actionFuture.addActionDescription(actionDescriptionBuilder);
        return actionFuture.build();
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
     *
     * @throws NotAvailableException {@inheritDoc }
     */
    @Override
    public ActionDescription getActionDescription() {
        return actionDescriptionBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel() {

        // return if action is done
        if (actionTask == null || actionTask.isDone()) {
            return;
        }

        actionTask.cancel(true);
    }

    public void cancelAndWait() throws InterruptedException {
        synchronized (executionSync) {
            cancel();
            waitUntilFinish();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void schedule() {
        //todo
    }

    private void updateActionState(ActionState.State state) {
        actionDescriptionBuilder.setActionState(ActionState.newBuilder().setValue(state));
        LOGGER.info(this + " State[" + state.name() + "]");
    }

    private void waitForExecution(final Future result) throws CouldNotPerformException, InterruptedException {
        try {
            result.get(getExecutionTime(), TimeUnit.MILLISECONDS);
            if(isValid()) {
                Thread.sleep(getExecutionTime());
            }
        } catch (CancellationException | ExecutionException | TimeoutException ex) {
            throw new CouldNotPerformException("Action execution aborted!", ex);
        } finally {
            if(!result.isDone()) {
                result.cancel(true);
            }
        }
    }

    @Override
    public String toString() {
        if (actionDescriptionBuilder == null) {
            return getClass().getSimpleName() + "[?]";
        }
        return getClass().getSimpleName() + "[" + actionDescriptionBuilder.getServiceStateDescription().getUnitId() + "|" + actionDescriptionBuilder.getServiceStateDescription().getServiceType() + "|" + actionDescriptionBuilder.getServiceStateDescription().getServiceAttribute() + "|" + actionDescriptionBuilder.getPriority().name() + "|" + StringProcessor.transformCollectionToString(actionDescriptionBuilder.getCategoryList(), " | ") + "]";
    }
}
