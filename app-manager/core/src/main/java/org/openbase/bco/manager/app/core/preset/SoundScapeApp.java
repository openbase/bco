package org.openbase.bco.manager.app.core.preset;

/*
 * #%L
 * BCO Manager App Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.agent.AgentRemote;
import org.openbase.bco.manager.app.core.AbstractAppController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rsb.com.RSBCommunicationService;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBListener;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.WatchDog;
import rsb.Event;
import rsb.Scope;
import rst.domotic.state.ActivationStateType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SoundScapeApp extends AbstractAppController {

    private enum SoundScape {

        OFF,
        FOREST,
        BEACH,
        NIGHT,
        ZEN;
    }

    private final RSBListener listener;
    private final WatchDog listenerWatchDog;
    private final Scope themeScope = new Scope("/app/soundscape/theme/");
    private final ActivationStateType.ActivationState activate = ActivationStateType.ActivationState.newBuilder().setValue(ActivationStateType.ActivationState.State.ACTIVE).build();
    private final ActivationStateType.ActivationState deactive = ActivationStateType.ActivationState.newBuilder().setValue(ActivationStateType.ActivationState.State.DEACTIVE).build();

    private AgentRemote agentBathAmbientColorBeachCeiling;
    private AgentRemote agentBathAmbientColorForest;
    private AgentRemote agentBathAmbientColorNight;
    private AgentRemote agentBathAmbientColorZen;

    public SoundScapeApp() throws CouldNotPerformException, InterruptedException {
        super(SoundScapeApp.class);
        logger.debug("Creating sound scape app with scope [" + themeScope.toString() + "]!");
        this.listener = RSBFactoryImpl.getInstance().createSynchronizedListener(themeScope, RSBSharedConnectionConfig.getParticipantConfig());
        this.listenerWatchDog = new WatchDog(listener, "RSBListener[" + themeScope.concat(RSBCommunicationService.SCOPE_SUFFIX_STATUS) + "]");
        listener.addHandler((Event event) -> {
            logger.debug("Got data [" + event.getData() + "]");
            if (event.getData() instanceof String) {
                try {
                    controlAmbienAgents(SoundScape.valueOf(StringProcessor.transformToUpperCase((String) event.getData())));
                } catch (CouldNotPerformException ex) {
                    logger.error("Could not de/activate ambient color agents");
                } catch (IllegalArgumentException ex) {
                    logger.error("Unable to parse [" + event.getData() + "] as sound scape!");
                }
            }
        }, false);
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        try {
            agentBathAmbientColorBeachCeiling = Units.getUnit("BathAmbientColorBeachCeiling", false, Units.UNIT_BASE_AGENT);
            agentBathAmbientColorForest = Units.getUnit("BathAmbientColorForest", false, Units.UNIT_BASE_AGENT);
            agentBathAmbientColorNight = Units.getUnit("BathAmbientColorNight", false, Units.UNIT_BASE_AGENT);
            agentBathAmbientColorZen = Units.getUnit("BathAmbientColorZen", false, Units.UNIT_BASE_AGENT);
            listenerWatchDog.activate();
        } catch (InterruptedException | CouldNotPerformException ex) {
            logger.error("Could not activate SoundScopeAgent");
        }
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        try {
            if (agentBathAmbientColorBeachCeiling != null) {
                agentBathAmbientColorBeachCeiling.setActivationState(deactive);
            }
            if (agentBathAmbientColorForest != null) {
                agentBathAmbientColorForest.setActivationState(deactive);
            }
            if (agentBathAmbientColorNight != null) {
                agentBathAmbientColorNight.setActivationState(deactive);
            }
            if (agentBathAmbientColorZen != null) {
                agentBathAmbientColorZen.setActivationState(deactive);
            }
            if (listenerWatchDog != null) {
                listenerWatchDog.deactivate();
            }
        } catch (InterruptedException | CouldNotPerformException ex) {
            logger.error("Could not deactivate SoundScopeAgent");
        }
    }

    private void controlAmbienAgents(SoundScape soundScape) throws CouldNotPerformException {
        switch (soundScape) {
            case BEACH:
                logger.debug("Case BEACH");
                agentBathAmbientColorBeachCeiling.setActivationState(activate);
                agentBathAmbientColorForest.setActivationState(deactive);
                agentBathAmbientColorNight.setActivationState(deactive);
                agentBathAmbientColorZen.setActivationState(deactive);
                break;
            case FOREST:
                logger.debug("Case FOREST");
                agentBathAmbientColorBeachCeiling.setActivationState(deactive);
                agentBathAmbientColorForest.setActivationState(activate);
                agentBathAmbientColorNight.setActivationState(deactive);
                agentBathAmbientColorZen.setActivationState(deactive);
                break;
            case NIGHT:
                logger.debug("Case NIGHT");
                agentBathAmbientColorBeachCeiling.setActivationState(deactive);
                agentBathAmbientColorForest.setActivationState(deactive);
                agentBathAmbientColorNight.setActivationState(activate);
                agentBathAmbientColorZen.setActivationState(deactive);
                break;
            case ZEN:
                logger.debug("Case ZEN");
                agentBathAmbientColorBeachCeiling.setActivationState(deactive);
                agentBathAmbientColorForest.setActivationState(deactive);
                agentBathAmbientColorNight.setActivationState(deactive);
                agentBathAmbientColorZen.setActivationState(activate);
                break;
            case OFF:
                logger.debug("Case OFF");
                agentBathAmbientColorBeachCeiling.setActivationState(deactive);
                agentBathAmbientColorForest.setActivationState(deactive);
                agentBathAmbientColorNight.setActivationState(deactive);
                agentBathAmbientColorZen.setActivationState(deactive);
        }
    }
}
