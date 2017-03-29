package org.openbase.bco.manager.scene.core;

/*
 * #%L
 * BCO Manager Scene Core
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.unit.AbstractExecutableBaseUnitController;
import org.openbase.bco.dal.remote.control.action.Action;
import org.openbase.bco.dal.remote.unit.ButtonRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.scene.lib.SceneController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionConfigType.ActionConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
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
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionConfig.getDefaultInstance()));
    }

    private final static long ACTION_EXECUTION_TIMEOUT = 15000;
    private final Object buttonObserverLock = new SyncObject("ButtonObserverLock");
    private final Set<ButtonRemote> buttonRemoteSet;
    private final List<Action> actionList;
    private final SyncObject actionListSync = new SyncObject("ActionListSync");
    private final Observer<ButtonData> buttonObserver;
    private boolean executing = false;

    public SceneControllerImpl() throws org.openbase.jul.exception.InstantiationException {
        super(SceneControllerImpl.class, SceneData.newBuilder());
        this.buttonRemoteSet = new HashSet<>();
        this.actionList = new ArrayList<>();
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
            actionList.clear();
            Action action;
            for (ActionConfig actionConfig : config.getSceneConfig().getActionConfigList()) {
                action = new Action();
                try {
                    action.init(actionConfig);
                    actionList.add(action);
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

    public static final int ACTION_REPLAY = 3;
    public static final int ACTION_EXECUTION_DEPLAY = 2000;

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.info("Activate Scene[" + getConfig().getLabel() + "]");

        executing = true;

        final Map<Future<Void>, Action> executionFutureList = new HashMap<>();

        // dublicate actions to make sure 
//        for (int i = 0; i <= ACTION_REPLAY; i++) {
        synchronized (actionListSync) {
            for (final Action action : actionList) {
                executionFutureList.put(action.execute(), action);
            }
        }
        Thread.sleep(11000);
        synchronized (actionListSync) {
            for (final Action action : actionList) {
                executionFutureList.put(action.execute(), action);
            }
        }
//        }

        MultiException.ExceptionStack exceptionStack = null;

        try {
            logger.debug("Waiting for action finalisation...");

            long checkStart = System.currentTimeMillis() + ACTION_EXECUTION_TIMEOUT;
            long timeout;
            for (Entry<Future<Void>, Action> futureActionEntry : executionFutureList.entrySet()) {
                if (futureActionEntry.getKey().isDone()) {
                    continue;
                }
                logger.info("Waiting for action [" + futureActionEntry.getValue().getConfig().getServiceAttributeType() + "]");
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
            throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Scene[" + getConfig().getLabel() + "] execution failed!"), logger);
        } finally {
            for (Entry<Future<Void>, Action> futureActionEntry : executionFutureList.entrySet()) {
                if (!futureActionEntry.getKey().isDone()) {
                    futureActionEntry.getKey().cancel(true);
                }
            }
            executing = false;
            setActivationState(ActivationState.State.DEACTIVE);
        }
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        logger.debug("Finished scene: " + getConfig().getLabel());
    }

    @Override
    public boolean isExecuting() {
        return executing;
    }

    @Override
    public Future<Void> applyAction(final ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        return GlobalCachedExecutorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    logger.info("applyAction: " + actionConfig.getLabel());
                    final Object attribute = new ServiceJSonProcessor().deserialize(actionConfig.getServiceAttribute(), actionConfig.getServiceAttributeType());
                    // Since its an action it has to be an operation service pattern
                    final ServiceTemplate serviceTemplate = ServiceTemplate.newBuilder().setType(actionConfig.getServiceType()).setPattern(ServiceTemplate.ServicePattern.OPERATION).build();
                    Service.invokeServiceMethod(serviceTemplate, SceneControllerImpl.this, attribute);
                    return null;
                } catch (CouldNotPerformException ex) {
                    throw new CouldNotPerformException("Could not apply action!", ex);
                }
            }
        });
    }

    @Override
    protected boolean isAutostartEnabled() throws CouldNotPerformException {
        return false;
    }
}
