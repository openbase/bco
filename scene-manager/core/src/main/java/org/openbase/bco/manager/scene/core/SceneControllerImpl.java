package org.openbase.bco.manager.scene.core;

/*
 * #%L
 * COMA SceneManager Core
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.remote.control.action.Action;
import org.openbase.bco.dal.remote.unit.ButtonRemote;
import org.openbase.bco.manager.scene.lib.Scene;
import org.openbase.bco.manager.scene.lib.SceneController;
import org.openbase.bco.registry.device.lib.DeviceRegistry;
import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.com.AbstractExecutableController;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionConfigType.ActionConfig;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.ButtonStateType.ButtonState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.dal.ButtonDataType.ButtonData;
import rst.domotic.unit.scene.SceneDataType.SceneData;

/**
 *
 * UnitConfig
 */
public class SceneControllerImpl extends AbstractExecutableController<SceneData, SceneData.Builder, UnitConfig> implements SceneController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionConfig.getDefaultInstance()));
    }

    private final List<ButtonRemote> buttonRemoteList;
    private final List<Action> actionList;
    private final SyncObject triggerListSync = new SyncObject("TriggerListSync");
    private final SyncObject actionListSync = new SyncObject("ActionListSync");
    private final Observer<ButtonData> buttonObserver;
    private DeviceRegistry deviceRegistry;

    public SceneControllerImpl() throws org.openbase.jul.exception.InstantiationException {
        super(SceneData.newBuilder());
        this.buttonRemoteList = new ArrayList<>();
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
            CachedDeviceRegistryRemote.waitForData();
            this.deviceRegistry = CachedDeviceRegistryRemote.getRegistry();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init(config);
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        synchronized (triggerListSync) {
            try {
                for (ButtonRemote buttonRemote : buttonRemoteList) {
                    buttonRemote.deactivate();
                    buttonRemote.removeDataObserver(buttonObserver);
                }
                buttonRemoteList.clear();
                ButtonRemote buttonRemote;

                for (UnitConfig unitConfig : deviceRegistry.getUnitConfigsByLabel(config.getLabel())) {
                    //TODO implement deviceregistry method get unit by label and type.
                    if (unitConfig.getType() != UnitTemplate.UnitType.BUTTON) {
                        continue;
                    }
                    try {
                        buttonRemote = new ButtonRemote();
                        buttonRemote.init(unitConfig);
                        buttonRemoteList.add(buttonRemote);
                    } catch (InitializationException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not register remote for Button[" + unitConfig.getLabel() + "]!", ex), logger);
                    }
                }
                if (isEnabled()) {
                    for (ButtonRemote button : buttonRemoteList) {
                        button.activate();
                        try {
                            button.waitForData(2, TimeUnit.SECONDS);
                        } catch (CouldNotPerformException ex) {
                            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial button sync failed!", ex), logger);
                        }
                        button.addDataObserver(buttonObserver);
                    }
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not init all related button remotes.", ex), logger);
            }
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
            MultiException.checkAndThrow("Could not activate service remotes for some actions", exceptionStack);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
        }
        return super.applyConfigUpdate(config);
    }

    @Override
    public void enable() throws CouldNotPerformException, InterruptedException {
        logger.info("enable " + getConfig().getLabel());
        super.enable();
        for (ButtonRemote button : buttonRemoteList) {
            button.activate();
            button.addDataObserver(buttonObserver);
        }
    }

    @Override
    public void disable() throws CouldNotPerformException, InterruptedException {
        logger.info("disable " + getConfig().getLabel());
        for (ButtonRemote button : buttonRemoteList) {
            button.removeDataObserver(buttonObserver);
            button.deactivate();
        }
        super.disable();
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Scene.class, this, server);
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.info("Activate scene: " + getConfig().getLabel());
        synchronized (actionListSync) {
            for (Action action : actionList) {
                action.execute();
            }
        }

        Thread thread = new Thread() {

            @Override
            public void run() {
                try {
                    logger.info("Waiting for action finalisation...");
                    synchronized (actionListSync) {
                        for (Action action : actionList) {
                            try {
                                logger.info("Waiting for action [" + action.getConfig().getServiceAttributeType() + "]");
                                action.waitForFinalization();
                            } catch (InterruptedException ex) {
                                ExceptionPrinter.printHistory(ex, logger);
                                break;
                            }
                        }
                    }
                    logger.info("All Actions are finished. Deactivate scene...");
                    setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).build());
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not wait for actions!", ex), logger);
                }
            }
        };
        thread.start();
        setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).build());
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        logger.info("Finished scene: " + getConfig().getLabel());
    }
}
