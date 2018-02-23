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
import org.openbase.bco.dal.lib.jp.JPResourceAllocation;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.AbstractUnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitAllocation;
import org.openbase.bco.dal.lib.layer.unit.UnitAllocator;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.communicationpatterns.ResourceAllocationType;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import rst.domotic.state.ActionStateType.ActionState;
import rst.timing.IntervalType;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class ActionImpl implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionImpl.class);

    private final SyncObject executionSync = new SyncObject(ActionImpl.class);
    private final ServiceJSonProcessor serviceJSonProcessor;
    private Message serviceAttribute;
    private ServiceDescription serviceDescription;

    protected final AbstractUnitController unit;
    protected ActionDescription.Builder actionDescriptionBuilder;

    public ActionImpl(final AbstractUnitController unit) {
        this.unit = unit;
        this.serviceJSonProcessor = new ServiceJSonProcessor();
    }

    @Override
    public void init(final ActionDescription actionDescription) throws InitializationException, InterruptedException {
        try {
            // verify
            this.verifyActionDescription(actionDescription);

            // prepare
            this.actionDescriptionBuilder = actionDescription.toBuilder();
            this.serviceAttribute = serviceJSonProcessor.deserialize(actionDescription.getServiceStateDescription().getServiceAttribute(), actionDescription.getServiceStateDescription().getServiceAttributeType());

            // verify service attribute
            Services.verifyServiceState(serviceAttribute);

            // since its an action it has to be an operation service pattern
            this.serviceDescription = ServiceDescription.newBuilder().setType(actionDescription.getServiceStateDescription().getServiceType()).setPattern(ServicePattern.OPERATION).build();

            // set resource allocation interval if not defined yet
            if (!actionDescription.getResourceAllocation().getSlot().hasBegin()) {
                ActionDescriptionProcessor.updateResourceAllocationSlot(actionDescriptionBuilder);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void verifyActionDescription(final ActionDescription actionDescription) throws VerificationFailedException {
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

    @Override
    public Future<ActionFuture> execute() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPResourceAllocation.class).getValue()) {
                return internalExecute().getTaskExecutor().getFuture();
            } else {
                return internalExecuteWithoutResourceAllocation();
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Cold not execute action", ex);
        }
    }

    protected UnitAllocation internalExecute() throws CouldNotPerformException {
        try {
            synchronized (executionSync) {

                if (actionDescriptionBuilder == null) {
                    throw new NotInitializedException("Action");
                }

                // Initiate
                updateActionState(ActionState.State.INITIATING);
                UnitAllocation unitAllocation;

                try {
                    // Verify authority
                    final ActionFuture.Builder actionFutureBuilder = ActionFuture.newBuilder();

                    unit.verifyAndUpdateAuthority(actionDescriptionBuilder.getActionAuthority(), actionFutureBuilder.getTicketAuthenticatorWrapperBuilder());

                    // Resource Allocation
                    unitAllocation = UnitAllocator.allocate(actionDescriptionBuilder, () -> {
                        try {
                            setRequestedState();
                            ActionFuture.Builder actionFuture = ActionFuture.newBuilder();

                            // Execute
                            updateActionState(ActionState.State.EXECUTING);

                            try {
                                waitForExecution(Services.invokeServiceMethod(serviceDescription, unit, serviceAttribute));
                                actionDescriptionBuilder.setTransactionId(unit.getTransactionIdByServiceType(actionDescriptionBuilder.getServiceStateDescription().getServiceType()));
                            } catch (CouldNotPerformException ex) {
                                if (ex.getCause() instanceof InterruptedException) {
                                    updateActionState(ActionState.State.ABORTED);
                                } else {
                                    updateActionState(ActionState.State.EXECUTION_FAILED);
                                }
                                throw new ExecutionException(ex);
                            }
                            actionFuture.addActionDescription(actionDescriptionBuilder);
                            updateActionState(ActionState.State.FINISHING);
                            return actionFuture.build();
                        } catch (final CancellationException ex) {
                            updateActionState(ActionState.State.ABORTED);
                            throw ex;
                        }
                    });

                    // register allocation update handler
                    unitAllocation.getTaskExecutor().getRemote().addSchedulerListener((allocation) -> {
                        try {
                            LOGGER.info("Update Allocation - Scope:[" + ScopeGenerator.generateStringRep(unit.getScope()) + "] State: [" + allocation.getState() + "]");
                        } catch (CouldNotPerformException ex) {
                            LOGGER.info("Update Allocation - Scope[?] State[" + allocation.getState() + "]");
                        }
                        actionDescriptionBuilder.setResourceAllocation(allocation);
                    });

                    return unitAllocation;
                } catch (CouldNotPerformException ex) {
                    updateActionState(ActionState.State.REJECTED);
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not execute action!", ex);
        }
    }

    protected Future<ActionFuture> internalExecuteWithoutResourceAllocation() throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                synchronized (executionSync) {

                    if (actionDescriptionBuilder == null) {
                        throw new NotInitializedException("Action");
                    }

                    // Initiate
                    updateActionState(ActionState.State.INITIATING);

                    try {
                        // Verify authority
                        final ActionFuture.Builder actionFuture = ActionFuture.newBuilder();

                        unit.verifyAndUpdateAuthority(actionDescriptionBuilder.getActionAuthority(), actionFuture.getTicketAuthenticatorWrapperBuilder());

                        // Resource Allocation
                        try {
                            // fake resource if needed
                            if (!actionDescriptionBuilder.hasResourceAllocation() || !actionDescriptionBuilder.getResourceAllocation().isInitialized()) {
                                ResourceAllocationType.ResourceAllocation.Builder resourceAllocationBuilder = actionDescriptionBuilder.getResourceAllocationBuilder();
                                resourceAllocationBuilder.setId(resourceAllocationBuilder.getId());
                                resourceAllocationBuilder.setState(ResourceAllocationType.ResourceAllocation.State.REQUESTED);
                                resourceAllocationBuilder.setPriority(ResourceAllocationType.ResourceAllocation.Priority.NORMAL);
                                resourceAllocationBuilder.setInitiator(ResourceAllocationType.ResourceAllocation.Initiator.SYSTEM);
                                resourceAllocationBuilder.setSlot(IntervalType.Interval.getDefaultInstance());
                                resourceAllocationBuilder.setPolicy(ResourceAllocationType.ResourceAllocation.Policy.PRESERVE);
                            }

                            setRequestedState();

                            // Execute
                            updateActionState(ActionState.State.EXECUTING);

                            try {
                                waitForExecution(Services.invokeServiceMethod(serviceDescription, unit, serviceAttribute));

                                actionDescriptionBuilder.setTransactionId(unit.getTransactionIdByServiceType(actionDescriptionBuilder.getServiceStateDescription().getServiceType()));
                            } catch (CouldNotPerformException ex) {
                                if (ex.getCause() instanceof InterruptedException) {
                                    updateActionState(ActionState.State.ABORTED);
                                } else {
                                    updateActionState(ActionState.State.EXECUTION_FAILED);
                                }
                                throw new ExecutionException(ex);
                            }
                            updateActionState(ActionState.State.FINISHING);

                            actionFuture.addActionDescription(actionDescriptionBuilder);
                            return actionFuture.build();
                        } catch (final CancellationException ex) {
                            updateActionState(ActionState.State.ABORTED);
                            throw ex;
                        }

                    } catch (CouldNotPerformException ex) {
                        updateActionState(ActionState.State.REJECTED);
                        throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
                    }
                }
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not execute action!", ex);
            } catch (InterruptedException ex) {
                if (JPService.debugMode()) {
                    ExceptionPrinter.printHistory(ex, LOGGER);
                }
                throw ex;
            }
        });
    }

    private void setRequestedState() throws CouldNotPerformException {
        try (ClosableDataBuilder dataBuilder = unit.getDataBuilder(this)) {
            // set the responsible action for the service attribute
            Message.Builder serviceStateBuilder = serviceAttribute.toBuilder();
            Descriptors.FieldDescriptor fieldDescriptor = ProtoBufFieldProcessor.getFieldDescriptor(serviceStateBuilder, Service.RESPONSIBLE_ACTION_FIELD_NAME);
            serviceStateBuilder.setField(fieldDescriptor, actionDescriptionBuilder.build());

            // set the updated service attribute as requested state in the unit data builder
            Services.invokeServiceMethod(serviceDescription.getType(), serviceDescription.getPattern(), ServiceTempus.REQUESTED, dataBuilder.getInternalBuilder(), serviceStateBuilder);
        }
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     * @throws NotAvailableException {@inheritDoc }
     */
    @Override
    public ActionDescription getActionDescription() throws NotAvailableException {
        return actionDescriptionBuilder.build();
    }

    private void updateActionState(ActionState.State state) {
        actionDescriptionBuilder.setActionState(ActionState.newBuilder().setValue(state));
        LOGGER.debug("StateUpdate[" + state.name() + "] of " + this);
    }

    private void waitForExecution(final Object result) throws ExecutionException, InterruptedException {
        if(result instanceof Future) {
            ((Future) result).get();
        } else {
            LOGGER.warn("Service["+serviceDescription.getType()+"] implementation of "+unit+" does not provide feedback about triggered operation! Just continue without feedback...");
        }
    }

    @Override
    public String toString() {
        if (actionDescriptionBuilder == null) {
            return getClass().getSimpleName() + "[?]";
        }
        return getClass().getSimpleName() + "[" + actionDescriptionBuilder.getServiceStateDescription().getUnitId() + "|" + actionDescriptionBuilder.getServiceStateDescription().getServiceType() + "|" + actionDescriptionBuilder.getServiceStateDescription().getServiceAttribute() + "|" + actionDescriptionBuilder.getResourceAllocation().getId() + "]";
    }
}
