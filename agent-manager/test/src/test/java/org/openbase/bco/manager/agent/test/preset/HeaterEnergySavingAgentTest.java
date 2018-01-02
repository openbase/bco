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
import org.openbase.bco.dal.lib.layer.unit.ReedContactController;
import org.openbase.bco.dal.remote.unit.ReedContactRemote;
import org.openbase.bco.dal.remote.unit.TemperatureControllerRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.agent.AgentRemote;
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
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
import rst.domotic.state.ContactStateType.ContactState;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.state.WindowStateType.WindowState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.domotic.unit.connection.ConnectionDataType.ConnectionData;
import rst.domotic.unit.dal.ReedContactDataType.ReedContactData;
import rst.domotic.unit.dal.TemperatureControllerDataType.TemperatureControllerData;
import rst.domotic.unit.location.LocationDataType.LocationData;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo
 * Michalski</a>
 */
public class HeaterEnergySavingAgentTest extends AbstractBCOAgentManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HeaterEnergySavingAgentTest.class);

    public static final String HEATER_ENERGY_SAVING_AGENT_LABEL = "Heater_Energy_Saving_Agent_Unit_Test";

    private static final PowerState ON = PowerState.newBuilder().setValue(PowerState.State.ON).build();

    private static final ContactState CLOSED = ContactState.newBuilder().setValue(ContactState.State.CLOSED).build();
    private static final ContactState OPEN = ContactState.newBuilder().setValue(ContactState.State.OPEN).build();
    private static final String LOCATION_LABEL = "Stairway to Heaven";

    private static AgentRemote agent;

    public HeaterEnergySavingAgentTest() throws Exception {
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
    public void testHeaterEnergySavingAgent() throws Exception {
        // TODO: turn back on when resource allocation is integrated for unit tests
        try {
            if (!JPService.getProperty(JPResourceAllocation.class).getValue()) {
                return;
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not access JPResourceAllocation property", ex);
        }
        System.out.println("testHeaterEnergySavingAgent");

        CachedAgentRegistryRemote.waitForData();

        UnitConfig config = registerAgent();
        agent = Units.getUnit(config, true, Units.AGENT);
        agent.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()).get();

        // It can take some time until the execute() method of the agent has finished
        // TODO: enable to acces controller instances via remoteRegistry to check and wait for the execution of the agent
        Registries.waitForData();

        LocationRemote locationRemote = Units.getUnitsByLabel(LOCATION_LABEL, true, Units.LOCATION).get(0);
        TemperatureControllerRemote temperatureControllerRemote = Units.getUnit(Registries.getLocationRegistry().getUnitConfigsByLocationLabel(UnitType.TEMPERATURE_CONTROLLER, LOCATION_LABEL).get(0), true, Units.TEMPERATURE_CONTROLLER);
        ConnectionRemote connectionRemote = Units.getUnitsByLabel("Stairs_Hell_Lookout", true, Units.CONNECTION).get(0);
        ReedContactRemote reedContactRemote = Units.getUnitsByLabel("Reed_Stairway_Window", true, Units.REED_CONTACT).get(0);
        ReedContactController reedContactController = (ReedContactController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(reedContactRemote.getId());

        UnitStateAwaiter<ReedContactData, ReedContactRemote> reedContactStateAwaiter = new UnitStateAwaiter(reedContactRemote);
        UnitStateAwaiter<ConnectionData, ConnectionRemote> connectionStateAwaiter = new UnitStateAwaiter(connectionRemote);
        UnitStateAwaiter<TemperatureControllerData, TemperatureControllerRemote> temperatureControllerStateAwaiter = new UnitStateAwaiter(temperatureControllerRemote);
        UnitStateAwaiter<LocationData, LocationRemote> locationStateAwaiter = new UnitStateAwaiter(locationRemote);

        // create intial values with reed closed and target temperature at 21.0
        reedContactController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(CLOSED));
        temperatureControllerRemote.setTargetTemperatureState(TimestampProcessor.updateTimestampWithCurrentTime(TemperatureState.newBuilder().setTemperature(21.0).build()));
        reedContactStateAwaiter.waitForState((ReedContactData data) -> data.getContactState().getValue() == ContactState.State.CLOSED);
        connectionStateAwaiter.waitForState((ConnectionData data) -> data.getWindowState().getValue() == WindowState.State.CLOSED);
        temperatureControllerStateAwaiter.waitForState((TemperatureControllerData data) -> data.getTargetTemperatureState().getTemperature() == 21.0);
        locationStateAwaiter.waitForState((LocationData data) -> data.getTargetTemperatureState().getTemperature() == 21.0);

        assertEquals("Initial ContactState of ReedContact[" + reedContactRemote.getLabel() + "] is not CLOSED", ContactState.State.CLOSED, reedContactRemote.getContactState().getValue());
        assertEquals("Initial ContactState of Connection[" + connectionRemote.getLabel() + "] is not CLOSED", WindowState.State.CLOSED, connectionRemote.getWindowState().getValue());
        assertEquals("Initial TargetTemperature of TemperatureController[" + temperatureControllerRemote.getLabel() + "] is not 21.0", 21.0, temperatureControllerRemote.getTargetTemperatureState().getTemperature(), 1.0);
        assertEquals("Initial TargetTemperature of location[" + locationRemote.getLabel() + "] is not 21.0", 21.0, locationRemote.getTargetTemperatureState().getTemperature(), 1.0);

        // test if on open reedsensor target temperature is set to 13.0
        reedContactController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(OPEN));
        reedContactStateAwaiter.waitForState((ReedContactData data) -> data.getContactState().getValue() == ContactState.State.OPEN);
        connectionStateAwaiter.waitForState((ConnectionData data) -> data.getWindowState().getValue() == WindowState.State.OPEN);
        temperatureControllerStateAwaiter.waitForState((TemperatureControllerData data) -> data.getTargetTemperatureState().getTemperature() == 13.0);
        locationStateAwaiter.waitForState((LocationData data) -> data.getTargetTemperatureState().getTemperature() == 13.0);

        assertEquals("ContactState of ReedContact[" + reedContactRemote.getLabel() + "] has not switched to OPEN", ContactState.State.OPEN, reedContactRemote.getContactState().getValue());
        assertEquals("ContactState of Connection[" + connectionRemote.getLabel() + "] has not switched to OPEN", WindowState.State.OPEN, connectionRemote.getWindowState().getValue());
        assertEquals("TargetTemperature of TemperatureController[" + temperatureControllerRemote.getLabel() + "] has not switched to 13.0", 13.0, temperatureControllerRemote.getTargetTemperatureState().getTemperature(), 1.0);
        assertEquals("TargetTemperature of location[" + locationRemote.getLabel() + "] has not switched to 13.0", 13.0, locationRemote.getTargetTemperatureState().getTemperature(), 1.0);

        // test if on closed reedsensor target temperature is set back to 21.0
        reedContactController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(CLOSED));
        reedContactStateAwaiter.waitForState((ReedContactData data) -> data.getContactState().getValue() == ContactState.State.CLOSED);
        connectionStateAwaiter.waitForState((ConnectionData data) -> data.getWindowState().getValue() == WindowState.State.CLOSED);
        temperatureControllerStateAwaiter.waitForState((TemperatureControllerData data) -> data.getTargetTemperatureState().getTemperature() == 21.0);
        locationStateAwaiter.waitForState((LocationData data) -> data.getTargetTemperatureState().getTemperature() == 21.0);

        assertEquals("ContactState of ReedContact[" + reedContactRemote.getLabel() + "] has not switched to CLOSED", ContactState.State.CLOSED, reedContactRemote.getContactState().getValue());
        assertEquals("ContactState of Connection[" + connectionRemote.getLabel() + "] has not switched to CLOSED", WindowState.State.CLOSED, connectionRemote.getWindowState().getValue());
        assertEquals("TargetTemperature of TemperatureController[" + temperatureControllerRemote.getLabel() + "] has not switched to 21.0", 21.0, temperatureControllerRemote.getTargetTemperatureState().getTemperature(), 1.0);
        assertEquals("TargetTemperature of location[" + locationRemote.getLabel() + "] has not switched to 21.0", 21.0, locationRemote.getTargetTemperatureState().getTemperature(), 1.0);
    }

    private UnitConfig registerAgent() throws CouldNotPerformException, InterruptedException, ExecutionException {
        System.out.println("Register the AbsenceEnergySavingAgent...");

        EnablingState enablingState = EnablingState.newBuilder().setValue(EnablingState.State.ENABLED).build();
        PlacementConfig.Builder placementConfig = PlacementConfig.newBuilder().setLocationId(Registries.getLocationRegistry().getLocationConfigsByLabel(LOCATION_LABEL).get(0).getId());

        String agentClassId = null;
        for (AgentClass agentClass : Registries.getAgentRegistry().getAgentClasses()) {
            if (MockRegistry.HEATER_ENERGY_SAVING_AGENT_LABEL.equals(agentClass.getLabel())) {
                agentClassId = agentClass.getId();
            }
        }
        if (agentClassId == null) {
            throw new CouldNotPerformException("Could not find id for AgentClass with label [" + MockRegistry.HEATER_ENERGY_SAVING_AGENT_LABEL + "]");
        }
        System.out.println("Foung agentClassId: [" + agentClassId + "]");

        UnitConfig.Builder agentUnitConfig = UnitConfig.newBuilder().setLabel(HEATER_ENERGY_SAVING_AGENT_LABEL).setType(UnitType.AGENT).setPlacementConfig(placementConfig).setEnablingState(enablingState);
        agentUnitConfig.getAgentConfigBuilder().setAgentClassId(agentClassId);
        return Registries.getAgentRegistry().registerAgentConfig(agentUnitConfig.build()).get();
    }
}
