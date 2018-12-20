package org.openbase.bco.dal.control.layer.unit.scene;

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

import org.openbase.bco.dal.control.layer.unit.AbstractExecutableBaseUnitController;
import org.openbase.bco.dal.lib.jp.JPUnitAllocation;
import org.openbase.bco.dal.lib.layer.unit.scene.SceneController;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.action.RemoteActionPool;
import org.openbase.bco.dal.remote.layer.unit.ButtonRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.MultiFuture;
import org.openbase.jul.schedule.SyncObject;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.action.ActionDescriptionType;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.ButtonStateType.ButtonState;
import org.openbase.type.domotic.state.ButtonStateType.ButtonState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.dal.ButtonDataType.ButtonData;
import org.openbase.type.domotic.unit.scene.SceneDataType.SceneData;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UnitConfig
 */
public class SceneControllerImpl extends AbstractExecutableBaseUnitController<SceneData, SceneData.Builder> implements SceneController {


    public static final long ACTION_EXECUTION_TIMEOUT = 15000;

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescriptionType.ActionDescription.getDefaultInstance()));
    }

    private final Object buttonObserverLock = new SyncObject("ButtonObserverLock");
    private final Set<ButtonRemote> buttonRemoteSet;
    private final Observer<DataProvider<ButtonData>, ButtonData> buttonObserver;
    private final RemoteActionPool requiredActionPool;
    private final RemoteActionPool optionalActionPool;


    public SceneControllerImpl() throws org.openbase.jul.exception.InstantiationException {
        super(SceneControllerImpl.class, SceneData.newBuilder());
        this.buttonRemoteSet = new HashSet<>();
        this.requiredActionPool = new RemoteActionPool(this);
        this.optionalActionPool = new RemoteActionPool(this);
        this.buttonObserver = (final DataProvider<ButtonData> source, ButtonData data) -> {

            // skip initial button state synchronization during system startup
            if (data.getButtonStateLast().getValue().equals(State.UNKNOWN)) {
                return;
            }

            if (data.getButtonState().getValue().equals(ButtonState.State.PRESSED)) {
                setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build());
            }
        };
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        try {
            Registries.waitForData();
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
                        logger.info("update: remove " + LabelProcessor.getBestMatch(getConfig().getLabel()) + " for button  " + button.getLabel());
                    } catch (NotAvailableException ex) {
                        Logger.getLogger(SceneControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    button.removeDataObserver(buttonObserver);
                }

                buttonRemoteSet.clear();
                ButtonRemote buttonRemote;

                for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByLabelAndUnitType(LabelProcessor.getBestMatch(config.getLabel()), UnitType.BUTTON)) {
                    try {
                        buttonRemote = Units.getUnit(unitConfig, false, Units.BUTTON);
                        buttonRemoteSet.add(buttonRemote);
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not register remote for Button[" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "]!", ex), logger);
                    }
                }
                if (isActive()) {
                    for (final ButtonRemote button : buttonRemoteSet) {
                        try {
                            logger.info("update: register " + LabelProcessor.getBestMatch(getConfig().getLabel()) + " for button  " + button.getLabel());
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
        requiredActionPool.initViaServiceStateDescription(config.getSceneConfig().getRequiredServiceStateDescriptionList());
        optionalActionPool.initViaServiceStateDescription(config.getSceneConfig().getOptionalServiceStateDescriptionList());
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
        logger.debug("deactivate " + LabelProcessor.getBestMatch(getConfig().getLabel()));
        synchronized (buttonObserverLock) {
            buttonRemoteSet.stream().forEach((button) -> {
                button.removeDataObserver(buttonObserver);
            });
        }
        super.deactivate();
    }

    @Override
    protected void execute(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        logger.error("Execute req: {}", LabelProcessor.getBestMatch(getConfig().getLabel()));
        final MultiFuture<ActionDescription> requiredActionsFuture = requiredActionPool.execute(activationState.getResponsibleAction(), true);
        logger.error("Execute: ops", LabelProcessor.getBestMatch(getConfig().getLabel()));
        final MultiFuture<ActionDescription> optionalActionsFuture = optionalActionPool.execute(activationState.getResponsibleAction(), true);

        // legacy handling
        final ActivationState deactivated = ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).build();
        try {
            if (!JPService.getProperty(JPUnitAllocation.class).getValue()) {
                requiredActionsFuture.get();
                optionalActionsFuture.get();
                stop(deactivated);
                applyDataUpdate(deactivated, ServiceType.ACTIVATION_STATE_SERVICE);
                return;
            }
        } catch (JPNotAvailableException e) {
            // just continue with default strategy
        } catch (ExecutionException e) {
            stop(deactivated);
            applyDataUpdate(deactivated, ServiceType.ACTIVATION_STATE_SERVICE);
            return;
        }
        logger.error("perform action observation");

        final Observer<RemoteAction, ActionDescription> requiredActionPoolObserver = new Observer<RemoteAction, ActionDescription>() {
            @Override
            public void update(RemoteAction source, ActionDescription data) throws Exception {
                if (source.isDone()&& source.isScheduled()) {
                    requiredActionPool.removeActionDescriptionObserver(this);
                    logger.error("deactivate scene because at least one required state can not be reached.");
                    stop(deactivated);
                    applyDataUpdate(deactivated, ServiceType.ACTIVATION_STATE_SERVICE);
                }
            }
        };

        requiredActionPool.addActionDescriptionObserver(requiredActionPoolObserver);

    }

    @Override
    protected void stop(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        logger.debug("Stop: {}", LabelProcessor.getBestMatch(getConfig().getLabel()));
        requiredActionPool.stop();
        optionalActionPool.stop();
    }

    @Override
    protected boolean isAutostartEnabled() throws CouldNotPerformException {
        return false;
    }
}
