package org.openbase.bco.manager.scene.core;

/*
 * #%L
 * BCO Manager Scene Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openbase.bco.dal.lib.layer.unit.AbstractExecutableBaseUnitController;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.unit.ButtonRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.scene.lib.SceneController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionDescriptionType;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.ButtonStateType.ButtonState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.dal.ButtonDataType.ButtonData;
import rst.domotic.unit.scene.SceneDataType.SceneData;

/**
 *
 * UnitConfig
 */
public class SceneControllerImpl extends AbstractExecutableBaseUnitController<SceneData, SceneData.Builder> implements SceneController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescriptionType.ActionDescription.getDefaultInstance()));
    }

    public static final int ACTION_REPLAY = (JPService.testMode() ? 1 : 3);
    public static final int ACTION_EXECUTION_DELAY = 5500;
    public static final long ACTION_EXECUTION_TIMEOUT = 15000;

    private final Object buttonObserverLock = new SyncObject("ButtonObserverLock");
    private final Set<ButtonRemote> buttonRemoteSet;
    private final List<RemoteAction> remoteActionList;
    private final SyncObject actionListSync = new SyncObject("ActionListSync");
    private final Observer<ButtonData> buttonObserver;

    public SceneControllerImpl() throws org.openbase.jul.exception.InstantiationException {
        super(SceneControllerImpl.class, SceneData.newBuilder());
        this.buttonRemoteSet = new HashSet<>();
        this.remoteActionList = new ArrayList<>();
        this.buttonObserver = (final Observable<ButtonData> source, ButtonData data) -> {
            if (data.getButtonState().getValue().equals(ButtonState.State.PRESSED)) {
                setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build());
            }
        };
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        try {
            Registries.getUnitRegistry().waitForData();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init(config);
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        config = super.applyConfigUpdate(config);

        try {
            synchronized (buttonObserverLock) {
                for (final ButtonRemote button : buttonRemoteSet) {
                    try {
                        logger.info("update: remove " + getConfig().getLabel() + " for button  " + button.getLabel());
                    } catch (NotAvailableException ex) {
                        Logger.getLogger(SceneControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    button.removeDataObserver(buttonObserver);
                }

                buttonRemoteSet.clear();
                ButtonRemote buttonRemote;

                for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType(config.getLabel(), UnitType.BUTTON)) {
                    try {
                        buttonRemote = Units.getUnit(unitConfig, false, Units.BUTTON);
                        buttonRemoteSet.add(buttonRemote);
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not register remote for Button[" + unitConfig.getLabel() + "]!", ex), logger);
                    }
                }
                if (isActive()) {
                    for (final ButtonRemote button : buttonRemoteSet) {
                        try {
                            logger.info("update: register " + getConfig().getLabel() + " for button  " + button.getLabel());
                        } catch (NotAvailableException ex) {
                            Logger.getLogger(SceneControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        button.addDataObserver(buttonObserver);
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not init all related button remotes.", ex), logger);
        }

        MultiException.ExceptionStack exceptionStack = null;

        synchronized (actionListSync) {
            remoteActionList.clear();
            RemoteAction action;
            for (ServiceStateDescription serviceStateDescription : config.getSceneConfig().getRequiredServiceStateDescriptionList()) {
                action = new RemoteAction();
                try {
                    action.init(ActionDescription.newBuilder().setServiceStateDescription(serviceStateDescription).build());
                    remoteActionList.add(action);
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }

            for (ServiceStateDescription serviceStateDescription : config.getSceneConfig().getOptionalServiceStateDescriptionList()) {
                action = new RemoteAction();
                try {
                    action.init(ActionDescription.newBuilder().setServiceStateDescription(serviceStateDescription).build());
                    remoteActionList.add(action);
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
        }

        try {
            MultiException.checkAndThrow("Could not fully init units of " + this, exceptionStack);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
        }
        return config;
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        synchronized (buttonObserverLock) {
            buttonRemoteSet.stream().forEach((button) -> {
                button.addDataObserver(buttonObserver);
            });
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        logger.debug("deactivate " + getConfig().getLabel());
        synchronized (buttonObserverLock) {
            buttonRemoteSet.stream().forEach((button) -> {
                button.removeDataObserver(buttonObserver);
            });
        }
        super.deactivate();
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.info("Activate Scene[" + getConfig().getLabel() + "]");

        final Map<Future<ActionFuture>, RemoteAction> executionFutureList = new HashMap<>();

        // dublicate actions to make sure all actions are applied.
        for (int i = 0; i < ACTION_REPLAY; i++) {
            synchronized (actionListSync) {
                for (final RemoteAction action : remoteActionList) {
                    executionFutureList.put(action.execute(), action);
                }
            }
            // only wait if another interation is following.
            if (i + 1 < ACTION_REPLAY) {
                Thread.sleep(ACTION_EXECUTION_DELAY);
            }
        }

        MultiException.ExceptionStack exceptionStack = null;

        try {
            logger.info("Waiting for action finalisation...");

            long checkStart = System.currentTimeMillis() + ACTION_EXECUTION_TIMEOUT;
            long timeout;
            for (Entry<Future<ActionFuture>, RemoteAction> futureActionEntry : executionFutureList.entrySet()) {
                if (futureActionEntry.getKey().isDone()) {
                    continue;
                }
                logger.info("Waiting for action [" + futureActionEntry.getValue().getActionDescription().getServiceStateDescription().getServiceAttributeType() + "]");
                try {
                    timeout = checkStart - System.currentTimeMillis();
                    if (timeout <= 0) {
                        throw new RejectedException("Rejected because of scene timeout.");
                    }
                    futureActionEntry.getKey().get(timeout, TimeUnit.MILLISECONDS);
                } catch (ExecutionException | TimeoutException ex) {
                    MultiException.push(this, ex, exceptionStack);
                }
            }
            MultiException.checkAndThrow("Could not execute all actions!", exceptionStack);
            logger.info("Deactivate Scene[" + getConfig().getLabel() + "] because all actions are sucessfully executed.");
        } catch (CouldNotPerformException | CancellationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Scene[" + getConfig().getLabel() + "] execution failed!", ex), logger);
        } finally {
            for (Entry<Future<ActionFuture>, RemoteAction> futureActionEntry : executionFutureList.entrySet()) {
                if (!futureActionEntry.getKey().isDone()) {
                    futureActionEntry.getKey().cancel(true);
                }
            }
            updateActivationState(ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).build());
        }
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        logger.debug("Finished scene: " + getConfig().getLabel());
    }
    
    @Override
    protected boolean isAutostartEnabled() throws CouldNotPerformException {
        return false;
    }
}
