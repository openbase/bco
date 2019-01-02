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
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.DimmerRemote;
import org.openbase.bco.dal.remote.layer.unit.PowerSwitchRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter;

import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.slf4j.LoggerFactory;
import org.openbase.type.configuration.EntryType.Entry;
import org.openbase.type.configuration.MetaConfigType.MetaConfig;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import org.openbase.type.domotic.unit.dal.DimmerDataType.DimmerData;
import org.openbase.type.domotic.unit.dal.PowerSwitchDataType.PowerSwitchData;
import org.openbase.type.vision.HSBColorType.HSBColor;

import static org.junit.Assert.assertEquals;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerStateSynchroniserAgentTest extends AbstractBCOAgentManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PowerStateSynchroniserAgentTest.class);

    private static final String AGENT_ALIAS = "Power_State_Sync_Agent_Unit_Test";

    private static final PowerState ON = PowerState.newBuilder().setValue(PowerState.State.ON).build();
    private static final PowerState OFF = PowerState.newBuilder().setValue(PowerState.State.OFF).build();

    public PowerStateSynchroniserAgentTest() {
    }

    private String sourceId;
    private String targetId1;
    private String targetId2;

    /**
     * Test of activate method, of class PowerStateSynchroniserAgent.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testPowerStateSyncAgent() throws Exception {
        System.out.println("testPowerStateSyncAgent");

        Registries.waitForData();

        // It can take some time until the execute() method of the agent has finished
        // TODO: enable to access controller instances via remoteRegistry to check and wait for the execution of the agent
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

        LOGGER.info("Turn everything off");
        dimmerRemote.setPowerState(OFF).get();
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        powerSwitchStateAwaiter.waitForState((PowerSwitchData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        LOGGER.info("Dimmer state [" + dimmerRemote.getPowerState().getValue() + "]");
        assertEquals("Dimmer[Source] has not been turned off", PowerState.State.OFF, dimmerRemote.getPowerState().getValue());
        assertEquals("AmbientLight[Target] has not been turned off as a reaction", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("PowerPlug[Target] has not been turned off as a reaction", PowerState.State.OFF, powerSwitchRemote.getPowerState().getValue());

        LOGGER.info("Turn source on");
        dimmerRemote.setPowerState(ON).get();
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        powerSwitchStateAwaiter.waitForState((PowerSwitchData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals("Dimmer[Source] has not been turned on", PowerState.State.ON, dimmerRemote.getPowerState().getValue());
        assertEquals("AmbientLight[Target] has not been turned on as a reaction", PowerState.State.ON, colorableLightRemote.getPowerState().getValue());
        assertEquals("PowerPlug[Target] has not been turned on as a reaction", PowerState.State.ON, powerSwitchRemote.getPowerState().getValue());

        LOGGER.info("Turn target1 off");
        colorableLightRemote.setPowerState(OFF).get();
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        powerSwitchStateAwaiter.waitForState((PowerSwitchData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals("AmbientLight[Target] has not been turned off", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("PowerPlug[Target] should not have turned off as a reaction", PowerState.State.ON, powerSwitchRemote.getPowerState().getValue());
        assertEquals("Dimmer[Source] should not have been turned off as a reaction. One target was still on.", PowerState.State.ON, dimmerRemote.getPowerState().getValue());

        LOGGER.info("Turn target2 off");
        powerSwitchRemote.setPowerState(OFF).get();
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        powerSwitchStateAwaiter.waitForState((PowerSwitchData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals("PowerPlug[Target] has not been turned off", PowerState.State.OFF, powerSwitchRemote.getPowerState().getValue());
        assertEquals("AmbientLight[Target] should still be off", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("Dimmer[Source] should have been turned off as a reaction.", PowerState.State.OFF, dimmerRemote.getPowerState().getValue());

        LOGGER.info("Turn target1 on again");
        colorableLightRemote.setPowerState(ON).get();
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        powerSwitchStateAwaiter.waitForState((PowerSwitchData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals("AmbientLight[Target] has not been turned on", PowerState.State.ON, colorableLightRemote.getPowerState().getValue());
        assertEquals("PowerPlug[Target] should still be off", PowerState.State.ON, powerSwitchRemote.getPowerState().getValue());
        assertEquals("Dimmer[Source] has not been turned on as a reaction", PowerState.State.ON, dimmerRemote.getPowerState().getValue());

        LOGGER.info("Turn source off");
        dimmerRemote.setPowerState(OFF).get();
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        powerSwitchStateAwaiter.waitForState((PowerSwitchData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals("Dimmer[Source] has not been turned off", PowerState.State.OFF, dimmerRemote.getPowerState().getValue());
        assertEquals("AmbientLight[Target] has not been turned off as a reaction", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("PowerPlug[Target] has not been turned off as a reaction", PowerState.State.OFF, powerSwitchRemote.getPowerState().getValue());

        LOGGER.info("Test by color");
        colorableLightRemote.setColor(HSBColor.newBuilder().setSaturation(100).setHue(20).setBrightness(100).build()).get();

        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        powerSwitchStateAwaiter.waitForState((PowerSwitchData data) -> data.getPowerState().getValue() == PowerState.State.ON);
    }

    @Override
    public UnitConfig getAgentConfig() throws CouldNotPerformException {
        final UnitConfig.Builder agentUnitConfig = MockRegistry.generateAgentConfig(MockRegistry.LABEL_AGENT_CLASS_POWER_STATE_SYNCHRONISER, AGENT_ALIAS, MockRegistry.ALIAS_LOCATION_ROOT_PARADISE);

        // generate meta config
        final MetaConfig.Builder metaConfig = agentUnitConfig.getMetaConfigBuilder();
        Entry.Builder source = metaConfig.addEntryBuilder().setKey(PowerStateSynchroniserAgent.SOURCE_KEY);
        Entry.Builder target1 = metaConfig.addEntryBuilder().setKey(PowerStateSynchroniserAgent.TARGET_KEY + "_1");
        Entry.Builder target2 = metaConfig.addEntryBuilder().setKey(PowerStateSynchroniserAgent.TARGET_KEY + "_2");
        metaConfig.addEntryBuilder().setKey(PowerStateSynchroniserAgent.SOURCE_BEHAVIOUR_KEY).setValue("OFF");
        metaConfig.addEntryBuilder().setKey(PowerStateSynchroniserAgent.TARGET_BEHAVIOUR_KEY).setValue("ON");
        for (UnitConfig unit : Registries.getUnitRegistry().getDalUnitConfigs()) {
            if (unit.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                continue;
            }

            if (unit.getUnitType() == UnitType.DIMMER && source.getValue().isEmpty()) {
                sourceId = unit.getId();
                source.setValue(unit.getId());
            } else if (unit.getUnitType() == UnitType.COLORABLE_LIGHT && target1.getValue().isEmpty()) {
                targetId1 = unit.getId();
                target1.setValue(unit.getId());
            } else if (unit.getUnitType() == UnitType.POWER_SWITCH && target2.getValue().isEmpty()) {
                targetId2 = unit.getId();
                target2.setValue(unit.getId());
            }

            if (source.hasValue() && target1.hasValue() && target2.hasValue()) {
                break;
            }
        }

        return agentUnitConfig.build();
    }
}
