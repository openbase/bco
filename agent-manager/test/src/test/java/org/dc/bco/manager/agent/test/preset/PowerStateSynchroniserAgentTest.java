/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.test.preset;

/*
 * #%L
 * COMA AgentManager Test
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.dal.remote.unit.AmbientLightRemote;
import org.dc.bco.dal.remote.unit.DimmerRemote;
import org.dc.bco.dal.remote.unit.PowerPlugRemote;
import org.dc.bco.manager.agent.core.AgentManagerLauncher;
import org.dc.bco.manager.agent.core.preset.PowerStateSynchroniserAgent;
import org.dc.bco.manager.agent.remote.AgentRemote;
import org.dc.bco.manager.device.core.DeviceManagerLauncher;
import org.dc.bco.registry.agent.remote.AgentRegistryRemote;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import rst.configuration.EntryType.Entry;
import rst.configuration.MetaConfigType.MetaConfig;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.state.ActivationStateType.ActivationState;
import rst.homeautomation.state.EnablingStateType.EnablingState;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class PowerStateSynchroniserAgentTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PowerStateSynchroniserAgentTest.class);

    public static final String POWER_STATE_SYNC_AGENT_LABEL = "Power_State_Sync_Agent_Unit_Test";

    private static PowerState ON = PowerState.newBuilder().setValue(PowerState.State.ON).build();
    private static PowerState OFF = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
    private static AgentRemote agent;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static AgentManagerLauncher agentManagerLauncher;
    private static MockRegistry registry;
    private static AgentRegistryRemote agentRemote;
    private static DeviceRegistryRemote deviceRemote;
    private static LocationRegistryRemote locationRemote;

    public PowerStateSynchroniserAgentTest() {
    }

    @BeforeClass
    public static void setUpClass() throws CouldNotPerformException, InstantiationException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();

        agentManagerLauncher = new AgentManagerLauncher();
        agentManagerLauncher.launch();

        agentRemote = new AgentRegistryRemote();
        agentRemote.init();
        agentRemote.activate();

        deviceRemote = new DeviceRegistryRemote();
        deviceRemote.init();
        deviceRemote.activate();

        locationRemote = new LocationRegistryRemote();
        locationRemote.init();
        locationRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (agentManagerLauncher != null) {
            agentManagerLauncher.shutdown();
        }
        if (agentRemote != null) {
            agentRemote.shutdown();
        }
        if (deviceRemote != null) {
            deviceRemote.shutdown();
        }
        if (locationRemote != null) {
            locationRemote.shutdown();
        }
        if (registry != null) {
            MockRegistryHolder.shutdownMockRegistry();
        }
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
    @Test
    public void testPowerStateSyncAgent() throws Exception {
        System.out.println("testPowerStateSyncAgent");

        AgentConfig config = registerAgent();
        agent = new AgentRemote();
        agent.init(config);
        agent.activate();
        agent.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build());

        DimmerRemote dimmerRemote = new DimmerRemote();
        AmbientLightRemote ambientLightRemote = new AmbientLightRemote();
        PowerPlugRemote powerPlugRemote = new PowerPlugRemote();
        dimmerRemote.init(deviceRemote.getUnitConfigById(sourceId));
        ambientLightRemote.init(deviceRemote.getUnitConfigById(targetId1));
        powerPlugRemote.init(deviceRemote.getUnitConfigById(targetId2));
        dimmerRemote.activate();
        ambientLightRemote.activate();
        powerPlugRemote.activate();

        Thread.sleep(2000);

        logger.info("\nDimmer id [" + sourceId + "]");
        logger.info("Ambient light id [" + ambientLightRemote.getId() + "]");
        logger.info("Power plug id [" + powerPlugRemote.getId() + "]");

        dimmerRemote.setPower(OFF);
        Thread.sleep(50);
        dimmerRemote.requestStatus();
        logger.info("\nDimmer state [" + dimmerRemote.getPower().getValue() + "]\n");
        ambientLightRemote.requestStatus();
        powerPlugRemote.requestStatus();
        assertEquals("Dimmer[Source] has not been turned off", PowerState.State.OFF, dimmerRemote.getPower().getValue());
        assertEquals("AmbientLight[Target] has not been turned off as a reaction", PowerState.State.OFF, ambientLightRemote.getPower().getValue());
        assertEquals("PowerPlug[Target] has not been turned off as a reaction", PowerState.State.OFF, powerPlugRemote.getPower().getValue());

        dimmerRemote.setPower(ON);
        dimmerRemote.requestStatus();
        Thread.sleep(50);
        ambientLightRemote.requestStatus();
        powerPlugRemote.requestStatus();
        assertEquals("Dimmer[Source] has not been turned on", PowerState.State.ON, dimmerRemote.getPower().getValue());
        assertEquals("AmbientLight[Target] has not been turned on as a reaction", PowerState.State.ON, ambientLightRemote.getPower().getValue());
        assertEquals("PowerPlug[Target] has not been turned on as a reaction", PowerState.State.ON, powerPlugRemote.getPower().getValue());

        ambientLightRemote.setPower(OFF);
        ambientLightRemote.requestStatus();
        Thread.sleep(50);
        dimmerRemote.requestStatus();
        powerPlugRemote.requestStatus();
        assertEquals("AmbientLight[Target] has not been turned off", PowerState.State.OFF, ambientLightRemote.getPower().getValue());
        assertEquals("PowerPlug[Target] should not have turned off as a reaction", PowerState.State.ON, powerPlugRemote.getPower().getValue());
        assertEquals("Dimmer[Source] should not have been turned off as a reaction. One target was still on.", PowerState.State.ON, dimmerRemote.getPower().getValue());

        powerPlugRemote.setPower(OFF);
        powerPlugRemote.requestStatus();
        Thread.sleep(50);
        ambientLightRemote.requestStatus();
        dimmerRemote.requestStatus();
        assertEquals("PowerPlug[Target] has not been turned off", PowerState.State.OFF, powerPlugRemote.getPower().getValue());
        assertEquals("AmbientLight[Target] should still be off", PowerState.State.OFF, ambientLightRemote.getPower().getValue());
        assertEquals("Dimmer[Source] should have been turned off as a reaction.", PowerState.State.OFF, dimmerRemote.getPower().getValue());

        ambientLightRemote.setPower(ON);
        ambientLightRemote.requestStatus();
        Thread.sleep(50);
        dimmerRemote.requestStatus();
        powerPlugRemote.requestStatus();
        assertEquals("AmbientLight[Target] has not been turned on", PowerState.State.ON, ambientLightRemote.getPower().getValue());
        assertEquals("PowerPlug[Target] should still be off", PowerState.State.OFF, powerPlugRemote.getPower().getValue());
        assertEquals("Dimmer[Source] has not been turned on as a reaction", PowerState.State.ON, dimmerRemote.getPower().getValue());

        dimmerRemote.shutdown();
        ambientLightRemote.shutdown();
        powerPlugRemote.shutdown();
        agent.deactivate();
    }

    private AgentConfig registerAgent() throws CouldNotPerformException {
        Entry.Builder source = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.SOURCE_KEY);
        Entry.Builder target1 = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.TARGET_KEY + "_1");
        Entry.Builder target2 = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.TARGET_KEY + "_2");
        Entry.Builder sourceBehaviour = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.SOURCE_BEHAVIOUR_KEY).setValue("OFF");
        Entry.Builder targetBehaviour = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.TARGET_BEHAVIOUR_KEY).setValue("ON");

        for (UnitConfig unit : deviceRemote.getUnitConfigs()) {
            if (unit.getType() == UnitType.DIMMER && source.getValue().isEmpty()) {
                sourceId = unit.getId();
                source.setValue(unit.getId());
            } else if (unit.getType() == UnitType.AMBIENT_LIGHT && target1.getValue().isEmpty()) {
                targetId1 = unit.getId();
                target1.setValue(unit.getId());
            } else if (unit.getType() == UnitType.POWER_PLUG && target2.getValue().isEmpty()) {
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
        return agentRemote.registerAgentConfig(AgentConfig.newBuilder().setLabel(POWER_STATE_SYNC_AGENT_LABEL).setLocationId(locationRemote.getRootLocationConfig().getId()).setMetaConfig(metaConfig)
                .setEnablingState(enablingState).setType(AgentConfig.AgentType.POWER_STATE_SYNCHRONISER).build());
    }
}
