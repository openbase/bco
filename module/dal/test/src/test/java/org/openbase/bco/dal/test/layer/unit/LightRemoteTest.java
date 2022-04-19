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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.dal.lib.state.States.Power;
import org.openbase.bco.dal.remote.layer.unit.LightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LightRemoteTest extends AbstractBCODeviceManagerTest {

    private static LightRemote lightRemote;

    public LightRemoteTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();
        lightRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.LIGHT), true, LightRemote.class);
    }

    /**
     * Test of setPowerState method, of class LightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(15)
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        waitForExecution(lightRemote.setPowerState(Power.ON));
        assertEquals("Power has not been set in time!", Power.ON.getValue(), lightRemote.getData().getPowerState().getValue());
    }

    /**
     * Test of getPowerState method, of class LightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");

        // apply service state
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(lightRemote.getId()).applyServiceState(Power.OFF, ServiceType.POWER_STATE_SERVICE);

        // force sync
        lightRemote.requestData().get();

        // validate service state
        assertEquals("Light has not been set in time!", Power.OFF.getValue(), lightRemote.getPowerState().getValue());

        // apply service state
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(lightRemote.getId()).applyServiceState(Power.ON, ServiceType.POWER_STATE_SERVICE);

        // force sync
        lightRemote.requestData().get();

        // validate service state
        assertEquals("Light has not been set in time!", Power.ON.getValue(), lightRemote.getPowerState().getValue());
    }
}
