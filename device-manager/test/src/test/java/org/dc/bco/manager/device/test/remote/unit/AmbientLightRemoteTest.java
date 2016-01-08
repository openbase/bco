/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.test.remote.unit;

import org.dc.bco.dal.remote.unit.AmbientLightRemote;
import org.dc.bco.dal.lib.data.Location;
import org.dc.bco.dal.lib.transform.HSVColorToRGBColorTransformer;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import java.awt.Color;
import org.dc.bco.manager.device.core.DeviceManagerLauncher;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.bco.registry.mock.MockRegistryHolder;
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
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();
        

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
        if (registry != null) {
            MockRegistryHolder.shutdownMockRegistry();
        }
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
//    @Test(timeout = 60000)
    @Test
    public void testSetColor_Color() throws Exception {
        System.out.println("setColor");
        Color color = Color.MAGENTA;
        ambientLightRemote.setColor(color);
        ambientLightRemote.requestStatus();
        assertEquals("Color has not been set in time!", HSVColorToRGBColorTransformer.transform(color), ambientLightRemote.getData().getColor());
    }

    /**
     * Test of setColor method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
//    @Test(timeout = 60000)
    @Test
    public void testSetColor_HSVColorTypeHSVColor() throws Exception {
        System.out.println("setColor");
        HSVColorType.HSVColor color = HSVColorType.HSVColor.newBuilder().setHue(50).setSaturation(50).setValue(50).build();
        ambientLightRemote.setColor(color);
        ambientLightRemote.requestStatus();
        assertEquals("Color has not been set in time!", color, ambientLightRemote.getData().getColor());
    }

    /**
     * Test of getColor method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    //    @Test(timeout = 60000)
    @Test
    public void testRemoteGetColor() throws Exception {
        System.out.println("getColor");
        HSVColorType.HSVColor color = HSVColorType.HSVColor.newBuilder().setHue(66).setSaturation(63).setValue(33).build();
        ambientLightRemote.setColor(color);
        ambientLightRemote.requestStatus();
        assertEquals("Color has not been set in time or the return value from the getter is different!", color, ambientLightRemote.getColor());
    }
    
    /**
     * Test of getColor method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testRemoteCallGetColor() throws Exception {
        System.out.println("getColor");
        HSVColor color = HSVColorType.HSVColor.newBuilder().setHue(61).setSaturation(23).setValue(37).build();
        ambientLightRemote.setColor(color);
        ambientLightRemote.requestStatus();
        HSVColor colorResult = (HSVColor) ambientLightRemote.callMethod("getColor");
        assertEquals("Color has not been set in time or the return value from the getter is different!", color, colorResult);
    }

    /**
     * Test of setPowerState method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    //    @Test(timeout = 60000)
    @Test
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        PowerState.State state = PowerState.State.ON;
        ambientLightRemote.setPower(state);
        ambientLightRemote.requestStatus();
        assertEquals("Power state has not been set in time!", state, ambientLightRemote.getData().getPowerState().getValue());
    }

    /**
     * Test of getPowerState method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
   //    @Test(timeout = 60000)
    @Test
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        PowerState.State state = PowerState.State.OFF;
        ambientLightRemote.setPower(state);
        ambientLightRemote.requestStatus();
        assertEquals("Power state has not been set in time or the return value from the getter is different!", state, ambientLightRemote.getPower().getValue());
    }

    /**
     * Test of setBrightness method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    //    @Test(timeout = 60000)
    @Test
    public void testSetBrightness() throws Exception {
        System.out.println("setBrightness");
        Double brightness = 75d;
        ambientLightRemote.setBrightness(brightness);
        ambientLightRemote.requestStatus();
        assertEquals("Power state has not been set in time!", brightness, ambientLightRemote.getData().getColor().getValue(), 0.1);
    }

    /**
     * Test of getBrightness method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    //    @Test(timeout = 60000)
    @Test
    public void testGetBrightness() throws Exception {
        System.out.println("getBrightness");
        Double brightness = 25d;
        ambientLightRemote.setBrightness(brightness);
        ambientLightRemote.requestStatus();
        assertEquals("Brightness has not been set in time or the return value from the getter is different!", brightness, ambientLightRemote.getBrightness(), 0.1);
    }
}
