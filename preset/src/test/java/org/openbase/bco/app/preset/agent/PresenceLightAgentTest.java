package org.openbase.bco.app.preset.agent;

/*
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 - 2019 openbase.org
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

import org.openbase.app.test.agent.AbstractBCOAgentManagerTest;
import org.junit.Test;
import org.openbase.bco.dal.control.layer.unit.LightSensorController;
import org.openbase.bco.dal.control.layer.unit.MotionDetectorController;
import org.openbase.bco.dal.remote.action.Actions;
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
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState;
import org.openbase.type.domotic.state.MotionStateType.MotionState.State;
import org.openbase.type.domotic.unit.dal.LightSensorDataType.LightSensorData;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.MotionStateType.MotionState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import org.openbase.type.domotic.unit.dal.MotionDetectorDataType.MotionDetectorData;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class PresenceLightAgentTest extends AbstractBCOAgentManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PresenceLightAgentTest.class);

    public static final String PRESENCE_LIGHT_AGENT_LABEL = "Presence_Light_Agent_Unit_Test";

    private static final PowerState OFF = PowerState.newBuilder().setValue(PowerState.State.OFF).build();

    private static final double ILLUMINANCEVALUE_DARK = 20d;
    private static final double ILLUMINANCEVALUE_BRIGHT = 500d;

    private static final MotionState MOTION = MotionState.newBuilder().setValue(MotionState.State.MOTION).build();
    private static final MotionState NO_MOTION = MotionState.newBuilder().setValue(MotionState.State.NO_MOTION).build();

    private static final IlluminanceState DARK = IlluminanceState.newBuilder().setIlluminance(ILLUMINANCEVALUE_DARK).build();
    private static final IlluminanceState BRIGHT = IlluminanceState.newBuilder().setIlluminance(ILLUMINANCEVALUE_BRIGHT).build();

    public PresenceLightAgentTest() {
        // uncomment to visualize action inspector during tests
//        String[] args = {};
//        new Thread(() -> {
//            try {
//                Registries.waitForData();
//                BCOActionInspector.main(args);
//            } catch (CouldNotPerformException e) {
//                e.printStackTrace();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }).start();
    }

    /**
     * Test of activate method, of class PreseceLightAgent.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 20000)
    public void testPreseceLightAgent() throws Exception {
        System.out.println("testPreseceLightAgent");

        LocationRemote locationRemote = Units.getUnitByAlias(MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN, true, Units.LOCATION);
        ColorableLightRemote colorableLightRemote = locationRemote.getUnits(UnitType.COLORABLE_LIGHT, true, Units.COLORABLE_LIGHT).get(0);
        MotionDetectorRemote motionDetectorRemote = locationRemote.getUnits(UnitType.MOTION_DETECTOR, true, Units.MOTION_DETECTOR).get(0);
        LightSensorRemote lightSensorRemote = locationRemote.getUnits(UnitType.LIGHT_SENSOR, true, Units.LIGHT_SENSOR).get(0);

        // set location emphasis to economy to make the agent more responsive on presence changes
        Actions.waitForExecution(locationRemote.setEconomyEmphasis(1));

        MotionDetectorController motionDetectorController = (MotionDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(motionDetectorRemote.getId());
        LightSensorController lightSensorController = (LightSensorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(lightSensorRemote.getId());

        UnitStateAwaiter<ColorableLightData, ColorableLightRemote> colorableLightStateAwaiter = new UnitStateAwaiter<>(colorableLightRemote);
        UnitStateAwaiter<MotionDetectorData, MotionDetectorRemote> motionDetectorStateAwaiter = new UnitStateAwaiter<>(motionDetectorRemote);
        UnitStateAwaiter<LightSensorData, LightSensorRemote> lightSensorStateAwaiter = new UnitStateAwaiter<>(lightSensorRemote);
        UnitStateAwaiter<LocationData, LocationRemote> locationStateAwaiter = new UnitStateAwaiter<>(locationRemote);

        colorableLightRemote.waitForData();
        locationRemote.waitForData();
        motionDetectorRemote.waitForData();
        lightSensorRemote.waitForData();

        // create initial values with no_motion and lights off and dark env
        motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(NO_MOTION), ServiceType.MOTION_STATE_SERVICE);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.NO_MOTION);
        final RemoteAction locationOffAction = new RemoteAction(locationRemote.setPowerState(OFF), () -> true);
        locationOffAction.waitForSubmission();

        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        lightSensorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(DARK), ServiceType.ILLUMINANCE_STATE_SERVICE);
        lightSensorStateAwaiter.waitForState((LightSensorData data) -> data.getIlluminanceState().getIlluminance() == ILLUMINANCEVALUE_DARK);

        assertEquals("Initial MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] is not NO_MOTION", MotionState.State.NO_MOTION, motionDetectorRemote.getMotionState().getValue());
        assertEquals("Initial PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] is not OFF", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("Initial PowerState of Location[" + locationRemote.getLabel() + "] is not OFF", PowerState.State.OFF, locationRemote.getPowerState().getValue());


        // test if on motion the lights are turned on
        motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(MOTION), ServiceType.MOTION_STATE_SERVICE);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.PRESENT);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals("MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to MOTION", MotionState.State.MOTION, motionDetectorRemote.getMotionState().getValue());
        assertEquals("PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to PRESENT.", PresenceState.State.PRESENT, locationRemote.getPresenceState().getValue());
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched to ON", PowerState.State.ON, colorableLightRemote.getPowerState().getValue());
        assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has not switched to ON", PowerState.State.ON, locationRemote.getPowerState().getValue());

//        for (ActionDescription actionDescription : colorableLightRemote.getActionList()) {
//            System.out.println("action: " + actionDescription.getId());
//            System.out.println("action: " + MultiLanguageTextProcessor.getBestMatch(actionDescription.getDescription()));
//        }

        // test if the lights switch off after no motion
        motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(NO_MOTION), ServiceType.MOTION_STATE_SERVICE);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.NO_MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.ABSENT);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals("MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to NO_MOTION", MotionState.State.NO_MOTION, motionDetectorRemote.getMotionState().getValue());
        assertEquals("PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to ABSENT.", PresenceState.State.ABSENT, locationRemote.getPresenceState().getValue());
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched back to off", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has not switched back to off", PowerState.State.OFF, locationRemote.getPowerState().getValue());

        // test if the lights stay off after bright illuminance
        lightSensorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(BRIGHT), ServiceType.ILLUMINANCE_STATE_SERVICE);
        motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(MOTION), ServiceType.MOTION_STATE_SERVICE);
        lightSensorStateAwaiter.waitForState((LightSensorData data) -> data.getIlluminanceState().getIlluminance() == ILLUMINANCEVALUE_BRIGHT);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == State.MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.PRESENT);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals("MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to MOTION", MotionState.State.MOTION, motionDetectorRemote.getMotionState().getValue());
        assertEquals("PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to PRESENT.", PresenceState.State.PRESENT, locationRemote.getPresenceState().getValue());
        assertEquals("Illuminance of Location[" + locationRemote.getLabel() + "] has not set to BRIGHT.", ILLUMINANCEVALUE_BRIGHT, locationRemote.getIlluminanceState().getIlluminance(), 0.1d);
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not stayed off", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has not stayed off", PowerState.State.OFF, locationRemote.getPowerState().getValue());


        // test if the lights switch on after darkness
        lightSensorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(DARK), ServiceType.ILLUMINANCE_STATE_SERVICE);
        lightSensorStateAwaiter.waitForState((LightSensorData data) -> data.getIlluminanceState().getIlluminance() == ILLUMINANCEVALUE_DARK);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals("Illuminance of Location[" + locationRemote.getLabel() + "] has not set to BRIGHT.", ILLUMINANCEVALUE_DARK, locationRemote.getIlluminanceState().getIlluminance(), 0.1d);
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched on", PowerState.State.ON, colorableLightRemote.getPowerState().getValue());
        assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has not switched on", PowerState.State.ON, locationRemote.getPowerState().getValue());


        // test if the lights switch off after no motion and brightness
        lightSensorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(BRIGHT), ServiceType.ILLUMINANCE_STATE_SERVICE);
        motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(NO_MOTION), ServiceType.MOTION_STATE_SERVICE);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.NO_MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.ABSENT);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        lightSensorStateAwaiter.waitForState((LightSensorData data) -> data.getIlluminanceState().getIlluminance() == ILLUMINANCEVALUE_BRIGHT);

        assertEquals("MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to NO_MOTION", MotionState.State.NO_MOTION, motionDetectorRemote.getMotionState().getValue());
        assertEquals("PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to ABSENT.", PresenceState.State.ABSENT, locationRemote.getPresenceState().getValue());
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched back to off", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has not switched back to off", PowerState.State.OFF, locationRemote.getPowerState().getValue());
        assertEquals("Illuminance of Location[" + locationRemote.getLabel() + "] has not set to BRIGHT.", ILLUMINANCEVALUE_BRIGHT, locationRemote.getIlluminanceState().getIlluminance(), 0.1d);

        // cancel initial control
        locationOffAction.cancel().get(5, TimeUnit.SECONDS);

        // test if all task are done.
        List<RemoteAction> actionList = new ArrayList<>();
        for (ActionDescription actionDescription : colorableLightRemote.requestData().get().getActionList()) {
            final RemoteAction remoteAction = new RemoteAction(actionDescription);
            remoteAction.waitForSubmission();
            assertEquals("There is still the running Action["+MultiLanguageTextProcessor.getBestMatch(remoteAction.getActionDescription().getDescription(), "?")+"] found which should actually be terminated!", true, remoteAction.isDone());
        }
    }

    @Override
    public UnitConfig getAgentConfig() throws CouldNotPerformException {
        return MockRegistry.generateAgentConfig(MockRegistry.LABEL_AGENT_CLASS_PRESENCE_LIGHT, PRESENCE_LIGHT_AGENT_LABEL, MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN).build();
    }
}
