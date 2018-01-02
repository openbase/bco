package org.openbase.bco.manager.device.test.remote.unit;

/*
 * #%L
 * BCO Manager Device Test
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.unit.TemperatureControllerController;
import org.openbase.bco.dal.remote.unit.TemperatureControllerRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.TemperatureStateType.TemperatureState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TemperatureControllerRemoteTest extends AbstractBCODeviceManagerTest {

    private static TemperatureControllerRemote temperatureControllerRemote;

    public TemperatureControllerRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        temperatureControllerRemote = Units.getUnitsByLabel(MockRegistry.TEMPERATURE_CONTROLLER_LABEL, true, TemperatureControllerRemote.class).get(0);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of notifyUpdated method, of class TemperatureSensorRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getTemperature method, of class TemperatureSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetTargetTemperature() throws Exception {
        System.out.println("setTargetTemperature");
        double temperature = 42.0F;
        TemperatureState temperatureState = TemperatureState.newBuilder().setTemperature(temperature).build();
        temperatureControllerRemote.setTargetTemperatureState(temperatureState).get();
        temperatureControllerRemote.requestData().get();
        Assert.assertEquals("The getter for the target temperature returns the wrong value!", temperature, temperatureControllerRemote.getTargetTemperatureState().getTemperature(), 0.1);
    }

    /**
     * Test of getTemperature method, of class TemperatureSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetTargetTemperature() throws Exception {
        System.out.println("getTargetTemperature");
        double temperature = 3.141F;
        TemperatureState temperatureState = TemperatureState.newBuilder().setTemperature(temperature).build();
        ((TemperatureControllerController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(temperatureControllerRemote.getId())).applyDataUpdate(temperatureState, ServiceType.TARGET_TEMPERATURE_STATE_SERVICE);
        temperatureControllerRemote.requestData().get();
        Assert.assertEquals("The getter for the target temperature returns the wrong value!", temperatureState.getTemperature(), temperatureControllerRemote.getTargetTemperatureState().getTemperature(), 0.1);
    }

    /**
     * Test of getTemperature method, of class TemperatureSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetTemperature() throws Exception {
        System.out.println("getTemperature");
        double temperature = 37.0F;
        TemperatureState temperatureState = TemperatureState.newBuilder().setTemperature(temperature).build();
        ((TemperatureControllerController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(temperatureControllerRemote.getId())).applyDataUpdate(temperatureState);
        temperatureControllerRemote.requestData().get();
        Assert.assertEquals("The getter for the temperature returns the wrong value!", temperature, temperatureControllerRemote.getTemperatureState().getTemperature(), 0.1);
    }
}
