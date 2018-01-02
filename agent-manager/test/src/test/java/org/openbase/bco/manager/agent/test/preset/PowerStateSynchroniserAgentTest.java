package org.openbase.bco.manager.agent.test.preset;

/*
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
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.DimmerRemote;
import org.openbase.bco.dal.remote.unit.PowerSwitchRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.agent.AgentRemote;
import org.openbase.bco.dal.remote.unit.util.UnitStateAwaiter;
import org.openbase.bco.manager.agent.core.preset.PowerStateSynchroniserAgent;
import org.openbase.bco.registry.agent.remote.CachedAgentRegistryRemote;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.pattern.Observable;
import org.slf4j.LoggerFactory;
import rst.configuration.EntryType.Entry;
import rst.configuration.MetaConfigType.MetaConfig;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import rst.domotic.unit.dal.DimmerDataType.DimmerData;
import rst.domotic.unit.dal.PowerSwitchDataType.PowerSwitchData;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerStateSynchroniserAgentTest extends AbstractBCOAgentManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PowerStateSynchroniserAgentTest.class);

    public static final String POWER_STATE_SYNC_AGENT_LABEL = "Power_State_Sync_Agent_Unit_Test";

    private static final PowerState ON = PowerState.newBuilder().setValue(PowerState.State.ON).build();
    private static final PowerState OFF = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
    private static AgentRemote agent;

    public PowerStateSynchroniserAgentTest() {
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
     *
     * @throws java.lang.Exception
     */
    @Test//(timeout = 10000)
    public void testPowerStateSyncAgent() throws Exception {
        System.out.println("testPowerStateSyncAgent");

        CachedAgentRegistryRemote.waitForData();

        UnitConfig config = registerAgent();
        agent = Units.getUnit(config, true, Units.AGENT);
        agent.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()).get();

        // It can take some time until the execute() method of the agent has finished
        // TODO: enable to acces controller instances via remoteRegistry to check and wait for the execution of the agent
        //Thread.sleep(500);
        Registries.waitForData();

        DimmerRemote dimmerRemote = Units.getUnit(sourceId, true, Units.DIMMER);
        ColorableLightRemote colorableLightRemote = Units.getUnit(targetId1, true, Units.COLORABLE_LIGHT);
        PowerSwitchRemote powerSwitchRemote = Units.getUnit(targetId2, true, Units.POWER_SWITCH);
        
        UnitStateAwaiter<DimmerData, DimmerRemote> dimmerStateAwaiter = new UnitStateAwaiter(dimmerRemote);
        UnitStateAwaiter<ColorableLightData, ColorableLightRemote> colorableLightStateAwaiter = new UnitStateAwaiter(colorableLightRemote);
        UnitStateAwaiter<PowerSwitchData, PowerSwitchRemote> powerSwitchStateAwaiter = new UnitStateAwaiter(powerSwitchRemote);

        LOGGER.info("Dimmer id [" + sourceId + "]");
        LOGGER.info("Ambient light id [" + colorableLightRemote.getId() + "]");
        LOGGER.info("Power plug id [" + powerSwitchRemote.getId() + "]");

        dimmerRemote.setPowerState(OFF).get();
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        powerSwitchStateAwaiter.waitForState((PowerSwitchData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        LOGGER.info("Dimmer state [" + dimmerRemote.getPowerState().getValue() + "]");
        assertEquals("Dimmer[Source] has not been turned off", PowerState.State.OFF, dimmerRemote.getPowerState().getValue());
        assertEquals("AmbientLight[Target] has not been turned off as a reaction", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("PowerPlug[Target] has not been turned off as a reaction", PowerState.State.OFF, powerSwitchRemote.getPowerState().getValue());

        dimmerRemote.setPowerState(ON).get();
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        powerSwitchStateAwaiter.waitForState((PowerSwitchData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals("Dimmer[Source] has not been turned on", PowerState.State.ON, dimmerRemote.getPowerState().getValue());
        assertEquals("AmbientLight[Target] has not been turned on as a reaction", PowerState.State.ON, colorableLightRemote.getPowerState().getValue());
        assertEquals("PowerPlug[Target] has not been turned on as a reaction", PowerState.State.ON, powerSwitchRemote.getPowerState().getValue());

        colorableLightRemote.setPowerState(OFF).get();
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        powerSwitchStateAwaiter.waitForState((PowerSwitchData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals("AmbientLight[Target] has not been turned off", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("PowerPlug[Target] should not have turned off as a reaction", PowerState.State.ON, powerSwitchRemote.getPowerState().getValue());
        assertEquals("Dimmer[Source] should not have been turned off as a reaction. One target was still on.", PowerState.State.ON, dimmerRemote.getPowerState().getValue());

        powerSwitchRemote.setPowerState(OFF).get();
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        powerSwitchStateAwaiter.waitForState((PowerSwitchData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals("PowerPlug[Target] has not been turned off", PowerState.State.OFF, powerSwitchRemote.getPowerState().getValue());
        assertEquals("AmbientLight[Target] should still be off", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("Dimmer[Source] should have been turned off as a reaction.", PowerState.State.OFF, dimmerRemote.getPowerState().getValue());

        colorableLightRemote.setPowerState(ON).get();
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        powerSwitchStateAwaiter.waitForState((PowerSwitchData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals("AmbientLight[Target] has not been turned on", PowerState.State.ON, colorableLightRemote.getPowerState().getValue());
        assertEquals("PowerPlug[Target] should still be off", PowerState.State.OFF, powerSwitchRemote.getPowerState().getValue());
        assertEquals("Dimmer[Source] has not been turned on as a reaction", PowerState.State.ON, dimmerRemote.getPowerState().getValue());
    }

    private UnitConfig registerAgent() throws CouldNotPerformException, InterruptedException, ExecutionException {
        System.out.println("Register the PowerStateSynchroniserAgent...");
        Entry.Builder source = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.SOURCE_KEY);
        Entry.Builder target1 = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.TARGET_KEY + "_1");
        Entry.Builder target2 = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.TARGET_KEY + "_2");
        Entry.Builder sourceBehaviour = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.SOURCE_BEHAVIOUR_KEY).setValue("OFF");
        Entry.Builder targetBehaviour = Entry.newBuilder().setKey(PowerStateSynchroniserAgent.TARGET_BEHAVIOUR_KEY).setValue("ON");

        for (UnitConfig unit : Registries.getUnitRegistry().getDalUnitConfigs()) {
            if (unit.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                continue;
            }

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
        PlacementConfig.Builder placementConfig = PlacementConfig.newBuilder().setLocationId(Registries.getLocationRegistry().getRootLocationConfig().getId());

        String agentClassId = null;
        for (AgentClass agentClass : Registries.getAgentRegistry().getAgentClasses()) {
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
        return Registries.getAgentRegistry().registerAgentConfig(agentUnitConfig.build()).get();
    }
}
