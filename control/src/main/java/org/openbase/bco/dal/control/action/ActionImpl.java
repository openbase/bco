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
import org.openbase.bco.dal.lib.action.SchedulableAction;
import org.openbase.bco.dal.lib.jp.JPProviderControlMode;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionDescriptionType.ActionDescription.Builder;
import rst.domotic.action.ActionDescriptionType.ActionDescriptionOrBuilder;
import rst.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import rst.domotic.state.ActionStateType.ActionState;
import rst.domotic.state.ActionStateType.ActionState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.language.MultiLanguageTextType.MultiLanguageText;
import rst.language.MultiLanguageTextType.MultiLanguageText.MapFieldEntry;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;

/**
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class ActionImpl implements SchedulableAction {

    public static final String INITIATOR_KEY = "$INITIATOR";
    public static final String SERVICE_TYPE_KEY = "$SERVICE_TYPE";
    public static final String UNIT_LABEL_KEY = "$UNIT_LABEL";
    public static final String SERVICE_ATTRIBUTE_KEY = "SERVICE_ATTRIBUTE";
    public static final String GENERIC_ACTION_LABEL = UNIT_LABEL_KEY + "[" + SERVICE_ATTRIBUTE_KEY + "]";


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
        try {
            actionDescriptionBuilder = actionDescription.toBuilder();

            TimestampProcessor.updateTimestampWithCurrentTime(actionDescriptionBuilder);

            // update initiator type
            if (actionDescriptionBuilder.getActionInitiator().hasInitiatorId() && !actionDescriptionBuilder.getActionInitiator().getInitiatorId().isEmpty()) {
                final UnitConfig initiatorUnitConfig = Registries.getUnitRegistry().getUnitConfigById(actionDescriptionBuilder.getActionInitiator().getInitiatorId());
                if ((initiatorUnitConfig.getUnitType() == UnitType.USER && initiatorUnitConfig.getUserConfig().getSystemUser())) {
                    actionDescriptionBuilder.getActionInitiatorBuilder().setInitiatorType(InitiatorType.HUMAN);
                } else {
                    actionDescriptionBuilder.getActionInitiatorBuilder().setInitiatorType(InitiatorType.SYSTEM);
                }
            } else if (!actionDescriptionBuilder.getActionInitiator().hasInitiatorType()) {
                // if no initiator is defined than use the system as initiator.
                actionDescriptionBuilder.getActionInitiatorBuilder().setInitiatorType(InitiatorType.SYSTEM);
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
            generateDescription(actionDescriptionBuilder, serviceState);

            // since its an action it has to be an operation service pattern
            serviceDescription = ServiceDescription.newBuilder().setServiceType(actionDescriptionBuilder.getServiceStateDescription().getServiceType()).setPattern(ServicePattern.OPERATION).build();

            // mark action as initialized.
            updateActionState(State.INITIALIZED);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void generateDescription(Builder actionDescriptionBuilder, Message serviceState) {

        final MultiLanguageText.Builder multiLanguageTextBuilder = MultiLanguageText.newBuilder();
        for (Entry<String, String> languageDescriptionEntry : GENERIC_ACTION_DESCRIPTION_MAP.entrySet()) {
            String description = languageDescriptionEntry.getValue();
            try {
                // setup unit label
                description = description.replace(UNIT_LABEL_KEY, unit.getLabel());

                // setup service type
                description = description.replace(SERVICE_TYPE_KEY,
                        StringProcessor.transformToCamelCase(actionDescriptionBuilder.getServiceStateDescription().getServiceType().name()));

                // setup initiator
                if (actionDescriptionBuilder.getActionInitiator().hasInitiatorId() && !actionDescriptionBuilder.getActionInitiator().getInitiatorId().isEmpty()) {
                    description = description.replace(INITIATOR_KEY, LabelProcessor.getBestMatch(Registries.getUnitRegistry().getUnitConfigById(actionDescriptionBuilder.getActionInitiator().getInitiatorId()).getLabel()));
                } else {
                    description = description.replace(INITIATOR_KEY, "Other");
                }

                // setup service attribute
                description = description.replace(SERVICE_ATTRIBUTE_KEY,
                        StringProcessor.transformCollectionToString(Services.generateServiceStateStringRepresentation(serviceState, actionDescriptionBuilder.getServiceStateDescription().getServiceType()), " "));

                // format
                description = StringProcessor.formatHumanReadable(description);

                // generate
                multiLanguageTextBuilder.addEntry(MapFieldEntry.newBuilder().setKey(languageDescriptionEntry.getKey()).setValue(description).build());
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not generate action description!", ex, LOGGER);
            }
            actionDescriptionBuilder.setDescription(multiLanguageTextBuilder);
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
            throw new VerificationFailedException("Given ActionDescription[" + actionDescription + "] is invalid!", ex);
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
                    synchronized (executionSync) {

                        LOGGER.info("================================================================================");

                        // Initiate
                        updateActionState(ActionState.State.INITIATING);

                        try {
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

                                try {
                                    LOGGER.warn("Wait for execution...");
                                    waitForExecution(unit.performOperationService(serviceState, serviceDescription.getServiceType()));
                                    LOGGER.warn("Execution finished!");
                                } catch (CouldNotPerformException ex) {
                                    if (ex.getCause() instanceof InterruptedException) {
                                        updateActionState(ActionState.State.ABORTED);
                                    } else {
                                        updateActionState(ActionState.State.EXECUTION_FAILED);
                                    }
                                    throw new ExecutionException(ex);
                                }
                                updateActionState(State.FINISHED);
                                return getActionDescription();
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
            if (!isExecuting()) {
                return;
            }
            executionSync.wait();
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

        // return if action is done
        if (actionTask == null || actionTask.isDone()) {
            return CompletableFuture.completedFuture(getActionDescription());
        }

        return GlobalCachedExecutorService.submit(() -> {
            actionTask.cancel(true);
            waitUntilFinish();
            unit.reschedule();
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

    private void updateActionState(ActionState.State state) {
        actionDescriptionBuilder.setActionState(ActionState.newBuilder().setValue(state));
        LOGGER.info(this + " State[" + state.name() + "]");
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
