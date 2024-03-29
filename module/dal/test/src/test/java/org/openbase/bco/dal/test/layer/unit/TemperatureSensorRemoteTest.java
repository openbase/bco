package org.openbase.bco.dal.test.layer.unit;

/*
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.dal.remote.layer.unit.TemperatureSensorRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.AlarmStateType.AlarmState;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TemperatureSensorRemoteTest extends AbstractBCODeviceManagerTest {

    private static TemperatureSensorRemote temperatureSensorRemote;

    public TemperatureSensorRemoteTest() {
    }

    @BeforeAll
    @Timeout(30)
    public static void setupTest() throws Throwable {
        temperatureSensorRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.TEMPERATURE_SENSOR), true, TemperatureSensorRemote.class);
    }

    /**
     * Test of notifyUpdated method, of class TemperatureSensorRemote.
     */
    @Disabled
    public void testNotifyUpdated() {
    }

    /**
     * Test of getTemperature method, of class TemperatureSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetTemperature() throws Exception {
        System.out.println("getTemperature");
        double temperature = 37.0F;
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(temperatureSensorRemote.getId()).applyServiceState(TemperatureState.newBuilder().setTemperature(temperature), ServiceType.TEMPERATURE_STATE_SERVICE);
        temperatureSensorRemote.requestData().get();
        assertEquals(temperature, temperatureSensorRemote.getTemperatureState().getTemperature(), 0.1, "The getter for the temperature returns the wrong value!");
    }

    /**
     * Test of getTemperatureAlarmState method, of class
     * TemperatureSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetTemperatureAlarmState() throws Exception {
        System.out.println("getTemperatureAlarmState");
        AlarmState alarmState = AlarmState.newBuilder().setValue(AlarmState.State.ALARM).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(temperatureSensorRemote.getId()).applyServiceState(alarmState, ServiceType.TEMPERATURE_ALARM_STATE_SERVICE);
        temperatureSensorRemote.requestData().get();
        assertEquals(alarmState.getValue(), temperatureSensorRemote.getTemperatureAlarmState().getValue(), "The getter for the temperature alarm state returns the wrong value!");
    }
}
