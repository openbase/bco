package org.dc.bco.manager.scene.core;

/*
 * #%L
 * COMA SceneManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.bco.dal.remote.control.action.Action;
import org.dc.bco.dal.remote.unit.ButtonRemote;
import org.dc.bco.manager.scene.lib.Scene;
import org.dc.bco.manager.scene.lib.SceneController;
import org.dc.bco.registry.device.lib.DeviceRegistry;
import org.dc.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.extension.rsb.com.AbstractExecutableController;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.action.ActionConfigType.ActionConfig;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.control.scene.SceneDataType.SceneData;
import rst.homeautomation.state.ActivationStateType;
import rst.homeautomation.state.ActivationStateType.ActivationState;
import rst.homeautomation.state.ButtonStateType.ButtonState;
import rst.homeautomation.unit.ButtonType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;

/**
 *
 * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class SceneControllerImpl extends AbstractExecutableController<SceneData, SceneData.Builder, SceneConfig> implements SceneController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationStateType.ActivationState.getDefaultInstance()));
    }

    private final List<ButtonRemote> buttonRemoteList;
    private final List<Action> actionList;
    private final Observer<ButtonType.Button> buttonObserver;

    private DeviceRegistry deviceRegistry;

    public SceneControllerImpl() throws org.dc.jul.exception.InstantiationException {
        super(SceneData.newBuilder(), false);
        this.buttonRemoteList = new ArrayList<>();
        this.actionList = new ArrayList<>();

        this.buttonObserver = new Observer<ButtonType.Button>() {

            @Override
            public void update(Observable<ButtonType.Button> source, ButtonType.Button data) throws Exception {
                if (data.getButtonState().getValue().equals(ButtonState.State.CLICKED)) {
                    setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build());
                }
            }
        };
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        try {
            logger.info("post init " + getConfig().getLabel());
            this.deviceRegistry = CachedDeviceRegistryRemote.getDeviceRegistry();
            ButtonRemote buttonRemote;
            try {
                for (UnitConfig unitConfig : deviceRegistry.getUnitConfigsByLabel(getConfig().getLabel())) {
                    //TODO implement deviceregistry method get unit by label and type.
                    if (unitConfig.getType() != UnitTemplateType.UnitTemplate.UnitType.BUTTON) {
                        continue;
                    }
                    try {
                        buttonRemote = new ButtonRemote();
                        buttonRemote.init(unitConfig);
                        buttonRemoteList.add(buttonRemote);
                    } catch (InitializationException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not register remote for Button[" + unitConfig.getLabel() + "]!"), logger);
                    }
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not init all related button remotes.", ex), logger);
            }

            Action action;
            for (ActionConfig actionConfig : getConfig().getActionConfigList()) {
                action = new Action();
                action.init(actionConfig);
                actionList.add(action);
            }

//            if (getConfig().getLabel().equals("Test3")) {
//                logger.info("### init test unit " + getConfig().getLabel());
//                PowerPlugRemote testplug = new PowerPlugRemote();
//                testplug.initByLabel("A10C1");
//                testplug.activate();
//                testplug.addObserver(new Observer<PowerPlugType.PowerPlug>() {
//
//                    @Override
//                    public void update(Observable<PowerPlugType.PowerPlug> source, PowerPlugType.PowerPlug data) throws Exception {
//                        logger.info("### got change!");
//                        if(data.getPowerState().getValue().equals(PowerStateType.PowerState.State.ON)) {
//                            logger.info("### activate scene!");
//                            setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build());
//                        }
//                    }
//                });
//            }

        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }

        super.postInit();
    }

    @Override
    public void enable() throws CouldNotPerformException, InterruptedException {
        logger.info("enable " + getConfig().getLabel());
        super.enable();
        for (ButtonRemote button : buttonRemoteList) {
            button.activate();
            button.addObserver(buttonObserver);
        }
    }

    @Override
    public void disable() throws CouldNotPerformException, InterruptedException {
        logger.info("disable " + getConfig().getLabel());
        for (ButtonRemote button : buttonRemoteList) {
            button.removeObserver(buttonObserver);
            button.deactivate();
        }
        super.disable();
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Scene.class, this, server);
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.info("Activate scene: " + getConfig().getLabel());
        for (Action action : actionList) {
            action.execute();
        }

        // TODO mpohling: implement external execution service
        Thread thread = new Thread() {

            @Override
            public void run() {
                try {
                    for (Action action : actionList) {
                        try {
                            action.waitForFinalization();
                        } catch (InterruptedException ex) {
                            ExceptionPrinter.printHistory(ex, logger);
                            break;
                        }
                    }
                    setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).build());
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not wait for actions!", ex), logger);
                }
            }
        };
        thread.start();
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        logger.info("Finished scene: " + getConfig().getLabel());
    }
}
