package org.openbase.bco.manager.device.test.remote.unit;

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
import java.awt.Color;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.transform.HSBColorToRGBColorTransformer;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.LightRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Remote;
import org.slf4j.LoggerFactory;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.vision.HSBColorType.HSBColor;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ColorableLightRemoteTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ColorableLightRemoteTest.class);

    private static DeviceManagerLauncher deviceManagerLauncher;
    private static ColorableLightRemote colorableLightRemote;
    private static MockRegistry registry;
    private static String label;

    public ColorableLightRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, InstantiationException, CouldNotPerformException, JPServiceException, InterruptedException {
        try {
            JPService.setupJUnitTestMode();
            JPService.registerProperty(JPHardwareSimulationMode.class, true);
//            JPService.registerProperty(JPRSBTransport.class, JPRSBTransport.TransportType.SPREAD);
            registry = MockRegistryHolder.newMockRegistry();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();
            deviceManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);

            label = MockRegistry.COLORABLE_LIGHT_LABEL;

            colorableLightRemote = new ColorableLightRemote();
            colorableLightRemote.initByLabel(label);
            colorableLightRemote.activate();
            colorableLightRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED);
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }
            if (colorableLightRemote != null) {
                colorableLightRemote.shutdown();
            }
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
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
    @Test(timeout = 10000)
    public void testSetColor_Color() throws Exception {
        System.out.println("setColor");
        Color color = Color.MAGENTA;
        colorableLightRemote.setColor(color).get();
        colorableLightRemote.requestData().get();
        assertEquals("Color has not been set in time!", HSBColorToRGBColorTransformer.transform(color), colorableLightRemote.getData().getColorState().getColor().getHsbColor());
    }

    @Test(timeout = 10000)
    public void testControllingColorableLightViaLightRemote() throws Exception {
        System.out.println("testControllingColorableLightViaLightRemote");

        try {
            LightRemote lightRemote = new LightRemote();
            lightRemote.initByLabel(label);
            lightRemote.activate();
            lightRemote.requestData().get();
            lightRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED);

            lightRemote.setPowerState(PowerState.newBuilder().setValue(PowerState.State.ON).build()).get();
            colorableLightRemote.requestData().get();

            assertEquals(lightRemote.getPowerState().getValue(), colorableLightRemote.getPowerState().getValue());

            lightRemote.setPowerState(PowerState.newBuilder().setValue(PowerState.State.OFF).build()).get();
            colorableLightRemote.requestData().get();

            assertEquals(lightRemote.getPowerState().getValue(), colorableLightRemote.getPowerState().getValue());
        } catch (InterruptedException | CouldNotPerformException | ExecutionException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    /**
     * Test of setColor method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColor_HSBColor() throws Exception {
        System.out.println("setColor");
        HSBColor color = HSBColor.newBuilder().setHue(50).setSaturation(50).setBrightness(50).build();
        colorableLightRemote.setColor(color).get();
        colorableLightRemote.requestData().get();
        assertEquals("Color has not been set in time!", color, colorableLightRemote.getHSBColor());
    }

    /**
     * Test of getColor method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testRemoteGetColor() throws Exception {
        System.out.println("getColor");
        HSBColor color = HSBColor.newBuilder().setHue(66).setSaturation(63).setBrightness(33).build();
        colorableLightRemote.setColor(color).get();
        colorableLightRemote.requestData().get();
        assertEquals("Color has not been set in time or the return value from the getter is different!", color, colorableLightRemote.getHSBColor());
    }

    /**
     * Test of getColor method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testRemoteCallGetColor() throws Exception {
        System.out.println("getColor");
        HSBColor color = HSBColor.newBuilder().setHue(61).setSaturation(23).setBrightness(37).build();
        colorableLightRemote.setColor(color).get();
        colorableLightRemote.requestData().get();
        ColorState colorResult = (ColorState) colorableLightRemote.callMethodAsync("getColorState").get();
        assertEquals("Color has not been set in time or the return value from the getter is different!", color, colorResult.getColor().getHsbColor());
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
        colorableLightRemote.setPowerState(state).get();
        colorableLightRemote.requestData().get();
        assertEquals("Power state has not been set in time!", state.getValue(), colorableLightRemote.getData().getPowerState().getValue());
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
        colorableLightRemote.setPowerState(state).get();
        colorableLightRemote.requestData().get();
        assertEquals("Power state has not been set in time or the return value from the getter is different!", state.getValue(), colorableLightRemote.getPowerState().getValue());
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
        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).build();
        colorableLightRemote.setBrightnessState(brightnessState).get();
        colorableLightRemote.requestData().get();
        assertEquals("Brightness has not been set in time!", brightness, colorableLightRemote.getHSBColor().getBrightness(), 0.1);
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
        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).build();
        colorableLightRemote.setBrightnessState(brightnessState).get();
        colorableLightRemote.requestData().get();
        assertEquals("Brightness has not been set in time or the return value from the getter is different!", brightnessState.getBrightness(), colorableLightRemote.getBrightnessState().getBrightness(), 0.1);
    }

    @Test(timeout = 10000)
    public void testSetNeutralWhite() throws Exception {
        System.out.println("testSetNeutralWhite");
        colorableLightRemote.setNeutralWhite().get();
        colorableLightRemote.requestData().get();
        assertEquals("Neutral white was not set to the default value!", ColorStateOperationService.DEFAULT_NEUTRAL_WHITE, colorableLightRemote.getColorState().getColor().getRgbColor());
    }
}
