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
import org.openbase.bco.dal.control.layer.unit.ReedContactController;
import org.openbase.bco.dal.remote.layer.unit.ReedContactRemote;
import org.openbase.bco.dal.remote.layer.unit.TemperatureControllerRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.connection.ConnectionRemote;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ContactStateType.ContactState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;
import org.openbase.type.domotic.state.WindowStateType.WindowState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.connection.ConnectionDataType.ConnectionData;
import org.openbase.type.domotic.unit.dal.ReedContactDataType.ReedContactData;
import org.openbase.type.domotic.unit.dal.TemperatureControllerDataType.TemperatureControllerData;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;

import static org.junit.Assert.assertEquals;

/**
 * * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo
 * Michalski</a>
 */
public class HeaterEnergySavingAgentTest extends AbstractBCOAgentManagerTest {

    public static final String HEATER_ENERGY_SAVING_AGENT_LABEL = "Heater_Energy_Saving_Agent_Unit_Test";
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HeaterEnergySavingAgentTest.class);
    private static final PowerState ON = PowerState.newBuilder().setValue(PowerState.State.ON).build();

    private static final ContactState CLOSED = ContactState.newBuilder().setValue(ContactState.State.CLOSED).build();
    private static final ContactState OPEN = ContactState.newBuilder().setValue(ContactState.State.OPEN).build();

    /**
     * Test of activate method, of class PowerStateSynchroniserAgent.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testHeaterEnergySavingAgent() throws Exception {
        System.out.println("testHeaterEnergySavingAgent");

        // It can take some time until the execute() method of the agent has finished
        // TODO: enable to acces controller instances via remoteRegistry to check and wait for the execution of the agent
        Registries.waitForData();

        LocationRemote locationRemote = Units.getUnitByAlias(MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN, true, Units.LOCATION);
        TemperatureControllerRemote temperatureControllerRemote = locationRemote.getUnits(UnitType.TEMPERATURE_CONTROLLER, true, Units.TEMPERATURE_CONTROLLER).get(0);
        ConnectionRemote connectionRemote = Units.getUnitByAlias(MockRegistry.ALIAS_WINDOW_STAIRWAY_HELL_LOOKOUT, true, Units.CONNECTION);
        ReedContactRemote reedContactRemote = Units.getUnitByAlias(MockRegistry.ALIAS_REED_SWITCH_STAIRWAY_HELL_WINDOW, true, Units.REED_CONTACT);
        ReedContactController reedContactController = (ReedContactController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(reedContactRemote.getId());

        // validate remote controller match
        assertEquals("Controller and remote are not compatible to each other!", reedContactController.getId(), reedContactRemote.getId());
        assertEquals("Reed contact is not connected to window.", connectionRemote.getConfig().getConnectionConfig().getUnitIdList().contains(reedContactRemote.getId()), true);

        UnitStateAwaiter<ReedContactData, ReedContactRemote> reedContactStateAwaiter = new UnitStateAwaiter<>(reedContactRemote);
        UnitStateAwaiter<ConnectionData, ConnectionRemote> connectionStateAwaiter = new UnitStateAwaiter<>(connectionRemote);
        UnitStateAwaiter<TemperatureControllerData, TemperatureControllerRemote> temperatureControllerStateAwaiter = new UnitStateAwaiter<>(temperatureControllerRemote);
        UnitStateAwaiter<LocationData, LocationRemote> locationStateAwaiter = new UnitStateAwaiter<>(locationRemote);

        // create intial values with reed closed and target temperature at 21.0
        reedContactController.applyServiceState(CLOSED, ServiceType.CONTACT_STATE_SERVICE);
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
        reedContactController.applyServiceState(OPEN, ServiceType.CONTACT_STATE_SERVICE);
        reedContactStateAwaiter.waitForState((ReedContactData data) -> data.getContactState().getValue() == ContactState.State.OPEN);
        connectionStateAwaiter.waitForState((ConnectionData data) -> data.getWindowState().getValue() == WindowState.State.OPEN);
        temperatureControllerStateAwaiter.waitForState((TemperatureControllerData data) -> data.getTargetTemperatureState().getTemperature() == 13.0);
        locationStateAwaiter.waitForState((LocationData data) -> data.getTargetTemperatureState().getTemperature() == 13.0);

        assertEquals("ContactState of ReedContact[" + reedContactRemote.getLabel() + "] has not switched to OPEN", ContactState.State.OPEN, reedContactRemote.getContactState().getValue());
        assertEquals("ContactState of Connection[" + connectionRemote.getLabel() + "] has not switched to OPEN", WindowState.State.OPEN, connectionRemote.getWindowState().getValue());
        assertEquals("TargetTemperature of TemperatureController[" + temperatureControllerRemote.getLabel() + "] has not switched to 13.0", 13.0, temperatureControllerRemote.getTargetTemperatureState().getTemperature(), 1.0);
        assertEquals("TargetTemperature of location[" + locationRemote.getLabel() + "] has not switched to 13.0", 13.0, locationRemote.getTargetTemperatureState().getTemperature(), 1.0);

//        Obsoled as functionality of HeaterEnergySavingAgent was reduced
//        // test if on closed reedsensor target temperature is set back to 21.0
//        reedContactController.applyServiceState(TimestampProcessor.updateTimestampWithCurrentTime(CLOSED), ServiceType.CONTACT_STATE_SERVICE);
//        reedContactStateAwaiter.waitForState((ReedContactData data) -> data.getContactState().getValue() == ContactState.State.CLOSED);
//        connectionStateAwaiter.waitForState((ConnectionData data) -> data.getWindowState().getValue() == WindowState.State.CLOSED);
//        temperatureControllerStateAwaiter.waitForState((TemperatureControllerData data) -> data.getTargetTemperatureState().getTemperature() == 21.0);
//        locationStateAwaiter.waitForState((LocationData data) -> data.getTargetTemperatureState().getTemperature() == 21.0);
//
//        assertEquals("ContactState of ReedContact[" + reedContactRemote.getLabel() + "] has not switched to CLOSED", ContactState.State.CLOSED, reedContactRemote.getContactState().getValue());
//        assertEquals("ContactState of Connection[" + connectionRemote.getLabel() + "] has not switched to CLOSED", WindowState.State.CLOSED, connectionRemote.getWindowState().getValue());
//        assertEquals("TargetTemperature of TemperatureController[" + temperatureControllerRemote.getLabel() + "] has not switched to 21.0", 21.0, temperatureControllerRemote.getTargetTemperatureState().getTemperature(), 1.0);
//        assertEquals("TargetTemperature of location[" + locationRemote.getLabel() + "] has not switched to 21.0", 21.0, locationRemote.getTargetTemperatureState().getTemperature(), 1.0);
    }

    @Override
    public UnitConfig getAgentConfig() throws CouldNotPerformException {
        return MockRegistry.generateAgentConfig(MockRegistry.LABEL_AGENT_CLASS_HEATER_ENERGY_SAVING, HEATER_ENERGY_SAVING_AGENT_LABEL, MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN).build();
    }
}
