package org.openbase.bco.dal.test.layer.unit;

/*
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.junit.*;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.state.States.Power;
import org.openbase.bco.dal.remote.action.Actions;
import org.openbase.bco.dal.remote.layer.unit.PowerSwitchRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerSwitchRemoteTest extends AbstractBCODeviceManagerTest {

    private static PowerSwitchRemote powerSwitchRemote;

    public PowerSwitchRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        powerSwitchRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.POWER_SWITCH), true, PowerSwitchRemote.class);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of setPowerState method, of class PowerPlugRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        waitForExecution(powerSwitchRemote.setPowerState(Power.ON));
        assertEquals("Power state has not been set in time!", Power.ON.getValue(), powerSwitchRemote.getData().getPowerState().getValue());
    }

    /**
     * Test of getPowerState method, of class PowerPlugRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        // apply service state
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(powerSwitchRemote.getId()).applyServiceState(Power.OFF, ServiceType.POWER_STATE_SERVICE);

        // force sync
        powerSwitchRemote.requestData().get();

        // validate service state
        assertEquals("Switch has not been set in time!", Power.OFF.getValue(), powerSwitchRemote.getPowerState().getValue());

        // apply service state
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(powerSwitchRemote.getId()).applyServiceState(Power.ON, ServiceType.POWER_STATE_SERVICE);

        // force sync
        powerSwitchRemote.requestData().get();

        // validate service state
        assertEquals("Switch has not been set in time!", Power.ON.getValue(), powerSwitchRemote.getPowerState().getValue());
    }

    /**
     * Test of notifyUpdated method, of class PowerPlugRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }
}
