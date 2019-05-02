package org.openbase.bco.app.preset.agent;

/*-
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
import org.openbase.bco.dal.control.layer.unit.SmokeDetectorController;
import org.openbase.bco.dal.lib.jp.JPUnitAllocation;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.RollerShutterRemote;
import org.openbase.bco.dal.remote.layer.unit.SmokeDetectorRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.state.BlindStateType.BlindState.State;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.AlarmStateType.AlarmState;
import org.openbase.type.domotic.state.BlindStateType.BlindState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import org.openbase.type.domotic.unit.dal.RollerShutterDataType.RollerShutterData;
import org.openbase.type.domotic.unit.dal.SmokeDetectorDataType.SmokeDetectorData;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;

import static org.junit.Assert.assertEquals;

public class FireAlarmAgentTest extends AbstractBCOAgentManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FireAlarmAgentTest.class);

    public static final String FIRE_ALARM_AGENT_LABEL = "Fire_Alarm_Agent_Unit_Test";

    private static final PowerState OFF = PowerState.newBuilder().setValue(PowerState.State.OFF).build();

    private static final AlarmState ALARM = AlarmState.newBuilder().setValue(AlarmState.State.ALARM).build();
    private static final AlarmState NO_ALARM = AlarmState.newBuilder().setValue(AlarmState.State.NO_ALARM).build();
    private static final BlindState CLOSED = BlindState.newBuilder().setOpeningRatio(0).build();

    /**
     * Test of activate method, of class FireAlarmAgent.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testFireAlarmAgent() throws Exception {
        // TODO: turn back on when resource allocation is integrated for unit tests
        try {
            if (!JPService.getProperty(JPUnitAllocation.class).getValue()) {
                return;
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not access JPResourceAllocation property", ex);
        }

        System.out.println("testFireAlarmAgent");

        // It can take some time until the execute() method of the agent has finished
        // TODO: enable to acces controller instances via remoteRegistry to check and wait for the execution of the agent
        Registries.waitForData();

        LocationRemote locationRemote = Units.getUnitByAlias(MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN, true, Units.LOCATION);
        ColorableLightRemote colorableLightRemote = locationRemote.getUnits(UnitType.COLORABLE_LIGHT,true, Units.COLORABLE_LIGHT).get(0);
        RollerShutterRemote rollerShutterRemote = locationRemote.getUnits(UnitType.ROLLER_SHUTTER, true, Units.ROLLER_SHUTTER).get(0);
        SmokeDetectorRemote smokeDetectorRemote = locationRemote.getUnits(UnitType.SMOKE_DETECTOR, true, Units.SMOKE_DETECTOR).get(0);
        SmokeDetectorController smokeDetectorController = (SmokeDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(smokeDetectorRemote.getId());

        UnitStateAwaiter<ColorableLightData, ColorableLightRemote> colorableLightStateAwaiter = new UnitStateAwaiter<>(colorableLightRemote);
        UnitStateAwaiter<RollerShutterData, RollerShutterRemote> rollerShutterStateAwaiter = new UnitStateAwaiter<>(rollerShutterRemote);
        UnitStateAwaiter<SmokeDetectorData, SmokeDetectorRemote> smokeDetectorStateAwaiter = new UnitStateAwaiter<>(smokeDetectorRemote);
        UnitStateAwaiter<LocationData, LocationRemote> locationStateAwaiter = new UnitStateAwaiter<>(locationRemote);

        colorableLightRemote.waitForData();
        rollerShutterRemote.waitForData();
        locationRemote.waitForData();
        smokeDetectorRemote.waitForData();

        // create intial values with NO_ALARM, lights off and blindstate to 0
        smokeDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(NO_ALARM), ServiceType.SMOKE_ALARM_STATE_SERVICE);
        smokeDetectorStateAwaiter.waitForState((SmokeDetectorData data) -> data.getSmokeAlarmState().getValue() == AlarmState.State.NO_ALARM);
        rollerShutterRemote.setBlindState(CLOSED);
        rollerShutterStateAwaiter.waitForState((RollerShutterData data) -> data.getBlindState().getOpeningRatio() == 0d);
        locationRemote.setPowerState(OFF).get();
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals("Initial SmokeAlarmState of SmokeDetector[" + smokeDetectorRemote.getLabel() + "] is not NO_ALARM", AlarmState.State.NO_ALARM, smokeDetectorRemote.getSmokeAlarmState().getValue());
        assertEquals("Initial OpeningRatio of Blindstate of Rollershutter[" + rollerShutterRemote.getLabel() + "] is not 0.0", 0d, rollerShutterRemote.getBlindState().getOpeningRatio(), 0.001);
        assertEquals("Initial PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] is not OFF", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("Initial PowerState of Location[" + locationRemote.getLabel() + "] is not OFF", PowerState.State.OFF, locationRemote.getPowerState().getValue());

        // test if on alarm the lights are turned on
        smokeDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(ALARM), ServiceType.SMOKE_ALARM_STATE_SERVICE);
        smokeDetectorStateAwaiter.waitForState((SmokeDetectorData data) -> data.getSmokeAlarmState().getValue() == AlarmState.State.ALARM);
        locationStateAwaiter.waitForState((LocationData data) -> data.getSmokeAlarmState().getValue() == AlarmState.State.ALARM);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        rollerShutterStateAwaiter.waitForState((RollerShutterData data) -> data.getBlindState().getOpeningRatio() == 1.0d && data.getBlindState().getValue() == State.UP);
        //locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals("SmokeAlarmState of SmokeDetector[" + smokeDetectorRemote.getLabel() + "] has not switched to ALARM", AlarmState.State.ALARM, smokeDetectorRemote.getSmokeAlarmState().getValue());
        assertEquals("OpeningRatio of Blindstate of Rollershutter[" + rollerShutterRemote.getLabel() + "] has not switched to 100", 1.0d, rollerShutterRemote.getBlindState().getOpeningRatio(), 0.001);
        assertEquals("SmokeAlarmState of Location[" + locationRemote.getLabel() + "] has not switched to ALARM.", AlarmState.State.ALARM, locationRemote.getSmokeAlarmState().getValue());
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched to ON", PowerState.State.ON, colorableLightRemote.getPowerState().getValue());
        //assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has not switched to ON", PowerState.State.ON, locationRemote.getPowerState().getValue());

    }

    @Override
    public UnitConfigType.UnitConfig getAgentConfig() throws CouldNotPerformException {
        return MockRegistry.generateAgentConfig(MockRegistry.LABEL_AGENT_CLASS_FIRE_ALARM, FIRE_ALARM_AGENT_LABEL, MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN).build();
    }
}
