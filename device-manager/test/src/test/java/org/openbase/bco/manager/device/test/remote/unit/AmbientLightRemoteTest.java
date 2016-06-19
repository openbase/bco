package org.openbase.bco.manager.device.test.remote.unit;

/*
 * #%L
 * COMA DeviceManager Test
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import org.openbase.bco.dal.remote.unit.AmbientLightRemote;
import org.openbase.bco.dal.lib.data.Location;
import org.openbase.bco.dal.lib.transform.HSVColorToRGBColorTransformer;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import java.awt.Color;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.vision.HSVColorType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author thuxohl
 */
public class AmbientLightRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AmbientLightRemoteTest.class);

    private static DeviceManagerLauncher deviceManagerLauncher;
    private static AmbientLightRemote ambientLightRemote;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public AmbientLightRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, InstantiationException, CouldNotPerformException, JPServiceException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
//        JPService.registerProperty(JPRSBTransport.class, JPRSBTransport.TransportType.SOCKET);
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();
        deviceManagerLauncher.getDeviceManager().waitForInit(30, TimeUnit.SECONDS);

        location = new Location(registry.getLocation());
        label = MockRegistry.AMBIENT_LIGHT_LABEL;

        ambientLightRemote = new AmbientLightRemote();
        ambientLightRemote.init(label, location);
        ambientLightRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (ambientLightRemote != null) {
            ambientLightRemote.shutdown();
        }
        MockRegistryHolder.shutdownMockRegistry();
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {

    }

    /**
     * Test of setColor method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColor_Color() throws Exception {
        System.out.println("setColor");
        Color color = Color.MAGENTA;
        ambientLightRemote.setColor(color).get();
        ambientLightRemote.requestData().get();
        assertEquals("Color has not been set in time!", HSVColorToRGBColorTransformer.transform(color), ambientLightRemote.getData().getColor());
    }

    /**
     * Test of setColor method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColor_HSVColorTypeHSVColor() throws Exception {
        System.out.println("setColor");
        HSVColorType.HSVColor color = HSVColorType.HSVColor.newBuilder().setHue(50).setSaturation(50).setValue(50).build();
        ambientLightRemote.setColor(color).get();
        ambientLightRemote.requestData().get();
        assertEquals("Color has not been set in time!", color, ambientLightRemote.getData().getColor());
    }

    /**
     * Test of getColor method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testRemoteGetColor() throws Exception {
        System.out.println("getColor");
        HSVColorType.HSVColor color = HSVColorType.HSVColor.newBuilder().setHue(66).setSaturation(63).setValue(33).build();
        ambientLightRemote.setColor(color).get();
        ambientLightRemote.requestData().get();
        assertEquals("Color has not been set in time or the return value from the getter is different!", color, ambientLightRemote.getColor());
    }

    /**
     * Test of getColor method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testRemoteCallGetColor() throws Exception {
        System.out.println("getColor");
        HSVColor color = HSVColorType.HSVColor.newBuilder().setHue(61).setSaturation(23).setValue(37).build();
        ambientLightRemote.setColor(color).get();
        ambientLightRemote.requestData().get();
        HSVColor colorResult = (HSVColor) ambientLightRemote.callMethodAsync("getColor").get();
        assertEquals("Color has not been set in time or the return value from the getter is different!", color, colorResult);
    }

    /**
     * Test of setPowerState method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        ambientLightRemote.setPower(state).get();
        ambientLightRemote.requestData().get();
        assertEquals("Power state has not been set in time!", state, ambientLightRemote.getData().getPowerState());
    }

    /**
     * Test of getPowerState method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        ambientLightRemote.setPower(state).get();
        ambientLightRemote.requestData().get();
        assertEquals("Power state has not been set in time or the return value from the getter is different!", state, ambientLightRemote.getPower());
    }

    /**
     * Test of setBrightness method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetBrightness() throws Exception {
        System.out.println("setBrightness");
        Double brightness = 75d;
        ambientLightRemote.setBrightness(brightness).get();
        ambientLightRemote.requestData().get();
        assertEquals("Brightness has not been set in time!", brightness, ambientLightRemote.getData().getColor().getValue(), 0.1);
    }

    /**
     * Test of getBrightness method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetBrightness() throws Exception {
        System.out.println("getBrightness");
        Double brightness = 25d;
        ambientLightRemote.setBrightness(brightness).get();
        ambientLightRemote.requestData().get();
        assertEquals("Brightness has not been set in time or the return value from the getter is different!", brightness, ambientLightRemote.getBrightness(), 0.1);
    }
}
