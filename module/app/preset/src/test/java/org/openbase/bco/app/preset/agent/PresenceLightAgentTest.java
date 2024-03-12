package org.openbase.bco.app.preset.agent;

/*
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.app.test.agent.AbstractBCOAgentManagerTest;
import org.openbase.bco.dal.control.layer.unit.LightSensorController;
import org.openbase.bco.dal.control.layer.unit.MotionDetectorController;
import org.openbase.bco.dal.lib.state.States;
import org.openbase.bco.dal.lib.state.States.Illuminance;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.LightSensorRemote;
import org.openbase.bco.dal.remote.layer.unit.MotionDetectorRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState;
import org.openbase.type.domotic.state.MotionStateType.MotionState;
import org.openbase.type.domotic.state.MotionStateType.MotionState.State;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import org.openbase.type.domotic.unit.dal.LightSensorDataType.LightSensorData;
import org.openbase.type.domotic.unit.dal.MotionDetectorDataType.MotionDetectorData;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class PresenceLightAgentTest extends AbstractBCOAgentManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PresenceLightAgentTest.class);

    public static final String PRESENCE_LIGHT_AGENT_LABEL = "Presence_Light_Agent_Unit_Test";

    private static final PowerState OFF = PowerState.newBuilder().setValue(PowerState.State.OFF).build();

    private LocationRemote locationRemote;
    private ColorableLightRemote colorableLightRemote;
    private MotionDetectorRemote motionDetectorRemote;
    private LightSensorRemote lightSensorRemote;
    private MotionDetectorController motionDetectorController;
    private LightSensorController lightSensorController;
    private UnitStateAwaiter<ColorableLightData, ColorableLightRemote> colorableLightStateAwaiter;
    private UnitStateAwaiter<MotionDetectorData, MotionDetectorRemote> motionDetectorStateAwaiter;
    private UnitStateAwaiter<LightSensorData, LightSensorRemote> lightSensorStateAwaiter;
    private UnitStateAwaiter<LocationData, LocationRemote> locationStateAwaiter;

    //@BeforeAll //uncomment to enable debug mode
    public static void showActionInspector() throws Throwable {

        // JPService.registerProperty(JPDebugMode.class, true);
        // JPService.registerProperty(JPVerbose.class, true);

        // uncomment to visualize action inspector during tests
        // String[] args = {};
        // new Thread(() -> BCOActionInspector.main(args)).start();
    }

    @Override
    public void prepareEnvironment() throws CouldNotPerformException, InterruptedException {

        locationRemote = Units.getUnitByAlias(MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN, true, Units.LOCATION);
        colorableLightRemote = locationRemote.getUnits(UnitType.COLORABLE_LIGHT, true, Units.COLORABLE_LIGHT).get(0);
        motionDetectorRemote = locationRemote.getUnits(UnitType.MOTION_DETECTOR, true, Units.MOTION_DETECTOR).get(0);
        lightSensorRemote = locationRemote.getUnits(UnitType.LIGHT_SENSOR, true, Units.LIGHT_SENSOR).get(0);

        // set location emphasis to economy to make the agent more responsive on presence changes
        waitForExecution(locationRemote.setEconomyEmphasis(1));

        motionDetectorController = (MotionDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(motionDetectorRemote.getId());
        lightSensorController = (LightSensorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(lightSensorRemote.getId());

        colorableLightStateAwaiter = new UnitStateAwaiter<>(colorableLightRemote);
        motionDetectorStateAwaiter = new UnitStateAwaiter<>(motionDetectorRemote);
        lightSensorStateAwaiter = new UnitStateAwaiter<>(lightSensorRemote);
        locationStateAwaiter = new UnitStateAwaiter<>(locationRemote);

        colorableLightRemote.waitForData();
        locationRemote.waitForData();
        motionDetectorRemote.waitForData();
        lightSensorRemote.waitForData();

        // create initial values with no_motion and lights off and dark env
        motionDetectorController.applyServiceState(States.Motion.NO_MOTION, ServiceType.MOTION_STATE_SERVICE);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.NO_MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF, 3000);

        lightSensorController.applyServiceState(Illuminance.DARK, ServiceType.ILLUMINANCE_STATE_SERVICE);
        lightSensorStateAwaiter.waitForState((LightSensorData data) -> data.getIlluminanceState().getValue() == IlluminanceState.State.DARK);

        assertEquals(MotionState.State.NO_MOTION, motionDetectorRemote.getMotionState().getValue(), "Initial MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] is not NO_MOTION");
        assertEquals(PowerState.State.OFF, colorableLightRemote.getPowerState().getValue(), "Initial PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] is not OFF");
        assertEquals(PowerState.State.OFF, locationRemote.getPowerState().getValue(), "Initial PowerState of Location[" + locationRemote.getLabel() + "] is not OFF");
    }

    /**
     * Test of activate method, of class PreseceLightAgent.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(30)
    public void testPresenceLightAgent() throws Exception {

        // test if on motion the lights are turned on
        motionDetectorController.applyServiceState(States.Motion.MOTION, ServiceType.MOTION_STATE_SERVICE);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.PRESENT);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals(MotionState.State.MOTION, motionDetectorRemote.getMotionState().getValue(), "MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to MOTION");
        assertEquals(PresenceState.State.PRESENT, locationRemote.getPresenceState().getValue(), "PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to PRESENT.");
        assertEquals(PowerState.State.ON, colorableLightRemote.getPowerState().getValue(), "PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched to ON");
        assertEquals(PowerState.State.ON, locationRemote.getPowerState().getValue(), "PowerState of Location[" + locationRemote.getLabel() + "] has not switched to ON");

//        for (ActionDescription actionDescription : colorableLightRemote.getActionList()) {
//            System.out.println("action: " + actionDescription.getId());
//            System.out.println("action: " + MultiLanguageTextProcessor.getBestMatch(actionDescription.getDescription()));
//        }

        // test if the lights switch off after no motion
        motionDetectorController.applyServiceState(States.Motion.NO_MOTION, ServiceType.MOTION_STATE_SERVICE);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.NO_MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.ABSENT);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals(MotionState.State.NO_MOTION, motionDetectorRemote.getMotionState().getValue(), "MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to NO_MOTION");
        assertEquals(PresenceState.State.ABSENT, locationRemote.getPresenceState().getValue(), "PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to ABSENT.");
        assertEquals(PowerState.State.OFF, colorableLightRemote.getPowerState().getValue(), "PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched back to off");
        assertEquals(PowerState.State.OFF, locationRemote.getPowerState().getValue(), "PowerState of Location[" + locationRemote.getLabel() + "] has not switched back to off");

        // test if the lights stay off after bright illuminance
        lightSensorController.applyServiceState(Illuminance.SUNNY, ServiceType.ILLUMINANCE_STATE_SERVICE);
        motionDetectorController.applyServiceState(States.Motion.MOTION, ServiceType.MOTION_STATE_SERVICE);
        lightSensorStateAwaiter.waitForState((LightSensorData data) -> data.getIlluminanceState().getValue() == IlluminanceState.State.SUNNY);
        locationStateAwaiter.waitForState((LocationData data) -> data.getIlluminanceState().getValue() == IlluminanceState.State.SUNNY);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == State.MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.PRESENT);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals(MotionState.State.MOTION, motionDetectorRemote.getMotionState().getValue(), "MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to MOTION");
        assertEquals(PresenceState.State.PRESENT, locationRemote.getPresenceState().getValue(), "PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to PRESENT.");
        assertEquals(IlluminanceState.State.SUNNY, locationRemote.getIlluminanceState().getValue(), "Illuminance of Location[" + locationRemote.getLabel() + "] has not set to SUNNY.");
        assertEquals(PowerState.State.OFF, colorableLightRemote.getPowerState().getValue(), "PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not stayed off");
        assertEquals(PowerState.State.OFF, locationRemote.getPowerState().getValue(), "PowerState of Location[" + locationRemote.getLabel() + "] has not stayed off");

        // test if the lights switch on after darkness
        lightSensorController.applyServiceState(Illuminance.DARK, ServiceType.ILLUMINANCE_STATE_SERVICE);
        lightSensorStateAwaiter.waitForState((LightSensorData data) -> data.getIlluminanceState().getValue() == IlluminanceState.State.DARK);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals(IlluminanceState.State.DARK, locationRemote.getIlluminanceState().getValue(), "Illuminance of Location[" + locationRemote.getLabel() + "] has not set to BRIGHT.");
        assertEquals(PowerState.State.ON, colorableLightRemote.getPowerState().getValue(), "PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched on");
        assertEquals(PowerState.State.ON, locationRemote.getPowerState().getValue(), "PowerState of Location[" + locationRemote.getLabel() + "] has not switched on");

        // test if the lights switch off after no motion and brightness
        lightSensorController.applyServiceState(Illuminance.SUNNY, ServiceType.ILLUMINANCE_STATE_SERVICE);
        motionDetectorController.applyServiceState(States.Motion.NO_MOTION, ServiceType.MOTION_STATE_SERVICE);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.NO_MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.ABSENT);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        lightSensorStateAwaiter.waitForState((LightSensorData data) -> data.getIlluminanceState().getValue() == IlluminanceState.State.SUNNY);

        assertEquals(MotionState.State.NO_MOTION, motionDetectorRemote.getMotionState().getValue(), "MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to NO_MOTION");
        assertEquals(PresenceState.State.ABSENT, locationRemote.getPresenceState().getValue(), "PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to ABSENT.");
        assertEquals(PowerState.State.OFF, colorableLightRemote.getPowerState().getValue(), "PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched back to off");
        assertEquals(PowerState.State.OFF, locationRemote.getPowerState().getValue(), "PowerState of Location[" + locationRemote.getLabel() + "] has not switched back to off");
        assertEquals(IlluminanceState.State.SUNNY, locationRemote.getIlluminanceState().getValue(), "Illuminance of Location[" + locationRemote.getLabel() + "] has not set to BRIGHT.");

        // cancel initial control
        cancelAllTestActions();

        // test if all task are done.
        for (ActionDescription actionDescription : colorableLightRemote.requestData().get().getActionList()) {

            // ignore termination action because its always on the stack
            if (actionDescription.getPriority() == Priority.TERMINATION) {
                continue;
            }

            // make sure all other actions are done
            final RemoteAction remoteAction = new RemoteAction(actionDescription);
            remoteAction.waitForRegistration();
            assertEquals(true, remoteAction.isDone(), "There is still the running Action[" + MultiLanguageTextProcessor.getBestMatch(remoteAction.getActionDescription().getDescription(), "?") + "] found which should actually be terminated!");
        }
    }

    @Override
    public UnitConfig getAgentConfig() throws CouldNotPerformException {
        return MockRegistry.generateAgentConfig(MockRegistry.LABEL_AGENT_CLASS_PRESENCE_LIGHT, PRESENCE_LIGHT_AGENT_LABEL, MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN).build();
    }
}
