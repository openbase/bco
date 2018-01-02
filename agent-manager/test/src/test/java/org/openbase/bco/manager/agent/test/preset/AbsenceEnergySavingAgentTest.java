package org.openbase.bco.manager.agent.test.preset;

/*-
 * #%L
 * BCO Manager Agent Test
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import java.util.concurrent.ExecutionException;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.openbase.bco.dal.lib.jp.JPResourceAllocation;
import org.openbase.bco.dal.lib.layer.unit.MotionDetectorController;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.MotionDetectorRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.agent.AgentRemote;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.unit.util.UnitStateAwaiter;
import org.openbase.bco.registry.agent.remote.CachedAgentRegistryRemote;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.slf4j.LoggerFactory;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.MotionStateType.MotionState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.domotic.unit.agent.AgentDataType.AgentData;
import rst.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import rst.domotic.unit.dal.MotionDetectorDataType.MotionDetectorData;
import rst.domotic.unit.location.LocationDataType.LocationData;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo
 * Michalski</a>
 */
public class AbsenceEnergySavingAgentTest extends AbstractBCOAgentManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbsenceEnergySavingAgentTest.class);

    public static final String ABSENCE_ENERGY_SAVING_AGENT_LABEL = "Absence_Energy_Saving_Agent_Unit_Test";

    private static final PowerState ON = PowerState.newBuilder().setValue(PowerState.State.ON).build();

    private static final MotionState MOTION = MotionState.newBuilder().setValue(MotionState.State.MOTION).build();
    private static final MotionState NO_MOTION = MotionState.newBuilder().setValue(MotionState.State.NO_MOTION).build();
    private static final String LOCATION_LABEL = "Stairway to Heaven";

    private AgentRemote agent;

    public AbsenceEnergySavingAgentTest() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of activate method, of class PowerStateSynchroniserAgent.
     *
     * @throws java.lang.Exception
     */
    @Test//(timeout = 10000)
    public void testAbsenceEnergySavingAgent() throws Exception {
        // TODO: turn back on when resource allocation is integrated for unit tests
        try {
            if (!JPService.getProperty(JPResourceAllocation.class).getValue()) {
                return;
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not access JPResourceAllocation property", ex);
        }

        System.out.println("testAbsenceEnergySavingAgent");
        CachedAgentRegistryRemote.waitForData();

        UnitConfig config = registerAgent();
        agent = Units.getUnit(config, true, Units.AGENT);
        agent.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()).get();
        UnitStateAwaiter<AgentData, AgentRemote> agentStateAwaiter = new UnitStateAwaiter(agent);
        agentStateAwaiter.waitForState((AgentData data) -> data.getActivationState().getValue() == ActivationState.State.ACTIVE);

        // It can take some time until the execute() method of the agent has finished
        // TODO: enable to acces controller instances via remoteRegistry to check and wait for the execution of the agent
        Registries.waitForData();

        LocationRemote locationRemote = Units.getUnitsByLabel(LOCATION_LABEL, true, Units.LOCATION).get(0);
        ColorableLightRemote colorableLightRemote = Units.getUnit(Registries.getLocationRegistry().getUnitConfigsByLocationLabel(UnitType.COLORABLE_LIGHT, LOCATION_LABEL).get(0), true, Units.COLORABLE_LIGHT);
        MotionDetectorRemote motionDetectorRemote = Units.getUnit(Registries.getLocationRegistry().getUnitConfigsByLocationLabel(UnitType.MOTION_DETECTOR, LOCATION_LABEL).get(0), true, Units.MOTION_DETECTOR);
        MotionDetectorController motionDetectorController = (MotionDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(motionDetectorRemote.getId());

        UnitStateAwaiter<ColorableLightData, ColorableLightRemote> colorableLightStateAwaiter = new UnitStateAwaiter(colorableLightRemote);
        UnitStateAwaiter<MotionDetectorData, MotionDetectorRemote> motionDetectorStateAwaiter = new UnitStateAwaiter(motionDetectorRemote);
        UnitStateAwaiter<LocationData, LocationRemote> locationStateAwaiter = new UnitStateAwaiter(locationRemote);

        colorableLightRemote.waitForData();
        locationRemote.waitForData();
        motionDetectorRemote.waitForData();

        // create intial values with motion and lights on
        motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(MOTION));
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.PRESENT);
        locationRemote.setPowerState(ON).get();
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals("MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to NO_MOTION", MotionState.State.MOTION, motionDetectorRemote.getMotionState().getValue());
        assertEquals("PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to ABSENT.", PresenceState.State.PRESENT, locationRemote.getPresenceState().getValue());
        assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has not switched to OFF", PowerState.State.ON, locationRemote.getPowerState().getValue());
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched to OFF", PowerState.State.ON, colorableLightRemote.getPowerState().getValue());

        // test if on no motion the lights are turned off
        motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(NO_MOTION));
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.NO_MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.ABSENT);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        //locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals("MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to NO_MOTION", MotionState.State.NO_MOTION, motionDetectorRemote.getMotionState().getValue());
        assertEquals("PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to ABSENT.", PresenceState.State.ABSENT, locationRemote.getPresenceState().getValue());
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched to OFF", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        //assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has not switched to OFF", PowerState.State.OFF, locationRemote.getPowerState().getValue());

        // test if the lights stay off on new motion
        motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(MOTION));
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.PRESENT);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        //locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals("MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to MOTION", MotionState.State.MOTION, motionDetectorRemote.getMotionState().getValue());
        assertEquals("PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to PRESENT.", PresenceState.State.PRESENT, locationRemote.getPresenceState().getValue());
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has changes without intention", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        //assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has changes without intention", PowerState.State.OFF, locationRemote.getPowerState().getValue());
    }

    private UnitConfig registerAgent() throws CouldNotPerformException, InterruptedException, ExecutionException {
        System.out.println("Register the AbsenceEnergySavingAgent...");

        EnablingState enablingState = EnablingState.newBuilder().setValue(EnablingState.State.ENABLED).build();
        PlacementConfig.Builder placementConfig = PlacementConfig.newBuilder().setLocationId(Registries.getLocationRegistry().getLocationConfigsByLabel(LOCATION_LABEL).get(0).getId());

        String agentClassId = null;
        for (AgentClass agentClass : Registries.getAgentRegistry().getAgentClasses()) {
            if (MockRegistry.ABSENCE_ENERGY_SAVING_AGENT_LABEL.equals(agentClass.getLabel())) {
                agentClassId = agentClass.getId();
            }
        }
        if (agentClassId == null) {
            throw new CouldNotPerformException("Could not find id for AgentClass with label [" + MockRegistry.ABSENCE_ENERGY_SAVING_AGENT_LABEL + "]");
        }
        System.out.println("Foung agentClassId: [" + agentClassId + "]");

        UnitConfig.Builder agentUnitConfig = UnitConfig.newBuilder().setLabel(ABSENCE_ENERGY_SAVING_AGENT_LABEL).setType(UnitType.AGENT).setPlacementConfig(placementConfig).setEnablingState(enablingState);
        agentUnitConfig.getAgentConfigBuilder().setAgentClassId(agentClassId);
        return Registries.getAgentRegistry().registerAgentConfig(agentUnitConfig.build()).get();
    }
}
