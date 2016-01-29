/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.core.preset;

import org.dc.bco.manager.agent.core.AbstractAgent;
import org.dc.bco.manager.agent.core.AgentRemote;
import org.dc.bco.registry.agent.remote.AgentRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
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
    private final ActivationState activate = ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build();
    private final ActivationState deactive = ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build();

    final AgentRemote agentBathAmbientColorBeachBottomRemote;
    final AgentRemote agentBathAmbientColorBeachCeiling;
    final AgentRemote agentBathAmbientColorForest;
    final AgentRemote agentBathAmbientColorNight;
    final AgentRemote agentBathAmbientColorZen;

    public SoundScapeAgent() throws CouldNotPerformException, InterruptedException {
        super(true);

        agentBathAmbientColorBeachBottomRemote = new AgentRemote();
        agentBathAmbientColorBeachCeiling = new AgentRemote();
        agentBathAmbientColorForest = new AgentRemote();
        agentBathAmbientColorNight = new AgentRemote();
        agentBathAmbientColorZen = new AgentRemote();

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
    public void init(final AgentConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            agentRegistryRemote.activate();
            listenerWatchDog.activate();
            agentBathAmbientColorBeachBottomRemote.init(agentRegistryRemote.getAgentConfigById("BathAmbientColorBeachBottom"));
            agentBathAmbientColorBeachCeiling.init(agentRegistryRemote.getAgentConfigById("BathAmbientColorBeachCeiling"));
            agentBathAmbientColorForest.init(agentRegistryRemote.getAgentConfigById("BathAmbientColorForest"));
            agentBathAmbientColorNight.init(agentRegistryRemote.getAgentConfigById("BathAmbientColorNight"));
            agentBathAmbientColorZen.init(agentRegistryRemote.getAgentConfigById("BathAmbientColorZen"));
            agentBathAmbientColorBeachBottomRemote.activate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        try {
            agentBathAmbientColorBeachCeiling.activate();
            agentBathAmbientColorForest.activate();
            agentBathAmbientColorNight.activate();
            agentBathAmbientColorZen.activate();
            agentBathAmbientColorBeachBottomRemote.setActivationState(deactive);
            agentBathAmbientColorBeachCeiling.setActivationState(deactive);
            agentBathAmbientColorForest.setActivationState(deactive);
            agentBathAmbientColorNight.setActivationState(deactive);
            agentBathAmbientColorZen.setActivationState(deactive);
            super.activate();
        } catch (InterruptedException | CouldNotPerformException ex) {
            logger.error("Could not activate SoundScopeAgent");
        }
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        try {
            agentBathAmbientColorBeachBottomRemote.setActivationState(deactive);
            agentBathAmbientColorBeachCeiling.setActivationState(deactive);
            agentBathAmbientColorForest.setActivationState(deactive);
            agentBathAmbientColorNight.setActivationState(deactive);
            agentBathAmbientColorZen.setActivationState(deactive);
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
                agentBathAmbientColorBeachBottomRemote.setActivationState(activate);
                agentBathAmbientColorBeachCeiling.setActivationState(activate);
                agentBathAmbientColorForest.setActivationState(deactive);
                agentBathAmbientColorNight.setActivationState(deactive);
                agentBathAmbientColorZen.setActivationState(deactive);
                break;
            case FOREST:
                logger.info("Case FOREST");
                agentBathAmbientColorBeachBottomRemote.setActivationState(deactive);
                agentBathAmbientColorBeachCeiling.setActivationState(deactive);
                agentBathAmbientColorForest.setActivationState(activate);
                agentBathAmbientColorNight.setActivationState(deactive);
                agentBathAmbientColorZen.setActivationState(deactive);
                break;
            case NIGHT:
                logger.info("Case NIGHT");
                agentBathAmbientColorBeachBottomRemote.setActivationState(deactive);
                agentBathAmbientColorBeachCeiling.setActivationState(deactive);
                agentBathAmbientColorForest.setActivationState(deactive);
                agentBathAmbientColorNight.setActivationState(activate);
                agentBathAmbientColorZen.setActivationState(deactive);
                break;
            case ZEN:
                logger.info("Case ZEN");
                agentBathAmbientColorBeachBottomRemote.setActivationState(deactive);
                agentBathAmbientColorBeachCeiling.setActivationState(deactive);
                agentBathAmbientColorForest.setActivationState(deactive);
                agentBathAmbientColorNight.setActivationState(deactive);
                agentBathAmbientColorZen.setActivationState(activate);
                break;
            case OFF:
                logger.info("Case OFF");
                agentBathAmbientColorBeachBottomRemote.setActivationState(deactive);
                agentBathAmbientColorBeachCeiling.setActivationState(deactive);
                agentBathAmbientColorForest.setActivationState(deactive);
                agentBathAmbientColorNight.setActivationState(deactive);
                agentBathAmbientColorZen.setActivationState(deactive);
        }
    }

    private AgentConfig setActivationState(AgentConfig config, ActivationState.State state) {
        return config.toBuilder().setActivationState(ActivationState.newBuilder().setValue(state)).build();
    }
}
