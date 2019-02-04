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

import org.junit.Test;
import org.openbase.app.test.agent.AbstractBCOAgentManagerTest;
import org.openbase.bco.dal.remote.action.Actions;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.DimmerRemote;
import org.openbase.bco.dal.remote.layer.unit.PowerSwitchRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.configuration.EntryType.Entry;
import org.openbase.type.configuration.MetaConfigType.MetaConfig;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import org.openbase.type.domotic.unit.dal.DimmerDataType.DimmerData;
import org.openbase.type.domotic.unit.dal.PowerSwitchDataType.PowerSwitchData;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerStateSynchroniserAgentTest extends AbstractBCOAgentManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PowerStateSynchroniserAgentTest.class);

    private static final String AGENT_ALIAS = "Power_State_Sync_Agent_Unit_Test";

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

        UnitStateAwaiter<DimmerData, DimmerRemote> dimmerStateAwaiter = new UnitStateAwaiter<>(dimmerRemote);
        UnitStateAwaiter<ColorableLightData, ColorableLightRemote> colorableLightStateAwaiter = new UnitStateAwaiter<>(colorableLightRemote);
        UnitStateAwaiter<PowerSwitchData, PowerSwitchRemote> powerSwitchStateAwaiter = new UnitStateAwaiter<>(powerSwitchRemote);

        LOGGER.info("Dimmer id [" + sourceId + "]");
        LOGGER.info("Ambient light id [" + colorableLightRemote.getId() + "]");
        LOGGER.info("Power plug id [" + powerSwitchRemote.getId() + "]");

        // Turn off targets which should make the agent turn of the source
        LOGGER.info("Turn off targets");
        Actions.waitForExecution(colorableLightRemote.setPowerState(State.OFF));
        Actions.waitForExecution(powerSwitchRemote.setPowerState(State.OFF));
        assertEquals("Target 1 has not turned off", State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("Target 2 has not turned off", State.OFF, powerSwitchRemote.getPowerState().getValue());
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        powerSwitchStateAwaiter.waitForState((PowerSwitchData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        // Turn on a target which should make the agent turn on the source
        LOGGER.info("Turn a target on");
        Actions.waitForExecution(powerSwitchRemote.setPowerState(State.ON));
        assertEquals("Target 1 did not stay off", State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("Target 2 has not turned on", State.ON, powerSwitchRemote.getPowerState().getValue());
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        // turn target off which should make the agent turn off the source
        LOGGER.info("Turn all targets off");
        Actions.waitForExecution(powerSwitchRemote.setPowerState(State.OFF));
        assertEquals("Target 1 did not stay off", State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("Target 2 has not turned off", State.OFF, powerSwitchRemote.getPowerState().getValue());
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        // change color of the target which should also make the agent turn on the source
        LOGGER.info("Set color of target");
        final HSBColor hsbColor = HSBColor.newBuilder().setHue(0).setSaturation(100).setBrightness(100).build();
        Actions.waitForExecution(colorableLightRemote.setColor(hsbColor));
        assertEquals("Target 1 has not turned on", State.ON, colorableLightRemote.getPowerState().getValue());
        assertEquals("Target 1 does not have the expected color", hsbColor, colorableLightRemote.getColorState().getColor().getHsbColor());
        assertEquals("Target 2 has not stayed off", State.OFF, powerSwitchRemote.getPowerState().getValue());
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.ON);
    }

    @Override
    public UnitConfig getAgentConfig() throws CouldNotPerformException {
        final UnitConfig.Builder agentUnitConfig = MockRegistry.generateAgentConfig(MockRegistry.LABEL_AGENT_CLASS_POWER_STATE_SYNCHRONISER, AGENT_ALIAS, MockRegistry.ALIAS_LOCATION_ROOT_PARADISE);

        // generate meta config
        final MetaConfig.Builder metaConfig = agentUnitConfig.getMetaConfigBuilder();
        Entry.Builder source = metaConfig.addEntryBuilder().setKey(PowerStateSynchroniserAgent.SOURCE_KEY);
        Entry.Builder target1 = metaConfig.addEntryBuilder().setKey(PowerStateSynchroniserAgent.TARGET_KEY + "_1");
        Entry.Builder target2 = metaConfig.addEntryBuilder().setKey(PowerStateSynchroniserAgent.TARGET_KEY + "_2");
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
