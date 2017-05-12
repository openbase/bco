package org.openbase.bco.manager.agent.test.preset;

/*-
 * #%L
 * BCO Manager Agent Test
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.unit.MotionDetectorController;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.MotionDetectorRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.agent.AgentRemote;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.agent.core.AgentManagerLauncher;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.manager.location.core.LocationManagerLauncher;
import org.openbase.bco.registry.agent.lib.AgentRegistry;
import org.openbase.bco.registry.agent.remote.CachedAgentRegistryRemote;
import org.openbase.bco.registry.location.lib.LocationRegistry;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.slf4j.LoggerFactory;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.MotionStateType;
import rst.domotic.state.PowerStateType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PresenceStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.spatial.PlacementConfigType;

/**
 *
 * * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class AbsenceEnergySavingAgentTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbsenceEnergySavingAgentTest.class);

    public static final String ABSENCE_ENERGY_SAVING_AGENT_LABEL = "Absence_Energy_Saving_Agent_Unit_Test";

    private static final PowerState ON = PowerState.newBuilder().setValue(PowerState.State.ON).build();

    private static final MotionStateType.MotionState MOTION = MotionStateType.MotionState.newBuilder().setValue(MotionStateType.MotionState.State.MOTION).build();
    private static final MotionStateType.MotionState NO_MOTION = MotionStateType.MotionState.newBuilder().setValue(MotionStateType.MotionState.State.NO_MOTION).build();

    private static AgentRemote agent;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static AgentManagerLauncher agentManagerLauncher;
    private static LocationManagerLauncher locationManagerLauncher;

    private static AgentRegistry agentRegistry;
    private static LocationRegistry locationRegistry;

    public AbsenceEnergySavingAgentTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();

            MockRegistryHolder.newMockRegistry();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();

            agentManagerLauncher = new AgentManagerLauncher();
            agentManagerLauncher.launch();

            locationManagerLauncher = new LocationManagerLauncher();
            locationManagerLauncher.launch();

            agentRegistry = CachedAgentRegistryRemote.getRegistry();
            locationRegistry = CachedLocationRegistryRemote.getRegistry();

            Registries.getUnitRegistry().waitForData(30, TimeUnit.SECONDS);
            agentManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }
            if (agentManagerLauncher != null) {
                agentManagerLauncher.shutdown();
            }
            if (locationManagerLauncher != null) {
                locationManagerLauncher.shutdown();
            }
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of activate method, of class PowerStateSynchroniserAgent.
     */
    @Test(timeout = 30000)
    public void testAbsenceEnergySavingAgent() throws Exception {
        System.out.println("testAbsenceEnergySavingAgent");

        CachedAgentRegistryRemote.waitForData();

        UnitConfig config = registerAgent();
        agent = Units.getUnitByLabel(config.getLabel(), true, Units.AGENT);
        agent.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()).get();

        // It can take some time until the execute() method of the agent has finished
        // TODO: enable to acces controller instances via remoteRegistry to check and wait for the execution of the agent
        //Thread.sleep(500);
        Registries.waitForData();

        LocationRemote locationRemote = Units.getUnitByLabel("Paradise", true, Units.LOCATION);
        ColorableLightRemote colorableLightRemote = Units.getUnit(locationRegistry.getUnitConfigsByLocationLabel(UnitType.COLORABLE_LIGHT, "Paradise").get(0), true, Units.COLORABLE_LIGHT);
        MotionDetectorRemote motionDetectorRemote = Units.getUnit(locationRegistry.getUnitConfigsByLocationLabel(UnitType.MOTION_DETECTOR, "Paradise").get(0), true, Units.MOTION_DETECTOR);
        MotionDetectorController motionDetectorController = (MotionDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(motionDetectorRemote.getId());

        colorableLightRemote.waitForData();
        locationRemote.waitForData();
        motionDetectorRemote.waitForData();

        LOGGER.info("Ambient light id [" + colorableLightRemote.getId() + "]");

        motionDetectorController.updateMotionStateProvider(TimestampProcessor.updateTimestampWithCurrentTime(MOTION));
        Thread.sleep(100);
        locationRemote.setPowerState(ON).get();
        motionDetectorRemote.requestData().get();
        locationRemote.requestData().get();
        colorableLightRemote.requestData().get();
        assertEquals("Initial MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] is not MOTION", MotionStateType.MotionState.State.MOTION, motionDetectorRemote.getMotionState().getValue());
        assertEquals("Initial PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] is not ON", PowerStateType.PowerState.State.ON, colorableLightRemote.getPowerState().getValue());
        assertEquals("Initial PowerState of Location[" + locationRemote.getLabel() + "] is not ON", PowerStateType.PowerState.State.ON, locationRemote.getPowerState().getValue());

        motionDetectorController.updateMotionStateProvider(TimestampProcessor.updateTimestampWithCurrentTime(NO_MOTION));
        Thread.sleep(100);
        motionDetectorRemote.requestData().get();
        locationRemote.requestData().get();
        colorableLightRemote.requestData().get();
        assertEquals("MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to NO_MOTION", MotionStateType.MotionState.State.NO_MOTION, motionDetectorRemote.getMotionState().getValue());
        assertEquals("PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to ABSENT.", PresenceStateType.PresenceState.State.ABSENT, locationRemote.getPresenceState().getValue());
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched to OFF", PowerStateType.PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        //assertEquals("Initial PowerState of Location[" + locationRemote.getLabel() + "] has not switched to OFF", PowerStateType.PowerState.State.OFF, locationRemote.getPowerState().getValue());

        motionDetectorController.updateMotionStateProvider(TimestampProcessor.updateTimestampWithCurrentTime(MOTION));
        Thread.sleep(100);
        motionDetectorRemote.requestData().get();
        locationRemote.requestData().get();
        colorableLightRemote.requestData().get();
        assertEquals("MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to MOTION", MotionStateType.MotionState.State.MOTION, motionDetectorRemote.getMotionState().getValue());
        assertEquals("PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to PRESENT.", PresenceStateType.PresenceState.State.PRESENT, locationRemote.getPresenceState().getValue());
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has changes without intention", PowerStateType.PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        //assertEquals("Initial PowerState of Location[" + locationRemote.getLabel() + "] has changes without intention", PowerStateType.PowerState.State.ON, locationRemote.getPowerState().getValue());
    }

    private UnitConfig registerAgent() throws CouldNotPerformException, InterruptedException, ExecutionException {
        System.out.println("Register the AbsenceEnergySavingAgent...");

        EnablingState enablingState = EnablingState.newBuilder().setValue(EnablingState.State.ENABLED).build();
        PlacementConfigType.PlacementConfig.Builder placementConfig = PlacementConfigType.PlacementConfig.newBuilder().setLocationId(locationRegistry.getLocationConfigsByLabel("Paradise").get(0).getId());

        String agentClassId = null;
        for (AgentClass agentClass : agentRegistry.getAgentClasses()) {
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
        return agentRegistry.registerAgentConfig(agentUnitConfig.build()).get();
    }
}
