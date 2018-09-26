package org.openbase.bco.dal.remote.action;

/*
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

import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.dal.remote.layer.service.AbstractServiceRemote;
import org.openbase.bco.dal.remote.layer.service.ServiceRemoteFactory;
import org.openbase.bco.dal.remote.layer.service.ServiceRemoteFactoryImpl;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.EnablingStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class RemoteAction implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteAction.class);
    private final SyncObject executionSync = new SyncObject(RemoteAction.class);
    private ActionDescription actionDescription;
    private UnitConfig unitConfig;
    private ServiceRemoteFactory serviceRemoteFactory;
    private AbstractServiceRemote<?, ?> serviceRemote;
    private Future<ActionFuture> executionFuture;

    @Override
    public void init(final ActionDescription actionDescription) throws InitializationException, InterruptedException {
        this.actionDescription = actionDescription;
        try {
            if (actionDescription.getServiceStateDescription().getUnitId().isEmpty()) {
                throw new InvalidStateException(actionDescription.getLabel() + " has no valid unit id!");
            }

            this.serviceRemoteFactory = ServiceRemoteFactoryImpl.getInstance();
            Registries.waitForData();
            this.unitConfig = Registries.getUnitRegistry().getUnitConfigById(actionDescription.getServiceStateDescription().getUnitId());
            this.verifyUnitConfig(unitConfig);
            this.serviceRemote = serviceRemoteFactory.newInstance(actionDescription.getServiceStateDescription().getServiceType());
            this.serviceRemote.setInfrastructureFilter(false);
            this.serviceRemote.init(unitConfig);
            serviceRemote.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void verifyUnitConfig(final UnitConfig unitConfig) throws VerificationFailedException {
        if (!unitConfig.getEnablingState().getValue().equals(EnablingStateType.EnablingState.State.ENABLED)) {
            try {
                throw new VerificationFailedException("Referred Unit[" + ScopeGenerator.generateStringRep(unitConfig.getScope()) + "] is disabled!");
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.WARN);
                try {
                    throw new VerificationFailedException("Referred Unit[" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "] is disabled!");
                } catch (NotAvailableException exx) {
                    throw new VerificationFailedException("Referred Unit[" + unitConfig.getId() + "] is disabled!");
                }
            }
        }
    }

    @Override
    public Future<ActionFuture> execute() throws CouldNotPerformException {
        synchronized (executionSync) {
            return serviceRemote.applyAction(getActionDescription());
        }
    }

    public void waitForFinalization() throws CouldNotPerformException, InterruptedException {
        Future currentExecution;
        synchronized (executionSync) {
            if (executionFuture == null) {
                throw new InvalidStateException("No execution running!");
            }
            currentExecution = executionFuture;
        }

        try {
            currentExecution.get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not wait for execution!", ex);
        }
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     *
     */
    @Override
    public ActionDescription getActionDescription() {
        return actionDescription;
    }

    @Override
    public long getCreationTime() {
        return 0;
    }

    @Override
    public void cancel() {

    }

    @Override
    public void schedule() {

    }

    @Override
    public ActionFuture getActionFuture() {
        return null;
    }

    @Override
    public void waitUntilFinish() throws InterruptedException {

    }

    @Override
    public String toString() {
        if (actionDescription == null) {
            return getClass().getSimpleName() + "[?]";
        }
        return getClass().getSimpleName() + "[" + actionDescription.getServiceStateDescription().getUnitId() + "|" + actionDescription.getServiceStateDescription().getServiceType() + "|" + actionDescription.getServiceStateDescription().getServiceAttribute() + "|" + actionDescription.getServiceStateDescription().getUnitId() + "]";
    }
}
