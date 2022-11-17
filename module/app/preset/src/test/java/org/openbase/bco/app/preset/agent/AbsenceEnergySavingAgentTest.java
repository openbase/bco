package org.openbase.bco.app.preset.agent;

/*-
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


import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.app.test.agent.AbstractBCOAgentManagerTest;
import org.openbase.bco.dal.control.layer.unit.MotionDetectorController;
import org.openbase.bco.dal.lib.state.States;
import org.openbase.bco.dal.lib.state.States.Motion;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.MotionDetectorRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.agent.AgentRemote;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter;

import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.MotionStateType.MotionState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.agent.AgentDataType.AgentData;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import org.openbase.type.domotic.unit.dal.MotionDetectorDataType.MotionDetectorData;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class AbsenceEnergySavingAgentTest extends AbstractBCOAgentManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbsenceEnergySavingAgentTest.class);

    public static final String ABSENCE_ENERGY_SAVING_AGENT_LABEL = "Absence_Energy_Saving_Agent_Unit_Test";

    /**
     * Test of activate method, of class PowerStateSynchroniserAgent.
     *
     * @throws java.lang.Exception
     */
    @Test
//    @Timeout(10)
    public void testAbsenceEnergySavingAgent() throws Exception {
        System.out.println("testAbsenceEnergySavingAgent");

        UnitStateAwaiter<AgentData, AgentRemote> UnitStateAwaiter = new UnitStateAwaiter<>(agentRemote);
        UnitStateAwaiter.waitForState((AgentData data) -> data.getActivationState().getValue() == ActivationState.State.ACTIVE);

        Registries.waitForData();

        LocationRemote locationRemote = Units.getUnitByAlias(MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN, true, Units.LOCATION);
        ColorableLightRemote colorableLightRemote = Units.getUnit(Registries.getUnitRegistry().getUnitConfigsByLocationIdAndUnitType(MockRegistry.getUnitIdByAlias(MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN), UnitType.COLORABLE_LIGHT).get(0), true, Units.COLORABLE_LIGHT);
        MotionDetectorRemote motionDetectorRemote = Units.getUnit(Registries.getUnitRegistry().getUnitConfigsByLocationIdAndUnitType(MockRegistry.getUnitIdByAlias(MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN), UnitType.MOTION_DETECTOR).get(0), true, Units.MOTION_DETECTOR);
        MotionDetectorController motionDetectorController = (MotionDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(motionDetectorRemote.getId());

        UnitStateAwaiter<ColorableLightData, ColorableLightRemote> colorableLightStateAwaiter = new UnitStateAwaiter<>(colorableLightRemote);
        UnitStateAwaiter<MotionDetectorData, MotionDetectorRemote> motionDetectorStateAwaiter = new UnitStateAwaiter<>(motionDetectorRemote);
        UnitStateAwaiter<LocationData, LocationRemote> locationStateAwaiter = new UnitStateAwaiter<>(locationRemote);

        colorableLightRemote.waitForData();
        locationRemote.waitForData();
        motionDetectorRemote.waitForData();

        // create initial values with motion and lights on
        motionDetectorController.applyServiceState(Motion.MOTION, ServiceType.MOTION_STATE_SERVICE);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.PRESENT);

        final ActionParameter lowPriorityActionParameter = ActionParameter.newBuilder()
                .setActionInitiator(ActionInitiator.newBuilder().setInitiatorType(InitiatorType.SYSTEM).build())
                .setPriority(Priority.LOW)
                .setExecutionTimePeriod(1000000)
                .build();

        waitForExecution(locationRemote.setPowerState(States.Power.ON, lowPriorityActionParameter));
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals(MotionState.State.MOTION, motionDetectorRemote.getMotionState().getValue(), "MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to NO_MOTION");
        assertEquals(PresenceState.State.PRESENT, locationRemote.getPresenceState().getValue(), "PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to ABSENT.");
        assertEquals(PowerState.State.ON, locationRemote.getPowerState().getValue(), "PowerState of Location[" + locationRemote.getLabel() + "] has not switched to OFF");
        assertEquals(PowerState.State.ON, colorableLightRemote.getPowerState().getValue(), "PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched to OFF");

        // test if on no motion the lights are turned off
        motionDetectorController.applyServiceState(Motion.NO_MOTION, ServiceType.MOTION_STATE_SERVICE);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.NO_MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.ABSENT);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        //locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals(MotionState.State.NO_MOTION, motionDetectorRemote.getMotionState().getValue(), "MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to NO_MOTION");
        assertEquals(PresenceState.State.ABSENT, locationRemote.getPresenceState().getValue(), "PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to ABSENT.");
        assertEquals(PowerState.State.OFF, colorableLightRemote.getPowerState().getValue(), "PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched to OFF");
        //assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has not switched to OFF", PowerState.State.OFF, locationRemote.getPowerState().getValue());

        // test if the lights stay off on new motion
        motionDetectorController.applyServiceState(Motion.MOTION, ServiceType.MOTION_STATE_SERVICE);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.PRESENT);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        //locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals(MotionState.State.MOTION, motionDetectorRemote.getMotionState().getValue(), "MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to MOTION");
        assertEquals(PresenceState.State.PRESENT, locationRemote.getPresenceState().getValue(), "PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to PRESENT.");
        assertEquals(PowerState.State.OFF, colorableLightRemote.getPowerState().getValue(), "PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has changes without intention");
        //assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has changes without intention", PowerState.State.OFF, locationRemote.getPowerState().getValue());
    }

    @Override
    public UnitConfig getAgentConfig() throws CouldNotPerformException {
        return MockRegistry.generateAgentConfig(MockRegistry.LABEL_AGENT_CLASS_ABSENCE_ENERGY_SAVING, ABSENCE_ENERGY_SAVING_AGENT_LABEL, MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN).build();
    }
}
