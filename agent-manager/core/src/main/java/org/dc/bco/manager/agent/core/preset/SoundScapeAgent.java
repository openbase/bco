/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.core.preset;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.dc.bco.registry.agent.remote.AgentRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.rsb.com.RSBCommunicationService;
import org.dc.jul.extension.rsb.com.RSBFactory;
import org.dc.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.dc.jul.extension.rsb.iface.RSBListenerInterface;
import org.dc.jul.processing.StringProcessor;
import org.dc.jul.schedule.WatchDog;
import rsb.Event;
import rsb.Handler;
import rsb.Scope;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.state.ActivationStateType.ActivationState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class SoundScapeAgent extends AbstractAgent {

    private enum SoundScape {

        OFF,
        FOREST,
        BEACH,
        NIGHT,
        ZEN;
    }

    private final RSBListenerInterface listener;
    private final WatchDog listenerWatchDog;
    private final Scope scope = new Scope("/app/soundscape/theme/");
    private final AgentRegistryRemote agentRegistryRemote;

    public SoundScapeAgent(AgentConfig config) throws CouldNotPerformException, InterruptedException {
        super(config);
        logger.info("Creating sound scape agent with scope [" + scope.toString() + "]!");
        this.listener = RSBFactory.getInstance().createSynchronizedListener(scope, RSBSharedConnectionConfig.getParticipantConfig());
        this.listenerWatchDog = new WatchDog(listener, "RSBListener[" + scope.concat(RSBCommunicationService.SCOPE_SUFFIX_STATUS) + "]");
        listener.addHandler(new Handler() {

            @Override
            public void internalNotify(Event event) {
                logger.info("Got data [" + event.getData() + "]");
                if (event.getData() instanceof String) {
                    try {
                        controlAmbienAgents(SoundScape.valueOf(StringProcessor.transformToUpperCase((String) event.getData())));
                    } catch (CouldNotPerformException ex) {
                        logger.error("Could not de/activate ambient color agents");
                    } catch (IllegalArgumentException ex) {
                        logger.error("Unable to parse [" + event.getData() + "] as sound scape!");
                    }
                }
            }
        }, false);

        this.agentRegistryRemote = new AgentRegistryRemote();
        this.agentRegistryRemote.init();
    }

    @Override
    public void activate() {
        logger.info("Activate");
        try {
            agentRegistryRemote.activate();
            listenerWatchDog.activate();
            super.activate();
        } catch (InterruptedException | CouldNotPerformException ex) {
            logger.error("Could not activate SoundScopeAgent");
        }
    }

    @Override
    public void deactivate() {
        logger.info("Deactivate");
        try {
            agentRegistryRemote.deactivate();
            listenerWatchDog.deactivate();
            super.deactivate();
        } catch (InterruptedException | CouldNotPerformException ex) {
            logger.error("Could not deactivate SoundScopeAgent");
        }

    }

    private void controlAmbienAgents(SoundScape soundScape) throws CouldNotPerformException {
        switch (soundScape) {
            case BEACH:
                logger.info("Case BEACH");
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorBeachBottom"), ActivationState.State.ACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorBeachCeiling"), ActivationState.State.ACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorForest"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorNight"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorZen"), ActivationState.State.DEACTIVE));
                break;
            case FOREST:
                logger.info("Case FOREST");
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorBeachBottom"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorBeachCeiling"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorForest"), ActivationState.State.ACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorNight"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorZen"), ActivationState.State.DEACTIVE));
                break;
            case NIGHT:
                logger.info("Case NIGHT");
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorBeachBottom"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorBeachCeiling"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorForest"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorNight"), ActivationState.State.ACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorZen"), ActivationState.State.DEACTIVE));
                break;
            case ZEN:
                logger.info("Case ZEN");
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorBeachBottom"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorBeachCeiling"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorForest"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorNight"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorZen"), ActivationState.State.ACTIVE));
                break;
            case OFF:
                logger.info("Case OFF");
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorBeachBottom"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorBeachCeiling"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorForest"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorNight"), ActivationState.State.DEACTIVE));
                agentRegistryRemote.updateAgentConfig(setActivationState(agentRegistryRemote.getAgentConfigById("BathAmbientColorZen"), ActivationState.State.DEACTIVE));
        }
    }

    private AgentConfig setActivationState(AgentConfig config, ActivationState.State state) {
        return config.toBuilder().setActivationState(ActivationState.newBuilder().setValue(state)).build();
    }
}
