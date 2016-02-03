/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.core.preset;

import com.google.protobuf.GeneratedMessage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.dc.jul.extension.rsb.com.AbstractIdentifiableRemote;
import org.dc.bco.dal.remote.unit.UnitRemoteFactory;
import org.dc.bco.dal.remote.unit.UnitRemoteFactoryInterface;
import org.dc.bco.manager.agent.core.AbstractAgent;
import org.dc.bco.manager.agent.core.AgentManagerController;
import org.dc.bco.registry.device.lib.DeviceRegistry;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class PowerStateSynchroniserAgent extends AbstractAgent {

    public static final String SOURCE_KEY = "SOURCE";
    public static final String TARGET_KEY = "TARGET";
    public static final String SOURCE_BEHAVIOUR_KEY = "SOURCE_BEHAVIOUR";
    public static final String TARGET_BEHAVIOUR_KEY = "TARGET_BEHAVIOUR";

    public enum PowerStateSyncBehaviour {

        ON,
        OFF,
        LAST_STATE;
    }

    private PowerState.State sourceLatestPowerState;
    /**
     * State that determines what the targets do if the source changes and vice
     * versa.
     *
     * OFF when all targets are off. ON when at least one target is one.
     */
    private PowerState.State targetLatestPowerState;
    private List<AbstractIdentifiableRemote> targetRemotes = new ArrayList<>();
    private AbstractIdentifiableRemote sourceRemote;
    private PowerStateSyncBehaviour sourceBehaviour, targetBehaviour;
    private final UnitRemoteFactoryInterface factory;
    private final DeviceRegistry deviceRegistry;

    public PowerStateSynchroniserAgent() throws InstantiationException, CouldNotPerformException {
        super(true);
        this.factory = UnitRemoteFactory.getInstance();
        this.deviceRegistry = AgentManagerController.getInstance().getDeviceRegistry();
    }

    @Override
    public void init(AgentConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            logger.info("Creating PowerStateSynchroniserAgent[" + config.getLabel() + "]");

//            final DeviceRegistryRemote deviceRegistryRemote = new DeviceRegistryRemote();
//            System.out.println("### init["+config.getLabel()+"]");
//            deviceRegistryRemote.init();
//            System.out.println("### activate["+config.getLabel()+"]");
//            deviceRegistryRemote.activate();
            MetaConfigVariableProvider configVariableProvider = new MetaConfigVariableProvider("PowerStateSynchroniserAgent", config.getMetaConfig());

            sourceRemote = factory.createAndInitUnitRemote(deviceRegistry.getUnitConfigById(configVariableProvider.getValue(SOURCE_KEY)));
            int i = 1;
            String unitId;
            try {
                while (!(unitId = configVariableProvider.getValue(TARGET_KEY + "_" + i)).isEmpty()) {
                    logger.info("Found target id [" + unitId + "] with key [" + TARGET_KEY + "_" + i + "]");
                    targetRemotes.add(factory.createAndInitUnitRemote(deviceRegistry.getUnitConfigById(unitId)));
                    i++;
                }
            } catch (NotAvailableException ex) {
                i--;
                logger.info("Found [" + i + "] target/s");
            }
            sourceBehaviour = PowerStateSyncBehaviour.valueOf(configVariableProvider.getValue(SOURCE_BEHAVIOUR_KEY));
            targetBehaviour = PowerStateSyncBehaviour.valueOf(configVariableProvider.getValue(TARGET_BEHAVIOUR_KEY));

            System.out.println("### shutdown[" + config.getLabel() + "]");

            logger.info("Initializing observers");
            initObserver();
            //TODO mpohling: interrupted should be forwarded! Interface change needed!
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void initObserver() {
        sourceRemote.addObserver(new Observer<GeneratedMessage>() {

            @Override
            public void update(Observable<GeneratedMessage> source, GeneratedMessage data) throws Exception {
                sourceLatestPowerState = invokeGetPowerState(data).getValue();
                logger.info("Recieved new value [" + sourceLatestPowerState + "] for source");
                if (sourceLatestPowerState == PowerState.State.OFF) {
                    if (targetLatestPowerState != PowerState.State.OFF) {
                        for (AbstractIdentifiableRemote targetRemote : targetRemotes) {
                            invokeSetPower(targetRemote, PowerState.State.OFF);
                        }
                    }
                } else if (sourceLatestPowerState == PowerState.State.ON) {
                    switch (targetBehaviour) {
                        case OFF:
                            if (targetLatestPowerState != PowerState.State.OFF) {
                                for (AbstractIdentifiableRemote targetRemote : targetRemotes) {
                                    invokeSetPower(targetRemote, PowerState.State.OFF);
                                }
                            }
                            break;
                        case ON:
                            if (targetLatestPowerState != PowerState.State.ON) {
                                for (AbstractIdentifiableRemote targetRemote : targetRemotes) {
                                    invokeSetPower(targetRemote, PowerState.State.ON);
                                }
                            }
                            break;
                        case LAST_STATE:
                            break;
                    }
                }
            }
        });

        for (AbstractIdentifiableRemote targetRemote : targetRemotes) {
            targetRemote.addObserver(new Observer<GeneratedMessage>() {

                @Override
                public void update(Observable<GeneratedMessage> source, GeneratedMessage data) throws Exception {
                    PowerState.State newPowerState = invokeGetPowerState(data).getValue();
                    logger.info("Recieved new value [" + targetLatestPowerState + "] for target [" + ((AbstractIdentifiableRemote) source).getId() + "]");
                    if (!updateLatestTargetPowerState(newPowerState)) {
                        return;
                    }
                    if (targetLatestPowerState == PowerState.State.ON) {
                        if (sourceLatestPowerState != PowerState.State.ON) {
                            invokeSetPower(sourceRemote, PowerState.State.ON);
                        }
                    } else if (targetLatestPowerState == PowerState.State.OFF) {
                        switch (sourceBehaviour) {
                            case OFF:
                                if (sourceLatestPowerState != PowerState.State.OFF) {
                                    invokeSetPower(sourceRemote, PowerState.State.OFF);
                                }
                                break;
                            case ON:
                                if (sourceLatestPowerState != PowerState.State.ON) {
                                    invokeSetPower(sourceRemote, PowerState.State.ON);
                                }
                                break;
                            case LAST_STATE:
                                break;
                        }
                    }
                }
            });
        }
    }

    /**
     *
     * @param powerState
     * @return if the latest target power state has changed
     * @throws CouldNotPerformException
     */
    private boolean updateLatestTargetPowerState(PowerState.State powerState) throws CouldNotPerformException {
        if (targetLatestPowerState == PowerState.State.UNKNOWN) {
            targetLatestPowerState = powerState;
            return true;
        }
        if (targetLatestPowerState == PowerState.State.OFF && powerState == PowerState.State.ON) {
            targetLatestPowerState = PowerState.State.ON;
            return true;
        }

        if (targetLatestPowerState == PowerState.State.ON && powerState == PowerState.State.OFF) {
            targetLatestPowerState = PowerState.State.OFF;
            for (AbstractIdentifiableRemote targetRemote : targetRemotes) {
                if (invokeGetPowerState(targetRemote.getData()).getValue() == PowerState.State.ON) {
                    targetLatestPowerState = PowerState.State.ON;
                    break;
                }
            }
            return targetLatestPowerState == PowerState.State.OFF;
        }

        return false;
    }

    private void invokeSetPower(AbstractIdentifiableRemote remote, PowerState.State powerState) {
        try {
            Method method = remote.getClass().getMethod("setPower", PowerState.State.class);
            method.invoke(remote, powerState);
        } catch (NoSuchMethodException ex) {
            logger.error("Remote [" + remote.getClass().getSimpleName() + "] has no set Power method");
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            logger.error("Could not invoke setPower method on remote [" + remote.getClass().getSimpleName() + "] with value [" + powerState + "]");
        }
    }

    private PowerState invokeGetPowerState(GeneratedMessage message) throws CouldNotPerformException {
        try {
            Method method = message.getClass().getMethod("getPowerState");
            return (PowerState) method.invoke(message);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not get powerState from message [" + message + "]", ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getClass().getSimpleName() + "]");
        super.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getClass().getSimpleName() + "]");
        super.deactivate();
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.info("Executing PowerStateSynchroniser agent");
        sourceRemote.activate();
        String targetIds = "";
        targetLatestPowerState = PowerState.State.UNKNOWN;
        for (AbstractIdentifiableRemote targetRemote : targetRemotes) {
            targetRemote.activate();
            targetIds += "[" + targetRemote.getId() + "]";
            if ((targetLatestPowerState == PowerState.State.OFF || targetLatestPowerState == PowerState.State.UNKNOWN) && invokeGetPowerState(targetRemote.getData()).getValue() == PowerState.State.ON) {
                targetLatestPowerState = PowerState.State.ON;
            } else if (targetLatestPowerState == PowerState.State.UNKNOWN && invokeGetPowerState(targetRemote.getData()).getValue() == PowerState.State.OFF) {
                targetLatestPowerState = PowerState.State.OFF;
            }
        }
        logger.info("Source [" + sourceRemote.getId() + "], behaviour [" + sourceBehaviour + "]");
        logger.info("Targets [" + targetIds + "], behaviour [" + targetBehaviour + "]");
        sourceLatestPowerState = invokeGetPowerState(sourceRemote.getData()).getValue();
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        sourceRemote.deactivate();
        for (AbstractIdentifiableRemote targetRemote : targetRemotes) {
            targetRemote.deactivate();
        }
    }

    public AbstractIdentifiableRemote getSourceRemote() {
        return sourceRemote;
    }

    public List<AbstractIdentifiableRemote> getTargetRemotes() {
        return targetRemotes;
    }

    public PowerStateSyncBehaviour getSourceBehaviour() {
        return sourceBehaviour;
    }

    public PowerStateSyncBehaviour getTargetBehaviour() {
        return targetBehaviour;
    }
}
