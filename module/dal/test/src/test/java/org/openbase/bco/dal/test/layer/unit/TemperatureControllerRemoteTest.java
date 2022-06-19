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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.dal.remote.layer.unit.TemperatureControllerRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TemperatureControllerRemoteTest extends AbstractBCODeviceManagerTest {

    private static TemperatureControllerRemote temperatureControllerRemote;

    public TemperatureControllerRemoteTest() {
    }

    @BeforeAll
    public static void setupTest() throws Throwable {
        temperatureControllerRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.TEMPERATURE_CONTROLLER), true, TemperatureControllerRemote.class);
    }

    /**
     * Test of getTemperature method, of class TemperatureSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testSetTargetTemperature() throws Exception {
        System.out.println("setTargetTemperature");
        double temperature = 42.0F;
        TemperatureState temperatureState = TemperatureState.newBuilder().setTemperature(temperature).build();
        waitForExecution(temperatureControllerRemote.setTargetTemperatureState(temperatureState));
        temperatureControllerRemote.requestData().get();
        assertEquals(temperature, temperatureControllerRemote.getTargetTemperatureState().getTemperature(), 0.1, "The getter for the target temperature returns the wrong value!");
    }

    /**
     * Test of getTemperature method, of class TemperatureSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetTargetTemperature() throws Exception {
        System.out.println("getTargetTemperature");

        double temperature = 3.141F;
        final TemperatureState temperatureState = TemperatureState.newBuilder().setTemperature(temperature).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(temperatureControllerRemote.getId()).applyServiceState(temperatureState, ServiceType.TARGET_TEMPERATURE_STATE_SERVICE);
        temperatureControllerRemote.requestData().get();
        assertEquals(temperatureState.getTemperature(), temperatureControllerRemote.getTargetTemperatureState().getTemperature(), 0.1, "The getter for the target temperature returns the wrong value!");
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
        TemperatureState temperatureState = TemperatureState.newBuilder().setTemperature(temperature).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(temperatureControllerRemote.getId()).applyServiceState(temperatureState, ServiceType.TEMPERATURE_STATE_SERVICE);
        temperatureControllerRemote.requestData().get();
        assertEquals(temperature, temperatureControllerRemote.getTemperatureState().getTemperature(), 0.1, "The getter for the temperature returns the wrong value!");
    }
}
