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
import org.openbase.bco.dal.lib.state.States.Power;
import org.openbase.bco.dal.remote.layer.unit.DimmableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DimmableLightRemoteTest extends AbstractBCODeviceManagerTest {

    private static DimmableLightRemote dimmableLightRemote;

    public DimmableLightRemoteTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();
        dimmableLightRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.DIMMABLE_LIGHT), true, DimmableLightRemote.class);
    }

    /**
     * Test of notifyUpdated method, of class DimmerRemote.
     */
    @Disabled
    public void testNotifyUpdated() {
    }

    /**
     * Test of setPower method, of class DimmerRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testSetPower() throws Exception {
        System.out.println("setPowerState");
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        waitForExecution(dimmableLightRemote.setPowerState(state));
        assertEquals("Power has not been set in time!", state.getValue(), dimmableLightRemote.getData().getPowerState().getValue());
    }

    /**
     * Test of getPower method, of class DimmerRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetPower() throws Exception {
        System.out.println("getPowerState");
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dimmableLightRemote.getId()).applyServiceState(Power.ON, ServiceType.POWER_STATE_SERVICE);
        dimmableLightRemote.requestData().get();
        assertEquals("Power has not been set in time!", Power.ON.getValue(), dimmableLightRemote.getPowerState().getValue());
    }

    /**
     * Test of setDimm method, of class DimmerRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testSetBrightness() throws Exception {
        System.out.println("setBrightness");
        Double brightness = 0.66d;
        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).build();
        waitForExecution(dimmableLightRemote.setBrightnessState(brightnessState));
        assertEquals("Brightness has not been set in time!", brightness, dimmableLightRemote.getBrightnessState().getBrightness(), 0.001);
    }

    /**
     * Test of getBrightness method, of class DimmerRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetBrightness() throws Exception {
        System.out.println("getBrightness");

        final Double brightness = 0.70d;
        final BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dimmableLightRemote.getId()).applyServiceState(brightnessState, ServiceType.BRIGHTNESS_STATE_SERVICE);
        dimmableLightRemote.requestData().get();
        assertEquals("Brightness has not been set in time!", brightnessState.getBrightness(), dimmableLightRemote.getBrightnessState().getBrightness(), 0.001);
    }
}
