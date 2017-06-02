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
import org.openbase.bco.dal.lib.layer.unit.ReedContactController;
import org.openbase.bco.dal.remote.unit.ReedContactRemote;
import org.openbase.bco.dal.remote.unit.TemperatureControllerRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.agent.AgentRemote;
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
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
import rst.domotic.state.ContactStateType;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.TemperatureStateType;
import rst.domotic.state.WindowStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.spatial.PlacementConfigType;

/**
 *
 * * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class HeaterEnergySavingAgentTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HeaterEnergySavingAgentTest.class);

    public static final String HEATER_ENERGY_SAVING_AGENT_LABEL = "Heater_Energy_Saving_Agent_Unit_Test";

    private static final PowerState ON = PowerState.newBuilder().setValue(PowerState.State.ON).build();

    private static final ContactStateType.ContactState CLOSED = ContactStateType.ContactState.newBuilder().setValue(ContactStateType.ContactState.State.CLOSED).build();
    private static final ContactStateType.ContactState OPEN = ContactStateType.ContactState.newBuilder().setValue(ContactStateType.ContactState.State.OPEN).build();
    private static final String LOCATION_LABEL = "Stairway to Heaven";

    private static AgentRemote agent;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static AgentManagerLauncher agentManagerLauncher;
    private static LocationManagerLauncher locationManagerLauncher;

    private static AgentRegistry agentRegistry;
    private static LocationRegistry locationRegistry;

    public HeaterEnergySavingAgentTest() {
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
    @Test//(timeout = 30000)
    public void testHeaterEnergySavingAgent() throws Exception {
        System.out.println("testHeaterEnergySavingAgent");

        CachedAgentRegistryRemote.waitForData();

        UnitConfig config = registerAgent();
        agent = Units.getUnitByLabel(config.getLabel(), true, Units.AGENT);
        agent.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()).get();

        // It can take some time until the execute() method of the agent has finished
        // TODO: enable to acces controller instances via remoteRegistry to check and wait for the execution of the agent
//        Thread.sleep(500);
        Registries.waitForData();

        LocationRemote locationRemote = Units.getUnitByLabel(LOCATION_LABEL, true, Units.LOCATION);
        TemperatureControllerRemote temperatureControllerRemote = Units.getUnit(locationRegistry.getUnitConfigsByLocationLabel(UnitType.TEMPERATURE_CONTROLLER, LOCATION_LABEL).get(0), true, Units.TEMPERATURE_CONTROLLER);
        ConnectionRemote connectionRemote = Units.getUnitByLabel("Stairs_Hell_Lookout", true, Units.CONNECTION);
        ReedContactRemote reedContactRemote = Units.getUnitByLabel("Reed_Stairway_Window", true, Units.REED_CONTACT);
        ReedContactController reedContactController = (ReedContactController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(reedContactRemote.getId());

        locationRemote.waitForData();
        connectionRemote.waitForData();
        reedContactRemote.waitForData();
        temperatureControllerRemote.waitForData();

        reedContactController.updateContactStateProvider(TimestampProcessor.updateTimestampWithCurrentTime(CLOSED));
        temperatureControllerRemote.setTargetTemperatureState(TimestampProcessor.updateTimestampWithCurrentTime(TemperatureStateType.TemperatureState.newBuilder().setTemperature(21.0).build()));
        Thread.sleep(100);
        temperatureControllerRemote.requestData().get();
        locationRemote.requestData().get();
        assertEquals("Initial ContactState of ReedContact[" + reedContactRemote.getLabel() + "] is not CLOSED", ContactStateType.ContactState.State.CLOSED, reedContactRemote.getContactState().getValue());
        assertEquals("Initial ContactState of Connection[" + connectionRemote.getLabel() + "] is not CLOSED", WindowStateType.WindowState.State.CLOSED, connectionRemote.getWindowState().getValue());
        assertEquals("Initial TargetTemperature of TemperatureController[" + temperatureControllerRemote.getLabel() + "] is not 21.0", 21.0, temperatureControllerRemote.getTargetTemperatureState().getTemperature(), 1.0);
        assertEquals("Initial TargetTemperature of location[" + locationRemote.getLabel() + "] is not 21.0", 21.0, locationRemote.getTargetTemperatureState().getTemperature(), 1.0);
    
        reedContactController.updateContactStateProvider(TimestampProcessor.updateTimestampWithCurrentTime(OPEN));
        Thread.sleep(100);
        temperatureControllerRemote.requestData().get();
        locationRemote.requestData().get();
        assertEquals("ContactState of ReedContact[" + reedContactRemote.getLabel() + "] has not switched to OPEN", ContactStateType.ContactState.State.OPEN, reedContactRemote.getContactState().getValue());
        assertEquals("ContactState of Connection[" + connectionRemote.getLabel() + "] has not switched to OPEN", WindowStateType.WindowState.State.OPEN, connectionRemote.getWindowState().getValue());
        assertEquals("TargetTemperature of TemperatureController[" + temperatureControllerRemote.getLabel() + "] has not switched to 13.0", 13.0, temperatureControllerRemote.getTargetTemperatureState().getTemperature(), 1.0);
        assertEquals("TargetTemperature of location[" + locationRemote.getLabel() + "] has not switched to 13.0", 13.0, locationRemote.getTargetTemperatureState().getTemperature(), 1.0);
    
        reedContactController.updateContactStateProvider(TimestampProcessor.updateTimestampWithCurrentTime(CLOSED));
        Thread.sleep(100);
        temperatureControllerRemote.requestData().get();
        locationRemote.requestData().get();
        assertEquals("ContactState of ReedContact[" + reedContactRemote.getLabel() + "] has not switched to CLOSED", ContactStateType.ContactState.State.CLOSED, reedContactRemote.getContactState().getValue());
        assertEquals("ContactState of Connection[" + connectionRemote.getLabel() + "] has not switched to CLOSED", WindowStateType.WindowState.State.CLOSED, connectionRemote.getWindowState().getValue());
        assertEquals("TargetTemperature of TemperatureController[" + temperatureControllerRemote.getLabel() + "] has not switched to 21.0", 21.0, temperatureControllerRemote.getTargetTemperatureState().getTemperature(), 1.0);
        assertEquals("TargetTemperature of location[" + locationRemote.getLabel() + "] has not switched to 21.0", 21.0, locationRemote.getTargetTemperatureState().getTemperature(), 1.0);
    }

    private UnitConfig registerAgent() throws CouldNotPerformException, InterruptedException, ExecutionException {
        System.out.println("Register the AbsenceEnergySavingAgent...");

        EnablingState enablingState = EnablingState.newBuilder().setValue(EnablingState.State.ENABLED).build();
        PlacementConfigType.PlacementConfig.Builder placementConfig = PlacementConfigType.PlacementConfig.newBuilder().setLocationId(locationRegistry.getLocationConfigsByLabel(LOCATION_LABEL).get(0).getId());

        String agentClassId = null;
        for (AgentClass agentClass : agentRegistry.getAgentClasses()) {
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
        return agentRegistry.registerAgentConfig(agentUnitConfig.build()).get();
    }
}
