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
import org.openbase.bco.dal.lib.layer.unit.PowerConsumptionSensorController;
import org.openbase.bco.dal.remote.unit.PowerConsumptionSensorRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerConsumptionStateType.PowerConsumptionState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerConsumptionSensorRemoteTest extends AbstractBCODeviceManagerTest {

    private static PowerConsumptionSensorRemote powerConsumptionRemote;

    public PowerConsumptionSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        powerConsumptionRemote = Units.getUnitsByLabel(MockRegistry.POWER_CONSUMPTION_LABEL, true, PowerConsumptionSensorRemote.class).get(0);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of notifyUpdated method, of class PowerConsumptionSensorRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getPowerConsumption method, of class
     * PowerConsumptionSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetPowerConsumption() throws Exception {
        System.out.println("getPowerConsumption");
        double consumption = 200d;
        double voltage = 100d;
        double current = 2d;
        PowerConsumptionState state = PowerConsumptionState.newBuilder().setConsumption(consumption).setCurrent(current).setVoltage(voltage).build();
        ((PowerConsumptionSensorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(powerConsumptionRemote.getId())).applyDataUpdate(state);
        powerConsumptionRemote.requestData().get();
        Assert.assertEquals("The getter for the power consumption returns the wrong voltage value!", state.getVoltage(), powerConsumptionRemote.getPowerConsumptionState().getVoltage(), 0.1);
        Assert.assertEquals("The getter for the power consumption returns the wrong consumption value!", state.getConsumption(), powerConsumptionRemote.getPowerConsumptionState().getConsumption(), 0.1);
        Assert.assertEquals("The getter for the power consumption returns the wrong cirremt value!", state.getCurrent(), powerConsumptionRemote.getPowerConsumptionState().getCurrent(), 0.1);
    }
}
