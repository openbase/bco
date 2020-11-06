package org.openbase.bco.dal.control.layer.unit;

/*-
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import com.google.protobuf.AbstractMessage;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.ServiceProvider;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor;
import org.openbase.bco.dal.lib.layer.service.operation.ActivationStateOperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ActivationStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionPriorityType;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState.Builder;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @param <D>  the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractExecutableBaseUnitController<D extends AbstractMessage & Serializable, DB extends D.Builder<DB>> extends AbstractBaseUnitController<D, DB> implements ActivationStateProviderService {

    public static final String FIELD_ACTIVATION_STATE = "activation_state";
    public static final String FIELD_AUTOSTART = "autostart";

    private final SyncObject activationLock = new SyncObject(AbstractExecutableBaseUnitController.class);
    private final SyncObject executionLock = new SyncObject("ExecutionLock");
    private Future<ActionDescription> executionFuture;
    private final ActivationStateOperationServiceImpl activationStateOperationService;

    public AbstractExecutableBaseUnitController(final DB builder) throws org.openbase.jul.exception.InstantiationException {
        super(builder);
        try {
            this.activationStateOperationService = new ActivationStateOperationServiceImpl(this);
            registerOperationService(ServiceType.ACTIVATION_STATE_SERVICE, activationStateOperationService);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
    }

    public boolean isExecuting() {
        synchronized (executionLock) {
            return executionFuture != null && !executionFuture.isDone();
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        try {
            synchronized (activationLock) {
                super.activate();
                if (detectAutostart()) {
                    activationStateOperationService.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build());
                } else {
                    activationStateOperationService.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.INACTIVE).build()).get();
                }
            }
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not activate " + this, ex);
        }
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        synchronized (activationLock) {
            stop(ActivationState.newBuilder().setValue(ActivationState.State.INACTIVE).build());
            cancelExecution();
            super.deactivate();
        }
    }

    private boolean detectAutostart() {
        try {
            return isAutostartEnabled();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new NotSupportedException("autostart", this, ex), logger, LogLevel.WARN);
            return true;
        }
    }

    public void cancelExecution() {
        synchronized (executionLock) {
            if (isExecuting() && !executionFuture.isDone()) {
                executionFuture.cancel(true);
            }
            executionFuture = null;
        }
    }

    protected abstract boolean isAutostartEnabled() throws CouldNotPerformException;

    protected abstract ActionDescription execute(final ActivationState activationState) throws CouldNotPerformException, InterruptedException;

    protected abstract void stop(final ActivationState activationState) throws CouldNotPerformException, InterruptedException;

    public class ActivationStateOperationServiceImpl implements ActivationStateOperationService {

        private final ServiceProvider serviceProvider;

        public ActivationStateOperationServiceImpl(ServiceProvider serviceProvider) {
            this.serviceProvider = serviceProvider;
        }

        @Override
        public Future<ActionDescription> setActivationState(final ActivationState activationState) {
            try {
                logger.trace("setActivationState: {}", activationState.getValue().name());
                final Builder activationStateBuilder = activationState.toBuilder();
                synchronized (executionLock) {

                    // filter events that do not change anything
                    if (activationStateBuilder.getValue() == getActivationState().getValue()) {
                        logger.trace("skip already applied state: {}", activationStateBuilder.getValue().name());
                        return FutureProcessor.completedFuture(ServiceStateProcessor.getResponsibleAction(activationState, () -> ActionDescription.getDefaultInstance()));
                    }

                    final ActivationState fallbackActivationState = getActivationState();

                    if (activationStateBuilder.getValue() == ActivationState.State.ACTIVE) {

                        // make sure timestamp is updated.
                        try {
                            logger.trace("inform about " + activationStateBuilder.getValue().name());
                            applyServiceState(activationStateBuilder, ServiceType.ACTIVATION_STATE_SERVICE);
                        } catch (CouldNotPerformException ex) {
                            throw new CouldNotPerformException("Could not " + StringProcessor.transformUpperCaseToPascalCase(activationStateBuilder.getValue().name()) + " " + this, ex);
                        }

                        // filter duplicated execution
                        if (isExecuting()) {
                            return FutureProcessor.completedFuture(activationState.getResponsibleAction());
                        }

                        //TODO: making this a separate thread makes it really difficult to wait for states from scenes in unit tests
                        executionFuture = GlobalCachedExecutorService.submit(() -> {
                            try {
                                return execute(activationStateBuilder.build());
                            } catch (CouldNotPerformException ex) {
                                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not execute [" + getLabel() + "]", ex), logger);

                                // rollback previous activation state
                                synchronized (executionLock) {
                                    try {
                                        stop(fallbackActivationState);
                                    } catch (InterruptedException exx) {
                                        Thread.currentThread().interrupt();
                                    } catch (Exception exx) {
                                        ExceptionPrinter.printHistory("rollback failed", exx, logger);
                                    }
                                    applyServiceState(fallbackActivationState, ServiceType.ACTIVATION_STATE_SERVICE);
                                }
                                return activationStateBuilder.getResponsibleAction();
                            }
                        });
                        return executionFuture;
                    } else {
                        if (isExecuting()) {
                            cancelExecution();
                        }
                        // call stop even if execution has already finished
                        // many components just register observer etc. in execute and this it is done quickly
                        try {
                            stop(activationStateBuilder.build());
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
                        } catch (Exception ex) {
                            ExceptionPrinter.printHistory("stop failed", ex, logger);
                            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
                        }
                        try {
                            logger.trace("inform about " + activationStateBuilder.getValue().name());
                            applyServiceState(activationStateBuilder, ServiceType.ACTIVATION_STATE_SERVICE);
                        } catch (CouldNotPerformException ex) {
                            throw new CouldNotPerformException("Could not " + StringProcessor.transformUpperCaseToPascalCase(activationStateBuilder.getValue().name()) + " " + this, ex);
                        }
                    }
                }
                return FutureProcessor.completedFuture(activationStateBuilder.getResponsibleAction());
            } catch (CouldNotPerformException ex) {
                return FutureProcessor.canceledFuture(ActionDescription.class, ex);
            }
        }

        @Override
        public ServiceProvider getServiceProvider() {
            return serviceProvider;
        }
    }
}
