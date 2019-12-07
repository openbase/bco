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
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.remote.action.Actions;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.LightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.jp.JPRSBLegacyMode;
import org.openbase.jul.pattern.Observer;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ColorableLightRemoteTest extends AbstractBCODeviceManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColorableLightRemoteTest.class);

    private static ColorableLightRemote colorableLightRemote;

    public ColorableLightRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {

        // legacy mode needed for testLegacyRemoteCallGetColor() test.
        JPService.registerProperty(JPRSBLegacyMode.class, true);
        AbstractBCODeviceManagerTest.setUpClass();
        colorableLightRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.COLORABLE_LIGHT), true, ColorableLightRemote.class);
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {

    }

    /**
     * Test of setColor method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColor_Color() throws Exception {
        System.out.println("setColor");
        HSBColor color = HSBColor.newBuilder().setBrightness(0.50).setSaturation(0.70).setHue(150).build();
        waitForExecution(colorableLightRemote.setColor(color));
        assertEquals("Color has not been set in time!", color, colorableLightRemote.getData().getColorState().getColor().getHsbColor());
    }

    /**
     * Test controlling a colorable light using a light remote.
     *
     * @throws Exception if an error occurs
     */
    @Test(timeout = 15000)
    public void testControllingViaLightRemote() throws Exception {
        System.out.println("testControllingViaLightRemote");

        waitForExecution(colorableLightRemote.setPowerState(State.OFF));
        final LightRemote lightRemote = new LightRemote();
        try {
            // create a light remote from colorable light config and wait for data
            lightRemote.setSessionManager(SessionManager.getInstance());
            lightRemote.init(colorableLightRemote.getConfig());
            lightRemote.activate();
            lightRemote.waitForData();

            while (!colorableLightRemote.getPowerState().equals(lightRemote.getPowerState())) {
                Thread.sleep(10);
            }

            // test if the initial state was synced correctly
            assertEquals(colorableLightRemote.getPowerState(), lightRemote.getPowerState());
            // test controlling vid light remote
            waitForExecution(lightRemote.setPowerState(PowerState.newBuilder().setValue(PowerState.State.ON).build()));
            lightRemote.requestData().get();
            while (!colorableLightRemote.getPowerState().equals(lightRemote.getPowerState())) {
                Thread.sleep(10);
            }
            assertEquals(lightRemote.getPowerState().getValue(), colorableLightRemote.getPowerState().getValue());

            // test controlling via colorable light remote
            waitForExecution(colorableLightRemote.setPowerState(State.OFF));
            lightRemote.requestData().get();
            while (!colorableLightRemote.getPowerState().equals(lightRemote.getPowerState())) {
                Thread.sleep(10);
            }
            assertEquals(colorableLightRemote.getPowerState().getValue(), lightRemote.getPowerState().getValue());
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        } finally {
            // custom remote not handled by shutdown hook so make sure to call shutdown
            lightRemote.shutdown();
        }
    }

    /**
     * Test of setColor method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColor_HSBColor() throws Exception {
        System.out.println("setColor");
        HSBColor color = HSBColor.newBuilder().setHue(50).setSaturation(0.50).setBrightness(0.50).build();
        waitForExecution(colorableLightRemote.setColor(color));
        assertEquals("Color has not been set in time!", color, colorableLightRemote.getHSBColor());
    }

    /**
     * Test of setColor method, of class ColorableLightRemote with invalid service state.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColor_InvalidHSBColor() throws Exception {
        System.out.println("setColor");

        // invalid because hue value is to high.
        HSBColor color = HSBColor.newBuilder().setHue(370).setSaturation(0.3d).setBrightness(0.8d).build();
        try {
            colorableLightRemote.setColor(color).get();
            Assert.assertTrue("Exception does not occur if invalid service state is set!", false);
        } catch (ExecutionException ex) {
            // should occur!
        }
        Assert.assertNotEquals("Invalid hue color has been applied!", color.getHue(), colorableLightRemote.getHSBColor().getHue());
        Assert.assertNotEquals("Parts of an invalid color has been applied!", color.getSaturation(), colorableLightRemote.getHSBColor().getSaturation());
        Assert.assertNotEquals("Parts of an invalid color has been applied!", color.getBrightness(), colorableLightRemote.getHSBColor().getBrightness());
    }

    /**
     * Test of getColor method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testRemoteGetColor() throws Exception {
        System.out.println("getColor");
        HSBColor color = HSBColor.newBuilder().setHue(66).setSaturation(0.63).setBrightness(0.33).build();
        waitForExecution(colorableLightRemote.setColor(color));
        assertEquals("Color has not been set in time or the return value from the getter is different!", color, colorableLightRemote.getHSBColor());
    }

    /**
     * Test of getColor method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testLegacyRemoteCallGetColor() throws Exception {
        System.out.println("getColor");
        HSBColor color = HSBColor.newBuilder().setHue(61).setSaturation(0.23).setBrightness(0.37).build();
        waitForExecution(colorableLightRemote.setColor(color));
        ColorState colorResult = (ColorState) colorableLightRemote.callMethodAsync("getColorState").get();
        assertEquals("Color has not been set in time or the return value from the getter is different!", color, colorResult.getColor().getHsbColor());
    }

    /**
     * Test of setPowerState method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        waitForExecution(colorableLightRemote.setPowerState(state));
        assertEquals("Power state has not been set in time!", state.getValue(), colorableLightRemote.getData().getPowerState().getValue());
    }

    /**
     * Test of getPowerState method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        waitForExecution(colorableLightRemote.setPowerState(state));
        assertEquals("Power state has not been set in time or the return value from the getter is different!", state.getValue(), colorableLightRemote.getPowerState().getValue());
    }

    /**
     * Test of setBrightness method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetBrightness() throws Exception {
        System.out.println("setBrightness");
        Double brightness = 0.75d;
        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).build();
        waitForExecution(colorableLightRemote.setBrightnessState(brightnessState));
        assertEquals("Brightness has not been set in time!", brightness, colorableLightRemote.getHSBColor().getBrightness(), 0.001);
    }

    /**
     * Test of getBrightness method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetBrightness() throws Exception {
        System.out.println("getBrightness");
        Double brightness = 0.25d;
        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).build();
        waitForExecution(colorableLightRemote.setBrightnessState(brightnessState));
        assertEquals("Brightness has not been set in time or the return value from the getter is different!", brightnessState.getBrightness(), colorableLightRemote.getBrightnessState().getBrightness(), 0.001);
    }

    @Test(timeout = 10000)
    public void testSetNeutralWhite() throws Exception {
        System.out.println("testSetNeutralWhite");
        waitForExecution(colorableLightRemote.setNeutralWhite());
        assertEquals("Neutral white was not set to the default value!", ColorStateOperationService.DEFAULT_NEUTRAL_WHITE, colorableLightRemote.getColorState().getColor().getHsbColor());
    }

    private int powerStateObserverUpdateNumber = 0;

    /**
     * Deactivated because timestamps are not filtered anymore.
     *
     * @throws Exception if something fails.
     */
    @Test(timeout = 15000)
    public void testPowerStateObserver() throws Exception {
        System.out.println("testPowerStateObserver");

        // set initial state to on for this test
        waitForExecution(colorableLightRemote.setPowerState(PowerState.State.ON));

        final Observer<ServiceStateProvider<PowerState>, PowerState> powerStateObserver = (source, data) -> {
            if (!data.hasValue()) {
                LOGGER.warn("Notification with empty value");
                return;
            }

            powerStateObserverUpdateNumber++;
            LOGGER.info("Power state update {} with {}", powerStateObserverUpdateNumber, data.getValue().name());
        };

        colorableLightRemote.addServiceStateObserver(ServiceType.POWER_STATE_SERVICE, powerStateObserver);

        waitForExecution(colorableLightRemote.setPowerState(PowerState.State.OFF)); // notification 1
        waitForExecution(colorableLightRemote.setPowerState(PowerState.State.ON)); // notification 2
        waitForExecution(colorableLightRemote.setNeutralWhite());
        waitForExecution(colorableLightRemote.setBrightnessState(BrightnessState.newBuilder().setBrightness(0.14d).build()));
        waitForExecution(colorableLightRemote.setColor(HSBColor.newBuilder().setBrightness(0.12d).setSaturation(0.10d).build()));
        waitForExecution(colorableLightRemote.setPowerState(PowerState.State.OFF)); // notification 3
        waitForExecution(colorableLightRemote.setPowerState(PowerState.State.OFF));

        assertEquals("PowerStateObserver wasn't notified the correct amount of times!", 3, powerStateObserverUpdateNumber);

        colorableLightRemote.removeServiceStateObserver(ServiceType.POWER_STATE_SERVICE, powerStateObserver);
    }
}
