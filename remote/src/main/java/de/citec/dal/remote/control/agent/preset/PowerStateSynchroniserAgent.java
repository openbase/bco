/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.agent.preset;

import de.citec.dal.remote.unit.PowerPlugRemote;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import rst.configuration.EntryType;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.state.PowerStateType;
import rst.homeautomation.unit.PowerPlugType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

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

    private PowerPlugRemote sourceRemote, targetRemote;
    private UnitConfig source, target;
    private PowerStateSyncBehaviour sourceBehaviour, targetBehaviour;

    public PowerStateSynchroniserAgent(AgentConfig agentConfig) throws InstantiationException, CouldNotPerformException, InterruptedException {
        super(agentConfig);
        DeviceRegistryRemote deviceRegistryRemote = new DeviceRegistryRemote();
        deviceRegistryRemote.init(JPService.getProperty(JPDeviceRegistryScope.class).getValue());
        deviceRegistryRemote.activate();

        sourceRemote = new PowerPlugRemote();
        targetRemote = new PowerPlugRemote();

        for (EntryType.Entry entry : agentConfig.getMetaConfig().getEntryList()) {
            switch (entry.getKey()) {
                case "SOURCE":
                    this.source = deviceRegistryRemote.getUnitConfigById(entry.getValue());
                    this.sourceRemote.init(source);
                    break;
                case "TARGET":
                    this.target = deviceRegistryRemote.getUnitConfigById(entry.getValue());
                    this.targetRemote.init(target);
                    break;
                case "SOURCE_BEHAVIOUR":
                    this.sourceBehaviour = PowerStateSyncBehaviour.valueOf(entry.getValue());
                    break;
                case "TARGET_BEHAVIOUR":
                    this.targetBehaviour = PowerStateSyncBehaviour.valueOf(entry.getValue());
                    break;
                default:
                    logger.warn("Unknown meta config key [" + entry.getKey() + "] witch value [" + entry.getValue() + "]");
            }
        }

        deviceRegistryRemote.shutdown();

        initObserver();
    }

    private void initObserver() {
        sourceRemote.addObserver(new Observer<PowerPlugType.PowerPlug>() {

            @Override
            public void update(Observable<PowerPlugType.PowerPlug> source, PowerPlugType.PowerPlug data) throws Exception {
                if (data.getPowerState().getValue() == PowerStateType.PowerState.State.OFF) {
                    targetRemote.setPower(PowerStateType.PowerState.State.OFF);
                } else if (data.getPowerState().getValue() == PowerStateType.PowerState.State.ON) {
                    switch (targetBehaviour) {
                        case OFF:
                            targetRemote.setPower(PowerStateType.PowerState.State.OFF);
                            break;
                        case ON:
                            targetRemote.setPower(PowerStateType.PowerState.State.ON);
                            break;
                        case LAST_STATE:
                            break;
                    }
                }
            }
        });

        targetRemote.addObserver(new Observer<PowerPlugType.PowerPlug>() {

            @Override
            public void update(Observable<PowerPlugType.PowerPlug> source, PowerPlugType.PowerPlug data) throws Exception {
                if (data.getPowerState().getValue() == PowerStateType.PowerState.State.ON) {
                    sourceRemote.setPower(PowerStateType.PowerState.State.ON);
                } else if (data.getPowerState().getValue() == PowerStateType.PowerState.State.OFF) {
                    switch (sourceBehaviour) {
                        case OFF:
                            sourceRemote.setPower(PowerStateType.PowerState.State.OFF);
                            break;
                        case ON:
                            sourceRemote.setPower(PowerStateType.PowerState.State.ON);
                            break;
                        case LAST_STATE:
                            break;
                    }
                }
            }
        });
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
