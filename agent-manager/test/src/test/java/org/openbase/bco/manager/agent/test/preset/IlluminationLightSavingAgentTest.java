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
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.provider.IlluminanceStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.LightSensorController;
import org.openbase.bco.dal.remote.service.IlluminanceStateServiceRemote;
import org.openbase.bco.dal.remote.service.PowerStateServiceRemote;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.LightSensorRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.agent.AgentRemote;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.unit.util.UnitStateAwaiter;
import org.openbase.bco.manager.agent.core.preset.IlluminationLightSavingAgent;
import org.openbase.bco.registry.agent.remote.CachedAgentRegistryRemote;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.slf4j.LoggerFactory;
import rst.configuration.EntryType;
import rst.configuration.MetaConfigType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.IlluminanceStateType.IlluminanceState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import rst.domotic.unit.dal.LightSensorDataType.LightSensorData;
import rst.domotic.unit.location.LocationDataType.LocationData;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo
 * Michalski</a>
 */
public class IlluminationLightSavingAgentTest extends AbstractBCOAgentManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(IlluminationLightSavingAgentTest.class);

    public static final String ILLUMINATION_LIGHT_SAVING_AGENT_LABEL = "Illumination_Light_Saving_Agent_Unit_Test";

    private static final String LOCATION_LABEL = "Stairway to Heaven";

    private AgentRemote agent;

    public IlluminationLightSavingAgentTest() throws Exception {
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
    @Test(timeout = 30000)
    public void testIlluminationLightSavingAgent() throws Exception {
        // TODO: turn back on when resource allocation is integrated for unit tests
        try {
            if (!JPService.getProperty(JPResourceAllocation.class).getValue()) {
                return;
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not access JPResourceAllocation property", ex);
        }

        System.out.println("testIlluminationLightSavingAgent");
        CachedAgentRegistryRemote.waitForData();

        UnitConfig config = registerAgent();
        agent = Units.getUnit(config, true, Units.AGENT);
        agent.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()).get();

        // It can take some time until the execute() method of the agent has finished
        // TODO: enable to acces controller instances via remoteRegistry to check and wait for the execution of the agent
        Registries.waitForData();

        LocationRemote locationRemote = Units.getUnitsByLabel(LOCATION_LABEL, true, Units.LOCATION).get(0);
        ColorableLightRemote colorableLightRemote = Units.getUnit(Registries.getLocationRegistry().getUnitConfigsByLocationLabel(UnitType.COLORABLE_LIGHT, LOCATION_LABEL).get(0), true, Units.COLORABLE_LIGHT);
        LightSensorRemote lightSensorRemote = Units.getUnit(Registries.getLocationRegistry().getUnitConfigsByLocationLabel(UnitType.LIGHT_SENSOR, LOCATION_LABEL).get(0), true, LightSensorRemote.class);
        LightSensorController lightSensorController = (LightSensorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(lightSensorRemote.getId());

        UnitStateAwaiter<LightSensorData, LightSensorRemote> lightSensorStateAwaiter = new UnitStateAwaiter(lightSensorRemote);
        UnitStateAwaiter<ColorableLightData, ColorableLightRemote> colorableLightStateAwaiter = new UnitStateAwaiter(colorableLightRemote);
        UnitStateAwaiter<LocationData, LocationRemote> locationStateAwaiter = new UnitStateAwaiter(locationRemote);

        // create intial values with lights on and illuminance of 5000.0
        lightSensorController.applyDataUpdate(IlluminanceState.newBuilder().setIlluminance(5000.0).build());
        locationRemote.setPowerState(PowerState.State.ON).get();
        LOGGER.info("WaitFor LightSensor illumincance update");
        lightSensorStateAwaiter.waitForState((LightSensorData data) -> data.getIlluminanceState().getIlluminance() == 5000.0);
        LOGGER.info("WaitFor location illumincance update");
//        locationStateAwaiter.waitForState((LocationData data) -> data.getIlluminanceState().getIlluminance() == 5000.0);
        locationStateAwaiter.waitForState((LocationData data) -> {
            try {
                LOGGER.info("Received location[" + locationRemote.getLabel() + "] illuminance update");
                IlluminanceStateServiceRemote serviceRemote = (IlluminanceStateServiceRemote) locationRemote.getServiceRemote(ServiceType.ILLUMINANCE_STATE_SERVICE);
                LOGGER.info("ServiceRemote [" + serviceRemote.getData().getIlluminance() + "]");
                for (IlluminanceStateProviderService service : serviceRemote.getServices()) {
                    LOGGER.info("Service[" + service.getIlluminanceState().getIlluminance() + "]");
                }
            } catch (NotAvailableException ex) {

            }
            LOGGER.info("Data illuminance: " + data.getIlluminanceState().getIlluminance());
            return data.getIlluminanceState().getIlluminance() == 5000.0;
        });
        LOGGER.info("WaitFor Location power update");
//        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.ON);
        locationStateAwaiter.waitForState((LocationData data) -> {
            try {
                LOGGER.info("Received location[" + locationRemote.getLabel() + "] power update");
                PowerStateServiceRemote serviceRemote = (PowerStateServiceRemote) locationRemote.getServiceRemote(ServiceType.POWER_STATE_SERVICE);
                LOGGER.info("ServiceRemote [" + serviceRemote.getData().getValue().name() + "]");
                for (PowerStateOperationService service : serviceRemote.getServices()) {
                    LOGGER.info("Service[" + service.getPowerState().getValue().name() + "]");
                }
            } catch (NotAvailableException ex) {

            }
            LOGGER.info("Data power: " + data.getPowerState().getValue().name());
            return data.getPowerState().getValue() == PowerState.State.ON;
        });
        LOGGER.info("WaitFor ColorableLight power update");
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.ON);

        assertEquals("Initial Illuminance of LightSensor[" + lightSensorRemote.getLabel() + "] is not 5000", 5000.0, lightSensorRemote.getIlluminanceState().getIlluminance(), 1);
        assertEquals("Initial Illuminance of Location[" + locationRemote.getLabel() + "] is not 5000", 5000.0, locationRemote.getIlluminanceState().getIlluminance(), 1);
        assertEquals("Initial PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] is not ON", PowerState.State.ON, colorableLightRemote.getPowerState().getValue());
        assertEquals("Initial PowerState of Location[" + locationRemote.getLabel() + "] is not ON", PowerState.State.ON, locationRemote.getPowerState().getValue());

        // test if on high illuminance lights get switched off
        lightSensorController.applyDataUpdate(IlluminanceState.newBuilder().setIlluminance(7000.0).build());
        LOGGER.info("WaitFor LightSensor illumincance update");
        lightSensorStateAwaiter.waitForState((LightSensorData data) -> data.getIlluminanceState().getIlluminance() == 7000.0);
        LOGGER.info("WaitFor location illumincance update");
        locationStateAwaiter.waitForState((LocationData data) -> data.getIlluminanceState().getIlluminance() == 7000.0);
        LOGGER.info("WaitFor ColorableLight power update");
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        LOGGER.info("WaitFor Location power update");
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals("Initial Illuminance of LightSensor[" + lightSensorRemote.getLabel() + "] is not 7000", 7000.0, lightSensorRemote.getIlluminanceState().getIlluminance(), 1);
        assertEquals("Initial Illuminance of Location[" + locationRemote.getLabel() + "] is not 7000", 7000.0, locationRemote.getIlluminanceState().getIlluminance(), 1);
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched to OFF", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        //assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has not switched to OFF", PowerState.State.OFF, locationRemote.getPowerState().getValue());

        // test if on low illuminance lights say off
        lightSensorController.applyDataUpdate(IlluminanceState.newBuilder().setIlluminance(2000.0).build());
        LOGGER.info("WaitFor LightSensor illumincance update");
        lightSensorStateAwaiter.waitForState((LightSensorData data) -> data.getIlluminanceState().getIlluminance() == 2000.0);
        LOGGER.info("WaitFor location illumincance update");
        locationStateAwaiter.waitForState((LocationData data) -> data.getIlluminanceState().getIlluminance() == 2000.0);
        LOGGER.info("WaitFor ColorableLight power update");
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
        LOGGER.info("WaitFor Location power update");
        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals("Initial Illuminance of LightSensor[" + lightSensorRemote.getLabel() + "] is not 2000", 2000.0, lightSensorRemote.getIlluminanceState().getIlluminance(), 1);
        assertEquals("Initial Illuminance of Location[" + locationRemote.getLabel() + "] is not 2000", 2000.0, locationRemote.getIlluminanceState().getIlluminance(), 1);
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has changes without intention", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        //assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has changes without intention", PowerState.State.OFF, locationRemote.getPowerState().getValue());
    }

    private UnitConfig registerAgent() throws CouldNotPerformException, InterruptedException, ExecutionException {
        System.out.println("Register the IlluminationLightSavingAgent...");

        EnablingState enablingState = EnablingState.newBuilder().setValue(EnablingState.State.ENABLED).build();
        PlacementConfig.Builder placementConfig = PlacementConfig.newBuilder().setLocationId(Registries.getLocationRegistry().getLocationConfigsByLabel(LOCATION_LABEL).get(0).getId());

        String agentClassId = null;
        for (AgentClass agentClass : Registries.getAgentRegistry().getAgentClasses()) {
            if (MockRegistry.ILLUMINATION_LIGHT_SAVING_AGENT_LABEL.equals(agentClass.getLabel())) {
                agentClassId = agentClass.getId();
            }
        }
        if (agentClassId == null) {
            throw new CouldNotPerformException("Could not find id for AgentClass with label [" + MockRegistry.ILLUMINATION_LIGHT_SAVING_AGENT_LABEL + "]");
        }
        System.out.println("Foung agentClassId: [" + agentClassId + "]");

        EntryType.Entry.Builder minimumNeeded = EntryType.Entry.newBuilder().setKey(IlluminationLightSavingAgent.MINIMUM_NEEDED_KEY).setValue("3000");
        EntryType.Entry.Builder maximumWanted = EntryType.Entry.newBuilder().setKey(IlluminationLightSavingAgent.MAXIMUM_WANTED_KEY).setValue("6000");

        MetaConfigType.MetaConfig metaConfig = MetaConfigType.MetaConfig.newBuilder()
                .addEntry(minimumNeeded)
                .addEntry(maximumWanted)
                .build();

        UnitConfig.Builder agentUnitConfig = UnitConfig.newBuilder().setLabel(ILLUMINATION_LIGHT_SAVING_AGENT_LABEL).setType(UnitType.AGENT).setPlacementConfig(placementConfig).setMetaConfig(metaConfig).setEnablingState(enablingState);
        agentUnitConfig.getAgentConfigBuilder().setAgentClassId(agentClassId);
        return Registries.getAgentRegistry().registerAgentConfig(agentUnitConfig.build()).get();
    }
}
