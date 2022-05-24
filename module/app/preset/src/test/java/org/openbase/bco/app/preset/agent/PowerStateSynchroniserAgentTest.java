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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.app.test.agent.AbstractBCOAgentManagerTest;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.DimmerRemote;
import org.openbase.bco.dal.remote.layer.unit.PowerSwitchRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.type.configuration.EntryType.Entry;
import org.openbase.type.configuration.MetaConfigType.MetaConfig;
import org.openbase.type.domotic.action.ActionDescriptionType;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
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

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerStateSynchroniserAgentTest extends AbstractBCOAgentManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PowerStateSynchroniserAgentTest.class);

    private static final String AGENT_ALIAS = "Power_State_Sync_Agent_Unit_Test";
    private static final long STATE_AWAIT_TIMEOUT = 1000;

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
    @Test
    @Timeout(15)
    public void testPowerStateSyncAgent() throws Exception {
        System.out.println("testPowerStateSyncAgent");

        final DimmerRemote dimmerRemote = Units.getUnit(sourceId, true, Units.DIMMER);
        final ColorableLightRemote colorableLightRemote = Units.getUnit(targetId1, true, Units.COLORABLE_LIGHT);
        final PowerSwitchRemote powerSwitchRemote = Units.getUnit(targetId2, true, Units.POWER_SWITCH);

        final UnitStateAwaiter<DimmerData, DimmerRemote> dimmerStateAwaiter = new UnitStateAwaiter<>(dimmerRemote);
        final UnitStateAwaiter<ColorableLightData, ColorableLightRemote> colorableLightStateAwaiter = new UnitStateAwaiter<>(colorableLightRemote);
        final UnitStateAwaiter<PowerSwitchData, PowerSwitchRemote> powerSwitchStateAwaiter = new UnitStateAwaiter<>(powerSwitchRemote);

        LOGGER.info("Dimmer id         [{}] state [{}]", dimmerRemote.getId(), dimmerRemote.getPowerState().getValue().name());
        LOGGER.info("ColorableLight id [{}] state [{}]", colorableLightRemote.getId(), colorableLightRemote.getPowerState().getValue().name());
        LOGGER.info("PowerSwitch id    [{}] state [{}]", powerSwitchRemote.getId(), powerSwitchRemote.getPowerState().getValue().name());

        // Turn off targets which should make the agent turn of the source
        LOGGER.info("Turn off targets");
        waitForExecution(colorableLightRemote.setPowerState(State.OFF));
        waitForExecution(powerSwitchRemote.setPowerState(State.OFF));
        assertEquals(State.OFF, colorableLightRemote.getPowerState().getValue(), "Target 1 has not turned off");
        assertEquals(State.OFF, powerSwitchRemote.getPowerState().getValue(), "Target 2 has not turned off");
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF, STATE_AWAIT_TIMEOUT);
        powerSwitchStateAwaiter.waitForState((PowerSwitchData data) -> data.getPowerState().getValue() == PowerState.State.OFF, STATE_AWAIT_TIMEOUT);
        // TODO: also validate that the executing action has expected settings:
        //    * executed by power state agent user for the bco user
        //    * validity time?
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.OFF, STATE_AWAIT_TIMEOUT);

        Thread.sleep(1000);
        System.out.println("\n\n\n");
        // make sure delivered requested state updates are never unknown.
        final Boolean[] deliveredUnknownRequestedState = new Boolean[1];
        deliveredUnknownRequestedState[0] = false;
        powerSwitchRemote.addDataObserver(ServiceTempus.REQUESTED,(source, data) -> {
            LOGGER.warn("Requested data update {}", data.hasPowerStateRequested() ? MultiLanguageTextProcessor.getBestMatch(data.getPowerStateRequested().getResponsibleAction().getDescription()) : data.getPowerStateRequested());
            /*LOGGER.warn("Requested data update?");
            if(!data.hasPowerStateRequested()) {
                LOGGER.error("Received requested state update without requested STATE!");
            }
            try {
                LOGGER.warn("Received requested power state = {}", MultiLanguageTextProcessor.getBestMatch(data.getPowerStateRequested().getResponsibleAction().getDescription()));
            }catch (NotAvailableException ex) {
                LOGGER.warn("Action has not description? {}", data.getPowerStateRequested());
            }
            /*final boolean unknown = data.getPowerStateRequested().getValue().name().equalsIgnoreCase("UNKNOWN");
            if (unknown) {
                if(data.toString().isEmpty()) {
                    ExceptionPrinter.printHistory(new InvalidStateException("Invalid data package received!"), LOGGER);
                }
                LOGGER.error("Incoming requested state via unit was unknown: [" + data+"]");
                deliveredUnknownRequestedState[0] = true;
            }*/
        });
        powerSwitchRemote.addServiceStateObserver(ServiceTempus.REQUESTED, ServiceType.POWER_STATE_SERVICE,(source, data) -> {
            final PowerState val = (PowerState) data;
            LOGGER.warn("Requested service update {}", (val.getValue() == State.UNKNOWN) ? val : MultiLanguageTextProcessor.getBestMatch(val.getResponsibleAction().getDescription()));
            /*LOGGER.warn("Requested service update");
            LOGGER.warn("Incoming requested state via service ob: " + ((PowerState) data).getValue().name());
            final boolean unknown = ((PowerState) data).getValue().name().equalsIgnoreCase("UNKNOWN");
            if (unknown) {
                if(data.toString().isEmpty()) {
                    ExceptionPrinter.printHistory(new InvalidStateException("Invalid data package received!"), LOGGER);
                }
                LOGGER.error("Incoming requested state via unit is unknown! ["+ data+"]");
                deliveredUnknownRequestedState[0] = true;
            }*/
        });
        powerSwitchRemote.addDataObserver(new Observer<DataProvider<PowerSwitchData>, PowerSwitchData>() {
            @Override
            public void update(DataProvider<PowerSwitchData> source, PowerSwitchData data) throws Exception {
                System.out.println("Actions: ");
                for (ActionDescriptionType.ActionDescription actionDescription : data.getActionList()) {
                    System.out.println(actionDescription.getActionId() + ": " + actionDescription.getActionState().getValue().name());
                }
            }
        });

        // Turn on a target which should make the agent turn on the source
        LOGGER.info("Turn a target on");
        waitForExecution(powerSwitchRemote.setPowerState(State.ON));
        // TODO: in a real test scenario the other target would turn on as well -> should the agent request its state as well?
        //   this could get pretty messy: imagine a scene turning on all targets at once...
        assertEquals(State.OFF, colorableLightRemote.getPowerState().getValue(), "Target 1 did not stay off");
        assertEquals(State.ON, powerSwitchRemote.getPowerState().getValue(), "Target 2 has not turned on");
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        // TODO: validate that according flags are set by the agent
        //dimmerRemote.getData().getPowerState().getResponsibleAction().getActionInitiator().

        Thread.sleep(1000);
        System.out.println("\n\n\n");

        // validate received requested states
        assertEquals(false, deliveredUnknownRequestedState[0], "Received at least one unknown requested state!");

        // turn target off which should make the agent turn off the source
        LOGGER.info("Turn all targets off");
        waitForExecution(powerSwitchRemote.setPowerState(State.OFF));
        assertEquals(State.OFF, colorableLightRemote.getPowerState().getValue(), "Target 1 did not stay off");
        assertEquals(State.OFF, powerSwitchRemote.getPowerState().getValue(), "Target 2 has not turned off");
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        Thread.sleep(1000);
        System.out.println("\n\n\n");

        // validate received requested states
        assertEquals(false, deliveredUnknownRequestedState[0], "Received at least one unknown requested state!");

        // change color of the target which should also make the agent turn on the source
        LOGGER.info("Set color of target");
        final HSBColor hsbColor = HSBColor.newBuilder().setHue(0).setSaturation(1d).setBrightness(1d).build();
        waitForExecution(colorableLightRemote.setColor(hsbColor));
        assertEquals(State.ON, colorableLightRemote.getPowerState().getValue(), "Target 1 has not turned on");
        assertEquals(hsbColor, colorableLightRemote.getColorState().getColor().getHsbColor(), "Target 1 does not have the expected color");
        assertEquals(State.OFF, powerSwitchRemote.getPowerState().getValue(), "Target 2 has not stayed off");
        dimmerStateAwaiter.waitForState((DimmerData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        Thread.sleep(1000);
        System.out.println("\n\n\n");

        // validate received requested states
        assertEquals(false, deliveredUnknownRequestedState[0], "Received at least one unknown requested state!");
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
