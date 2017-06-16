package org.openbase.bco.manager.device.test;

/*
 * #%L
 * BCO Manager Device Test
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import org.junit.Test;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DalRegisterDeviceTest extends AbstractBCODeviceManagerTest {

    private static final String deviceConfigLabel = "RunningDevice";

    public DalRegisterDeviceTest() {
    }

    @Before
    public void setUp() throws InitializationException, InstantiationException {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test registering a new device while the device manager is running.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testRegisterDeviceWhileRunning() throws Exception {
        System.out.println("testRegisterDeviceWhileRunning");

        DeviceClass deviceClass = Registries.getDeviceRegistry().registerDeviceClass(MockRegistry.getDeviceClass("TestRegisterDeviceWhileRunnint", "DeviceManagerLauncherAndCoKG123456", "DeviceManagerLauncherAndCoKG", UnitType.COLORABLE_LIGHT)).get();
        mockRegistry.waitForDeviceClass(deviceClass);

        UnitConfig deviceUnitConfig = Registries.getUnitRegistry().registerUnitConfig(MockRegistry.getDeviceConfig(deviceConfigLabel, "DeviceManagerLauncherTestSerialNumber", deviceClass)).get();
        UnitConfig colorableLightConfig = Registries.getUnitRegistry().getUnitConfigById(deviceUnitConfig.getDeviceConfig().getUnitId(0));

        ColorableLightRemote colorableLightRemote = Units.getUnit(colorableLightConfig, true, ColorableLightRemote.class);

        PowerState state = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        colorableLightRemote.setPowerState(state).get();
        assertEquals("Power state has not been set in time!", state.getValue(), colorableLightRemote.getData().getPowerState().getValue());
    }
}
