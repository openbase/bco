package org.openbase.bco.dal.remote.control.action;

/*
 * #%L
 * BCO DAL Remote
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import org.openbase.bco.dal.remote.service.AbstractServiceRemote;
import org.openbase.bco.dal.remote.service.ServiceRemoteFactory;
import org.openbase.bco.dal.remote.service.ServiceRemoteFactoryImpl;
import org.openbase.bco.registry.device.lib.DeviceRegistry;
import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.iface.Initializable;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionConfigType;
import rst.domotic.action.ActionConfigType.ActionConfig;
import rst.domotic.action.ActionDataType.ActionData;
import rst.domotic.state.ActionStateType;
import rst.domotic.state.ActionStateType.ActionState;

/**
 *
 * * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class Action implements ActionService, Initializable<ActionConfig> {

    private static final Logger logger = LoggerFactory.getLogger(Action.class);

    private ActionConfig.Builder config;
    private final ActionData.Builder data;
    private ServiceRemoteFactory serviceRemoteFactory;
    private DeviceRegistry deviceRegistry;
    private AbstractServiceRemote serviceRemote;
    private Future executionFuture;
    private final SyncObject executionSync = new SyncObject(Action.class);

    public Action() {
        data = ActionData.newBuilder();
    }

    @Override
    public void init(final ActionConfigType.ActionConfig config) throws InitializationException, InterruptedException {
        try {
            this.config = config.toBuilder();
            this.data.setLabel(config.getLabel());
            this.deviceRegistry = CachedDeviceRegistryRemote.getRegistry();
            this.serviceRemoteFactory = ServiceRemoteFactoryImpl.getInstance();
            CachedDeviceRegistryRemote.waitForData();
            this.serviceRemote = serviceRemoteFactory.createAndInitServiceRemote(config.getServiceType(), deviceRegistry.getUnitConfigById(config.getUnitId()));
            serviceRemote.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void execute() throws CouldNotPerformException {
        synchronized (executionSync) {
            FutureTask task = new FutureTask(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    try {

                        // Initiate
                        updateActionState(ActionState.State.INITIATING);
                        try {
                            acquireService();
                        } catch (CouldNotPerformException e) {
                            ExceptionPrinter.printHistory(e, logger);
                            updateActionState(ActionState.State.REJECTED);
                        }

                        // Execute
                        updateActionState(ActionState.State.EXECUTING);
                        try {
                            serviceRemote.applyAction(getConfig());
                            updateActionState(ActionState.State.FINISHING);
                            releaseService();
                            updateActionState(ActionState.State.FINISHED);
                        } catch (InterruptedException ex) {
                            updateActionState(ActionState.State.ABORTING);
                            releaseService();
                            updateActionState(ActionState.State.ABORTED);
                            throw ex;
                        } catch (CouldNotPerformException | NullPointerException ex) {
                            updateActionState(ActionState.State.EXECUTION_FAILED);
                            releaseService();
                            throw ex;
                        }
                    } catch (Exception ex) {
                        throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Execution " + data.getActionState().getValue() + "!", ex), logger, LogLevel.WARN);
                    }

                    return null;
                }
            });
            executionFuture = GlobalCachedExecutorService.submit(task);
        }
    }

    private void acquireService() throws CouldNotPerformException {
        //TODO
        logger.info("Acquire service for execution of " + this);
    }

    private void releaseService() {
        try {
            // TODO
            logger.info("Release acquired services of " + this);
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("FatalExecutionError: Could not release service!", ex), logger);
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

    public ActionConfig getConfig() {
        return config.build();
    }

    private void updateActionState(ActionState.State state) {
        data.setActionState(ActionStateType.ActionState.newBuilder().setValue(state));
        logger.info("Stateupdate[" + state.name() + "] of " + this);
    }

    @Override
    public String toString() {
        if (config == null) {
            return getClass().getSimpleName() + "[?]";
        }
        return getClass().getSimpleName() + "[" + config.getOriginId() + "|" + config.getUnitId() + "|" + config.getServiceType() + "|" + config.getServiceAttribute() + "|" + config.getUnitId() + "]";
    }
}
