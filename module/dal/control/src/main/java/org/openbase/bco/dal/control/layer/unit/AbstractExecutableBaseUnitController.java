package org.openbase.bco.dal.control.layer.unit;

/*-
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.operation.ActivationStateOperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ActivationStateProviderService;
import org.openbase.bco.dal.lib.state.States;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.iface.TimedProcessable;
import org.openbase.jul.schedule.CloseableWriteLockWrapper;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.io.Serializable;
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

    public AbstractExecutableBaseUnitController(final DB builder) throws org.openbase.jul.exception.InstantiationException {
        super(builder);
        try {
            registerOperationService(ServiceType.ACTIVATION_STATE_SERVICE, new ActivationStateOperationServiceImpl(this));
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        super.activate();
        handleAutostart();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        cancelAllActions();
        super.deactivate();
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        try (final CloseableWriteLockWrapper ignored = getManageWriteLockInterruptible(this)) {
            var updatedConfig = super.applyConfigUpdate(config);
            handleAutostart();
            return updatedConfig;
        }
    }

    private boolean detectAutostart() {
        try {
            return isAutostartEnabled();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new NotSupportedException("autostart", this, ex), logger, LogLevel.WARN);
            return false;
        }
    }

    private void handleAutostart() {

        if (!detectAutostart()) {
            return;
        }

        try {
            final ActionParameter.Builder actionParameter = ActionDescriptionProcessor.generateDefaultActionParameter(States.Activation.ACTIVE, ServiceType.ACTIVATION_STATE_SERVICE, this);
            actionParameter.setInterruptible(true);
            actionParameter.setSchedulable(true);
            actionParameter.setPriority(Priority.NO);
            actionParameter.getActionInitiatorBuilder().setInitiatorType(InitiatorType.SYSTEM);
            actionParameter.setExecutionTimePeriod(TimeUnit.MILLISECONDS.toMicros(TimedProcessable.INFINITY_TIMEOUT));
            new RemoteAction(applyAction(actionParameter), this::isActive);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not autostart " + this, ex, logger, LogLevel.ERROR);
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

                switch (activationState.getValue()) {
                    case ACTIVE:
                        applyServiceState(activationState, ServiceType.ACTIVATION_STATE_SERVICE);
                        execute(activationState);
                        break;
                    case INACTIVE:
                    case UNKNOWN:
                        stop(activationState);
                        applyServiceState(activationState, ServiceType.ACTIVATION_STATE_SERVICE);
                        break;
                }

                return FutureProcessor.completedFuture(activationState.getResponsibleAction());
            } catch (CouldNotPerformException | InterruptedException ex) {
                return FutureProcessor.canceledFuture(ActionDescription.class, ex);
            }
        }

        @Override
        public ServiceProvider getServiceProvider() {
            return serviceProvider;
        }
    }
}
