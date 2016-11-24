package org.openbase.bco.manager.agent.test.preset;

/*
 * #%L
 * COMA AgentManager Test
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.DimmerRemote;
import org.openbase.bco.dal.remote.unit.PowerSwitchRemote;
import org.openbase.bco.manager.agent.core.AgentManagerLauncher;
import org.openbase.bco.manager.agent.core.preset.PowerStateSynchroniserAgent;
import org.openbase.bco.dal.remote.unit.agent.AgentRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.registry.agent.lib.AgentRegistry;
import org.openbase.bco.registry.agent.remote.CachedAgentRegistryRemote;
import org.openbase.bco.registry.location.lib.LocationRegistry;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.slf4j.LoggerFactory;
import rst.configuration.EntryType.Entry;
import rst.configuration.MetaConfigType.MetaConfig;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.spatial.PlacementConfigType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerStateSynchroniserAgentTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PowerStateSynchroniserAgentTest.class);

    public static final String POWER_STATE_SYNC_AGENT_LABEL = "Power_State_Sync_Agent_Unit_Test";

    private static final PowerState ON = PowerState.newBuilder().setValue(PowerState.State.ON).build();
    private static final PowerState OFF = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
    private static AgentRemote agent;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static AgentManagerLauncher agentManagerLauncher;

    private static AgentRegistry agentRegistry;
    private static UnitRegistry unitRegistry;
    private static LocationRegistry locationRegistry;

    public PowerStateSynchroniserAgentTest() {
    }

    @BeforeClass
    public static void setUpClass() throws CouldNotPerformException, InstantiationException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);

        MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();

        agentManagerLauncher = new AgentManagerLauncher();
        agentManagerLauncher.launch();

        agentRegistry = CachedAgentRegistryRemote.getRegistry();
        unitRegistry = CachedUnitRegistryRemote.getRegistry();
        locationRegistry = CachedLocationRegistryRemote.getRegistry();

        deviceManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);
        agentManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);
    }

    @AfterClass
    public static void tearDownClass() {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (agentManagerLauncher != null) {
            agentManagerLauncher.shutdown();
        }
        if (agentRegistry != null) {
            agentRegistry.shutdown();
        }
        MockRegistryHolder.shutdownMockRegistry();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private String sourceId;
    private String targetId1;
    private String targetId2;

    /**
     * Test of activate method, of class PowerStateSynchroniserAgent.
     */
    @Test(timeout = 30000)
    public void testPowerStateSyncAgent() throws Exception {
        System.out.println("testPowerStateSyncAgent");

        CachedAgentRegistryRemote.waitForData();
        UnitConfig config = registerAgent();
        agent = new AgentRemote();
        agent.init(config);
        agent.activate();
        agent.requestData().get();
        agent.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()).get();

        // It can take some time until the execute() method of the agent has finished
        // TODO: enable to acces controller instances via remoteRegistry to check and wait for the execution of the agent
        Thread.sleep(500);

        DimmerRemote dimmerRemote = new DimmerRemote();
        ColorableLightRemote ambientLightRemote = new ColorableLightRemote();
        PowerSwitchRemote powerPlugRemote = new PowerSwitchRemote();
        dimmerRemote.init(unitRegistry.getUnitConfigById(sourceId));
        ambientLightRemote.init(unitRegistry.getUnitConfigById(targetId1));
        powerPlugRemote.init(unitRegistry.getUnitConfigById(targetId2));
        dimmerRemote.activate();
        ambientLightRemote.activate();
        powerPlugRemote.activate();

        dimmerRemote.waitForData();
        ambientLightRemote.waitForData();
        powerPlugRemote.waitForData();

        logger.info("Dimmer id [" + sourceId + "]");
        logger.info("Ambient light id [" + ambientLightRemote.getId() + "]");
        logger.info("Power plug id [" + powerPlugRemote.getId() + "]");

        dimmerRemote.setPowerState(OFF).get();
        Thread.sleep(50);
        dimmerRemote.requestData().get();
        logger.info("Dimmer state [" + dimmerRemote.getPowerState().getValue() + "]");
        ambientLightRemote.requestData().get();
        powerPlugRemote.requestData().get();
        assertEquals("Dimmer[Source] has not been turned off", PowerState.State.OFF, dimmerRemote.getPowerState().getValue());
        assertEquals("AmbientLight[Target] has not been turned off as a reaction", PowerState.State.OFF, ambientLightRemote.getPowerState().getValue());
        assertEquals("PowerPlug[Target] has not been turned off as a reaction", PowerState.State.OFF, powerPlugRemote.getPowerState().getValue());

        dimmerRemote.setPowerState(ON).get();
        dimmerRemote.requestData().get();
        Thread.sleep(50);
        ambientLightRemote.requestData().get();
        powerPlugRemote.requestData().get();
        assertEquals("Dimmer[Source] has not been turned on", PowerState.State.ON, dimmerRemote.getPowerState().getValue());
        assertEquals("AmbientLight[Target] has not been turned on as a reaction", PowerState.State.ON, ambientLightRemote.getPowerState().getValue());
        assertEquals("PowerPlug[Target] has not been turned on as a reaction", PowerState.State.ON, powerPlugRemote.getPowerState().getValue());

        ambientLightRemote.setPowerState(OFF).get();
        ambientLightRemote.requestData().get();
        Thread.sleep(50);
        dimmerRemote.requestData().get();
        powerPlugRemote.requestData().get();
        assertEquals("AmbientLight[Target] has not been turned off", PowerState.State.OFF, ambientLightRemote.getPowerState().getValue());
        assertEquals("PowerPlug[Target] should not have turned off as a reaction", PowerState.State.ON, powerPlugRemote.getPowerState().getValue());
        assertEquals("Dimmer[Source] should not have been turned off as a reaction. One target was still on.", PowerState.State.ON, dimmerRemote.getPowerState().getValue());

        powerPlugRemote.setPowerState(OFF).get();
        powerPlugRemote.requestData().get();
        Thread.sleep(50);
        ambientLightRemote.requestData().get();
        dimmerRemote.requestData().get();
        assertEquals("PowerPlug[Target] has not been turned off", PowerState.State.OFF, powerPlugRemote.getPowerState().getValue());
        assertEquals("AmbientLight[Target] should still be off", PowerState.State.OFF, ambientLightRemote.getPowerState().getValue());
        assertEquals("Dimmer[Source] should have been turned off as a reaction.", PowerState.State.OFF, dimmerRemote.getPowerState().getValue());

        ambientLightRemote.setPowerState(ON).get();
        ambientLightRemote.requestData().get();
        Thread.sleep(50);
        dimmerRemote.requestData().get();
        powerPlugRemote.requestData().get();
        assertEquals("AmbientLight[Target] has not been turned on", PowerState.State.ON, ambientLightRemote.getPowerState().getValue());
        assertEquals("PowerPlug[Target] should still be off", PowerState.State.OFF, powerPlugRemote.getPowerState().getValue());
        assertEquals("Dimmer[Source] has not been turned on as a reaction", PowerState.State.ON, dimmerRemote.getPowerState().getValue());

        dimmerRemote.shutdown();
        ambientLightRemote.shutdown();
        powerPlugRemote.shutdown();
        agent.deactivate();
    }

    private UnitConfig registerAgent() throws CouldNotPerformException, InterruptedException, ExecutionException {
        System.out.println("Register the PowerStateSynchroniserAgent...");
        Entry.Builder source = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.SOURCE_KEY);
        Entry.Builder target1 = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.TARGET_KEY + "_1");
        Entry.Builder target2 = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.TARGET_KEY + "_2");
        Entry.Builder sourceBehaviour = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.SOURCE_BEHAVIOUR_KEY).setValue("OFF");
        Entry.Builder targetBehaviour = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.TARGET_BEHAVIOUR_KEY).setValue("ON");

        for (UnitConfig unit : unitRegistry.getUnitConfigs()) {
            if (unit.getType() == UnitType.DIMMER && source.getValue().isEmpty()) {
                sourceId = unit.getId();
                source.setValue(unit.getId());
            } else if (unit.getType() == UnitType.COLORABLE_LIGHT && target1.getValue().isEmpty()) {
                targetId1 = unit.getId();
                target1.setValue(unit.getId());
            } else if (unit.getType() == UnitType.POWER_SWITCH && target2.getValue().isEmpty()) {
                targetId2 = unit.getId();
                target2.setValue(unit.getId());
            }

            if (source.hasValue() && target1.hasValue() && target2.hasValue()) {
                break;
            }
        }

        MetaConfig metaConfig = MetaConfig.newBuilder()
                .addEntry(source)
                .addEntry(target1)
                .addEntry(target2)
                .addEntry(sourceBehaviour)
                .addEntry(targetBehaviour).build();
        EnablingState enablingState = EnablingState.newBuilder().setValue(EnablingState.State.ENABLED).build();
        PlacementConfigType.PlacementConfig.Builder placementConfig = PlacementConfigType.PlacementConfig.newBuilder().setLocationId(locationRegistry.getRootLocationConfig().getId());

        String agentClassId = null;
        for (AgentClass agentClass : agentRegistry.getAgentClasses()) {
            if (MockRegistry.POWER_STATE_SYNCHRONISER_AGENT_LABEL.equals(agentClass.getLabel())) {
                agentClassId = agentClass.getId();
            }
        }
        if (agentClassId == null) {
            throw new CouldNotPerformException("Could not find id for AgentClass with label [" + MockRegistry.POWER_STATE_SYNCHRONISER_AGENT_LABEL + "]");
        }
        System.out.println("Foung agentClassId: [" + agentClassId + "]");

        UnitConfig.Builder agentUnitConfig = UnitConfig.newBuilder().setLabel(POWER_STATE_SYNC_AGENT_LABEL).setType(UnitType.AGENT).setPlacementConfig(placementConfig).setMetaConfig(metaConfig).setEnablingState(enablingState);
        agentUnitConfig.getAgentConfigBuilder().setAgentClassId(agentClassId);
        return agentRegistry.registerAgentConfig(agentUnitConfig.build()).get();
    }
}
