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
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.unit.LightSensorController;
import org.openbase.bco.dal.remote.unit.LightSensorRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.IlluminanceStateType.IlluminanceState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LightSensorRemoteTest extends AbstractBCODeviceManagerTest {

    private static LightSensorRemote lightSensorRemote;

    public LightSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        lightSensorRemote = Units.getUnitsByLabel(MockRegistry.LIGHT_SENSOR_LABEL, true, LightSensorRemote.class).get(0);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getIlluminance method, of class LightSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetIlluminance() throws Exception {
        System.out.println("getIlluminance");
        double illuminance = 0.5;
        IlluminanceState illuminanceState = IlluminanceState.newBuilder().setIlluminance(illuminance).build();
        ((LightSensorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(lightSensorRemote.getId())).applyDataUpdate(illuminanceState);
        lightSensorRemote.requestData().get();
        assertEquals("The getter for the illuminance returns the wrong value!", illuminanceState.getIlluminance(), lightSensorRemote.getIlluminanceState().getIlluminance(), 0.1);
    }
}
