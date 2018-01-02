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
import org.junit.Ignore;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.unit.DimmableLightController;
import org.openbase.bco.dal.remote.unit.DimmableLightRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.PowerStateType.PowerState;

/**
 * TODO reactivate this test.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DimmableLightRemoteTest extends AbstractBCODeviceManagerTest {

    private static DimmableLightRemote dimmableLightRemote;

    public DimmableLightRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        dimmableLightRemote = Units.getUnitsByLabel(MockRegistry.DIMMABLE_LIGHT_LABEL, true, DimmableLightRemote.class).get(0);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of notifyUpdated method, of class DimmerRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of setPower method, of class DimmerRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetPower() throws Exception {
        System.out.println("setPowerState");
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        dimmableLightRemote.setPowerState(state).get();
        dimmableLightRemote.requestData().get();
        assertEquals("Power has not been set in time!", state.getValue(), dimmableLightRemote.getData().getPowerState().getValue());
    }

    /**
     * Test of getPower method, of class DimmerRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetPower() throws Exception {
        System.out.println("getPowerState");
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        ((DimmableLightController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dimmableLightRemote.getId())).applyDataUpdate(state);
        dimmableLightRemote.requestData().get();
        assertEquals("Power has not been set in time!", state.getValue(), dimmableLightRemote.getPowerState().getValue());
    }

    /**
     * Test of setDimm method, of class DimmerRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetBrightness() throws Exception {
        System.out.println("setBrightness");
        Double brightness = 66d;
        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).build();
        dimmableLightRemote.setBrightnessState(brightnessState).get();
        dimmableLightRemote.requestData().get();
        assertEquals("Dimm has not been set in time!", brightness, dimmableLightRemote.getBrightnessState().getBrightness(), 0.1);
    }

    /**
     * Test of getDimm method, of class DimmerRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetBrightness() throws Exception {
        System.out.println("getBrightness");
        Double brightness = 70.0d;
        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).build();
        ((DimmableLightController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dimmableLightRemote.getId())).applyDataUpdate(brightnessState);
        dimmableLightRemote.requestData().get();
        assertEquals("Dimm has not been set in time!", brightnessState.getBrightness(), dimmableLightRemote.getBrightnessState().getBrightness(), 0.1);
    }
}
