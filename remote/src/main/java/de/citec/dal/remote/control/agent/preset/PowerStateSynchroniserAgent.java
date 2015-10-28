/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.agent.preset;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.remote.unit.DALRemoteService;
import de.citec.dal.remote.unit.UnitRemoteFactory;
import de.citec.dal.remote.unit.UnitRemoteFactoryInterface;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import rst.configuration.EntryType;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class PowerStateSynchroniserAgent extends AbstractAgent {

    private enum PowerStateSyncBehaviour {

        ON,
        OFF,
        LAST_STATE;
    }

    private DALRemoteService sourceRemote, targetRemote;
    private PowerStateSyncBehaviour sourceBehaviour, targetBehaviour;
    private UnitRemoteFactoryInterface factory;

    public PowerStateSynchroniserAgent(AgentConfig agentConfig) throws InstantiationException, CouldNotPerformException, InterruptedException {
        super(agentConfig);
        factory = UnitRemoteFactory.getInstance();

        DeviceRegistryRemote deviceRegistryRemote = new DeviceRegistryRemote();
        deviceRegistryRemote.init(JPService.getProperty(JPDeviceRegistryScope.class).getValue());
        deviceRegistryRemote.activate();

        for (EntryType.Entry entry : agentConfig.getMetaConfig().getEntryList()) {
            switch (entry.getKey()) {
                case "SOURCE":
                    sourceRemote = factory.createAndInitUnitRemote(deviceRegistryRemote.getUnitConfigById(entry.getValue()));
                    break;
                case "TARGET":
                    targetRemote = factory.createAndInitUnitRemote(deviceRegistryRemote.getUnitConfigById(entry.getValue()));
                    break;
                case "SOURCE_BEHAVIOUR":
                    this.sourceBehaviour = PowerStateSyncBehaviour.valueOf(entry.getValue());
                    break;
                case "TARGET_BEHAVIOUR":
                    this.targetBehaviour = PowerStateSyncBehaviour.valueOf(entry.getValue());
                    break;
                default:
                    logger.debug("Unknown meta config key [" + entry.getKey() + "] with value [" + entry.getValue() + "]");
            }
        }

        deviceRegistryRemote.shutdown();

        initObserver();
    }

    private void initObserver() {
        sourceRemote.addObserver(new Observer<GeneratedMessage>() {

            @Override
            public void update(Observable<GeneratedMessage> source, GeneratedMessage data) throws Exception {
                if (invokeGetPowerState(data).getValue() == PowerState.State.OFF) {
                    invokeSetPower(targetRemote, PowerState.State.OFF);
                } else if (invokeGetPowerState(data).getValue() == PowerState.State.ON) {
                    switch (targetBehaviour) {
                        case OFF:
                            invokeSetPower(targetRemote, PowerState.State.OFF);
                            break;
                        case ON:
                            invokeSetPower(targetRemote, PowerState.State.ON);
                            break;
                        case LAST_STATE:
                            break;
                    }
                }
            }
        });

        targetRemote.addObserver(new Observer<GeneratedMessage>() {

            @Override
            public void update(Observable<GeneratedMessage> source, GeneratedMessage data) throws Exception {
                if (invokeGetPowerState(data).getValue() == PowerState.State.ON) {
                    invokeSetPower(sourceRemote, PowerState.State.ON);
                } else if (invokeGetPowerState(data).getValue() == PowerState.State.OFF) {
                    switch (sourceBehaviour) {
                        case OFF:
                            invokeSetPower(sourceRemote, PowerState.State.OFF);
                            break;
                        case ON:
                            invokeSetPower(sourceRemote, PowerState.State.ON);
                            break;
                        case LAST_STATE:
                            break;
                    }
                }
            }
        });
    }

    private void invokeSetPower(DALRemoteService remote, PowerState.State powerState) {
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
        sourceRemote.activate();
        targetRemote.deactivate();
        super.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        sourceRemote.deactivate();
        targetRemote.deactivate();
        super.deactivate();
    }

}
