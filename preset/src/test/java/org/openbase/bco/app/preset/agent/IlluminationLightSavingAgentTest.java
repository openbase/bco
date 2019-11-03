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

import org.openbase.app.test.agent.AbstractBCOAgentManagerTest;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.provider.IlluminanceStateProviderService;
import org.openbase.bco.dal.control.layer.unit.LightSensorController;
import org.openbase.bco.dal.remote.layer.service.IlluminanceStateServiceRemote;
import org.openbase.bco.dal.remote.layer.service.PowerStateServiceRemote;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.LightSensorRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.agent.AgentRemote;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.slf4j.LoggerFactory;
import org.openbase.type.configuration.MetaConfigType.MetaConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.agent.AgentDataType;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import org.openbase.type.domotic.unit.dal.LightSensorDataType.LightSensorData;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;

import static org.junit.Assert.assertEquals;

/**
 * * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo
 * Michalski</a>
 */
public class IlluminationLightSavingAgentTest extends AbstractBCOAgentManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(IlluminationLightSavingAgentTest.class);

    public static final String ILLUMINATION_LIGHT_SAVING_AGENT_LABEL = "Illumination_Light_Saving_Agent_Unit_Test";

    public IlluminationLightSavingAgentTest() throws Exception {
    }

    /**
     * Test of activate method, of class PowerStateSynchroniserAgent.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testIlluminationLightSavingAgent() throws Exception {
        System.out.println("testIlluminationLightSavingAgent");

        UnitStateAwaiter<AgentDataType.AgentData, AgentRemote> UnitStateAwaiter = new UnitStateAwaiter<>(agentRemote);
        UnitStateAwaiter.waitForState((AgentDataType.AgentData data) -> data.getActivationState().getValue() == ActivationStateType.ActivationState.State.ACTIVE);

        // It can take some time until the execute() method of the agent has finished
        // TODO: enable to acces controller instances via remoteRegistry to check and wait for the execution of the agent
        Registries.waitForData();

        LocationRemote locationRemote = Units.getUnitByAlias(MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN, true, Units.LOCATION);
        ColorableLightRemote colorableLightRemote = locationRemote.getUnits(UnitType.COLORABLE_LIGHT,true, Units.COLORABLE_LIGHT).get(0);
        LightSensorRemote lightSensorRemote = locationRemote.getUnits(UnitType.LIGHT_SENSOR,true, Units.LIGHT_SENSOR).get(0);
        LightSensorController lightSensorController = (LightSensorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(lightSensorRemote.getId());

        UnitStateAwaiter<LightSensorData, LightSensorRemote> lightSensorStateAwaiter = new UnitStateAwaiter<>(lightSensorRemote);
        UnitStateAwaiter<ColorableLightData, ColorableLightRemote> colorableLightStateAwaiter = new UnitStateAwaiter<>(colorableLightRemote);
        UnitStateAwaiter<LocationData, LocationRemote> locationStateAwaiter = new UnitStateAwaiter<>(locationRemote);

        // create intial values with lights on and illuminance of 5000.0
        lightSensorController.applyDataUpdate(IlluminanceState.newBuilder().setIlluminance(5000.0).build(), ServiceType.ILLUMINANCE_STATE_SERVICE);
        LOGGER.info("WaitFor LightSensor illumincance update");
        lightSensorStateAwaiter.waitForState((LightSensorData data) -> data.getIlluminanceState().getIlluminance() == 5000.0);
        locationRemote.setPowerState(PowerState.State.ON).get();
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
        lightSensorController.applyDataUpdate(IlluminanceState.newBuilder().setIlluminance(7000.0).build(), ServiceType.ILLUMINANCE_STATE_SERVICE);
        LOGGER.info("WaitFor LightSensor illumincance update");
        lightSensorStateAwaiter.waitForState((LightSensorData data) -> data.getIlluminanceState().getIlluminance() == 7000.0);
        LOGGER.info("WaitFor location illumincance update");
        locationStateAwaiter.waitForState((LocationData data) -> data.getIlluminanceState().getIlluminance() == 7000.0);
        LOGGER.info("WaitFor ColorableLight power update");
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
//        LOGGER.info("WaitFor Location power update");
//        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals("Initial Illuminance of LightSensor[" + lightSensorRemote.getLabel() + "] is not 7000", 7000.0, lightSensorRemote.getIlluminanceState().getIlluminance(), 1);
        assertEquals("Initial Illuminance of Location[" + locationRemote.getLabel() + "] is not 7000", 7000.0, locationRemote.getIlluminanceState().getIlluminance(), 1);
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has not switched to OFF", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        //assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has not switched to OFF", PowerState.State.OFF, locationRemote.getPowerState().getValue());

        // Not Part of this agent, is it? As it is just cancelling its action and is not responsible for behavior afterwards.
        // test if on low illuminance lights stay off
        lightSensorController.applyDataUpdate(IlluminanceState.newBuilder().setIlluminance(2000.0).build(), ServiceType.ILLUMINANCE_STATE_SERVICE);
        LOGGER.info("WaitFor LightSensor illumincance update");
        lightSensorStateAwaiter.waitForState((LightSensorData data) -> data.getIlluminanceState().getIlluminance() == 2000.0);
        LOGGER.info("WaitFor location illumincance update");
        locationStateAwaiter.waitForState((LocationData data) -> data.getIlluminanceState().getIlluminance() == 2000.0);
        LOGGER.info("WaitFor ColorableLight power update");
        colorableLightStateAwaiter.waitForState((ColorableLightData data) -> data.getPowerState().getValue() == PowerState.State.OFF);
//        LOGGER.info("WaitFor Location power update");
//        locationStateAwaiter.waitForState((LocationData data) -> data.getPowerState().getValue() == PowerState.State.OFF);

        assertEquals("Initial Illuminance of LightSensor[" + lightSensorRemote.getLabel() + "] is not 2000", 2000.0, lightSensorRemote.getIlluminanceState().getIlluminance(), 1);
        assertEquals("Initial Illuminance of Location[" + locationRemote.getLabel() + "] is not 2000", 2000.0, locationRemote.getIlluminanceState().getIlluminance(), 1);
        assertEquals("PowerState of ColorableLight[" + colorableLightRemote.getLabel() + "] has changes without intention", PowerState.State.OFF, colorableLightRemote.getPowerState().getValue());
        //assertEquals("PowerState of Location[" + locationRemote.getLabel() + "] has changes without intention", PowerState.State.OFF, locationRemote.getPowerState().getValue());
    }

    @Override
    public UnitConfig getAgentConfig() throws CouldNotPerformException {
        final UnitConfig.Builder agentConfig = MockRegistry.generateAgentConfig(MockRegistry.LABEL_AGENT_CLASS_ILLUMINATION_LIGHT_SAVING, ILLUMINATION_LIGHT_SAVING_AGENT_LABEL, MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN);

        // generate meta config
        final MetaConfig.Builder metaConfig = agentConfig.getMetaConfigBuilder();
        metaConfig.addEntryBuilder().setKey(IlluminationLightSavingAgent.MINIMUM_NEEDED_KEY).setValue("3000");
        metaConfig.addEntryBuilder().setKey(IlluminationLightSavingAgent.MAXIMUM_WANTED_KEY).setValue("6000");

        return agentConfig.build();
    }
}
