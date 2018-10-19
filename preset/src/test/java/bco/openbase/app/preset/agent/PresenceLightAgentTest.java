package bco.openbase.app.preset.agent;

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

import bco.openbase.app.test.agent.AbstractBCOAgentManagerTest;
import org.junit.Test;
import org.openbase.bco.dal.lib.jp.JPUnitAllocation;
import org.openbase.bco.dal.control.layer.unit.MotionDetectorController;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.MotionDetectorRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.MotionStateType.MotionState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import rst.domotic.unit.dal.MotionDetectorDataType.MotionDetectorData;
import rst.domotic.unit.location.LocationDataType.LocationData;

import static org.junit.Assert.assertEquals;

/**
 * * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class PresenceLightAgentTest extends AbstractBCOAgentManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PresenceLightAgentTest.class);

    public static final String PRESENCE_LIGHT_AGENT_LABEL = "Presence_Light_Agent_Unit_Test";

    private static final PowerState OFF = PowerState.newBuilder().setValue(PowerState.State.OFF).build();

    private static final MotionState MOTION = MotionState.newBuilder().setValue(MotionState.State.MOTION).build();
    private static final MotionState NO_MOTION = MotionState.newBuilder().setValue(MotionState.State.NO_MOTION).build();

    public PresenceLightAgentTest() {
    }

    /**
     * Test of activate method, of class PreseceLightAgent.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testPreseceLightAgent() throws Exception {
        // TODO: turn back on when resource allocation is integrated for unit tests
        try {
            if (!JPService.getProperty(JPUnitAllocation.class).getValue()) {
                return;
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not access JPResourceAllocation property", ex);
        }

        System.out.println("testPreseceLightAgent");

        // It can take some time until the execute() method of the agent has finished
        // TODO: enable to acces controller instances via remoteRegistry to check and wait for the execution of the agent
        Registries.waitForData();

        LocationRemote locationRemote = Units.getUnitByAlias(MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN, true, Units.LOCATION);
        ColorableLightRemote colorableLightRemote = Units.getUnit(Registries.getUnitRegistry().getUnitConfigsByLocation(UnitType.COLORABLE_LIGHT, MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN).get(0), true, Units.COLORABLE_LIGHT);
        MotionDetectorRemote motionDetectorRemote = Units.getUnit(Registries.getUnitRegistry().getUnitConfigsByLocation(UnitType.MOTION_DETECTOR, MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN).get(0), true, Units.MOTION_DETECTOR);
        MotionDetectorController motionDetectorController = (MotionDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(motionDetectorRemote.getId());

        UnitStateAwaiter<ColorableLightData, ColorableLightRemote> colorableLightStateAwaiter = new UnitStateAwaiter<>(colorableLightRemote);
        UnitStateAwaiter<MotionDetectorData, MotionDetectorRemote> motionDetectorStateAwaiter = new UnitStateAwaiter<>(motionDetectorRemote);
        UnitStateAwaiter<LocationData, LocationRemote> locationStateAwaiter = new UnitStateAwaiter<>(locationRemote);

        colorableLightRemote.waitForData();
        locationRemote.waitForData();
        motionDetectorRemote.waitForData();

        // create intial values with no_motion and lights off
        motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(NO_MOTION), ServiceType.MOTION_STATE_SERVICE);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.NO_MOTION);
        locationRemote.setPowerState(OFF).get();
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals("Initial MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] is not NO_MOTION", MotionState.State.NO_MOTION, motionDetectorRemote.getMotionState().getValue());
        assertEquals("Initial PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] is not OFF", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        assertEquals("Initial PowerState of Location[" + locationRemote.getLabel() + "] is not OFF", PowerState.State.OFF, locationRemote.getPowerState().getValue());

        // test if on motion the lights are turned on
        motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(MOTION), ServiceType.MOTION_STATE_SERVICE);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.PRESENT);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        //locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals("MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to MOTION", MotionState.State.MOTION, motionDetectorRemote.getMotionState().getValue());
        assertEquals("PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to PRESENT.", PresenceState.State.PRESENT, locationRemote.getPresenceState().getValue());
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched to ON", PowerState.State.ON, colorableLightRemote.getPowerState().getValue());
        //assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has not switched to ON", PowerState.State.ON, locationRemote.getPowerState().getValue());

        // test if the lights stay on on no motion
        motionDetectorController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(NO_MOTION), ServiceType.MOTION_STATE_SERVICE);
        motionDetectorStateAwaiter.waitForState((MotionDetectorData data) -> data.getMotionState().getValue() == MotionState.State.NO_MOTION);
        locationStateAwaiter.waitForState((LocationData data) -> data.getPresenceState().getValue() == PresenceState.State.ABSENT);
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        //locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals("MotionState of MotionDetector[" + motionDetectorRemote.getLabel() + "] has not switched to NO_MOTION", MotionState.State.NO_MOTION, motionDetectorRemote.getMotionState().getValue());
        assertEquals("PresenceState of Location[" + locationRemote.getLabel() + "] has not switched to ABSENT.", PresenceState.State.ABSENT, locationRemote.getPresenceState().getValue());
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has changes without intention", PowerState.State.ON, colorableLightRemote.getPowerState().getValue());
        //assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has changes without intention", PowerState.State.ON, locationRemote.getPowerState().getValue());
    }

    @Override
    public UnitConfig getAgentConfig() throws CouldNotPerformException {
        return MockRegistry.generateAgentConfig(MockRegistry.LABEL_AGENT_CLASS_PRESENCE_LIGHT, PRESENCE_LIGHT_AGENT_LABEL, MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN).build();
    }
}
