package org.openbase.bco.dal.lib.layer.unit;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import com.google.protobuf.GeneratedMessage;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.iface.Enableable;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import rst.domotic.state.ActivationStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * @param <D> the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 */
public abstract class AbstractExecutableBaseUnitController<D extends GeneratedMessage, DB extends D.Builder<DB>> extends AbstractBaseUnitController<D, DB> implements Enableable {

    public static final String FIELD_ACTIVATION_STATE = "activation_state";
    public static final String FIELD_AUTOSTART = "autostart";

    private final SyncObject enablingLock = new SyncObject(AbstractExecutableBaseUnitController.class);
    private Future<Void> executionFuture;
    private SyncObject executionLock = new SyncObject("ExecutionLock");
    private boolean enabled;

    public AbstractExecutableBaseUnitController(final Class unitClass, final DB builder) throws org.openbase.jul.exception.InstantiationException {
        super(unitClass, builder);
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
    }

    public ActivationStateType.ActivationState getActivationState() throws NotAvailableException {
        return (ActivationStateType.ActivationState) getDataField(FIELD_ACTIVATION_STATE);
    }

    public Future<Void> setActivationState(final ActivationStateType.ActivationState activation) throws CouldNotPerformException {
        if (activation == null || activation.getValue().equals(ActivationStateType.ActivationState.State.UNKNOWN)) {
            throw new InvalidStateException("Unknown is not a valid state!");
        }

        Future<Void> result = null;

        try (ClosableDataBuilder<DB> dataBuilder = getDataBuilder(this)) {
            try {
                synchronized (executionLock) {
                    if (activation.getValue() == ActivationStateType.ActivationState.State.ACTIVE) {

                        // filter dublicated execution
                        if (isExecuting()) {
                            return executionFuture;
                        }

                        executionFuture = GlobalCachedExecutorService.submit(new Callable<Void>() {

                            @Override
                            public Void call() throws Exception {
                                try {
                                    execute();
                                } catch (CouldNotPerformException ex) {
                                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not execute [" + getLabel() + "]", ex), logger);
                                }
                                return null;
                            }
                        });
                        result = executionFuture;
                    } else {
                        if (isExecuting()) {
                            cancelExecution();
                            result = GlobalCachedExecutorService.submit(() -> {
                                try {
                                    stop();
                                } catch (CouldNotPerformException ex) {
                                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not stop [" + getLabel() + "]", ex), logger);
                                }
                                return null;
                            });
                        }
                    }
                }

                // save new activation state
                Descriptors.FieldDescriptor findFieldByName = dataBuilder.getInternalBuilder().getDescriptorForType().findFieldByName(FIELD_ACTIVATION_STATE);
                if (findFieldByName == null) {
                    throw new NotAvailableException("Field[" + FIELD_ACTIVATION_STATE + "] does not exist for type " + dataBuilder.getClass().getName());
                }
                dataBuilder.getInternalBuilder().setField(findFieldByName, activation);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update execution state!", ex), logger);
            }
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not " + StringProcessor.transformUpperCaseToCamelCase(activation.getValue().name()) + " " + this, ex);
        }

        if (result == null) {
            return CompletableFuture.completedFuture(null);
        }
        return result;
    }

    public boolean isExecuting() {
        synchronized (executionLock) {
            return executionFuture != null && !executionFuture.isDone();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() throws CouldNotPerformException, InterruptedException {
        try {
            synchronized (enablingLock) {
                enabled = true;
                activate();
                if (detectAutostart()) {
                    setActivationState(ActivationStateType.ActivationState.newBuilder().setValue(ActivationStateType.ActivationState.State.ACTIVE).build()).get();
                } else {
                    setActivationState(ActivationStateType.ActivationState.newBuilder().setValue(ActivationStateType.ActivationState.State.DEACTIVE).build()).get();
                }
            }
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not enable " + this, ex);
        }
    }

    @Override
    public void disable() throws CouldNotPerformException, InterruptedException {
        try {
            synchronized (enablingLock) {
                cancelExecution();
                enabled = false;
                setActivationState(ActivationStateType.ActivationState.newBuilder().setValue(ActivationStateType.ActivationState.State.DEACTIVE).build()).get();
                deactivate();
            }
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not disable " + this, ex);
        }
    }

    private boolean detectAutostart() {
        try {
            return isAutostartEnabled();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new NotSupportedException("autostart", (AbstractExecutableBaseUnitController) this, (Throwable) ex), logger, LogLevel.WARN);
            return true;
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        stop();
        super.deactivate();
    }

    public void cancelExecution() {
        synchronized (executionLock) {
            if (isExecuting()) {
                executionFuture.cancel(true);
            }
            executionFuture = null;
        }
    }

    protected abstract boolean isAutostartEnabled() throws CouldNotPerformException;

    protected abstract void execute() throws CouldNotPerformException, InterruptedException;

    protected abstract void stop() throws CouldNotPerformException, InterruptedException;
}
