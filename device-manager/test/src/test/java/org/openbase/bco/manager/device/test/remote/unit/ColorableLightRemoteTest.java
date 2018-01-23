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
import java.awt.Color;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.LightRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.transform.HSBColorToRGBColorTransformer;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.Remote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;
import rst.vision.HSBColorType.HSBColor;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ColorableLightRemoteTest extends AbstractBCODeviceManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColorableLightRemoteTest.class);

    private static ColorableLightRemote colorableLightRemote;

    public ColorableLightRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        colorableLightRemote = Units.getUnitsByLabel(MockRegistry.COLORABLE_LIGHT_LABEL, true, ColorableLightRemote.class).get(0);
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
        HSBColor color = HSBColor.newBuilder().setBrightness(50).setSaturation(70).setHue(150).build();
        colorableLightRemote.setColor(color).get();
        colorableLightRemote.requestData().get();
        assertEquals("Color has not been set in time!", color, colorableLightRemote.getData().getColorState().getColor().getHsbColor());
    }

//    @Test//(timeout = 10000)
//    public void testControllingColorableLightViaLightRemote() throws Exception {
//        System.out.println("testControllingColorableLightViaLightRemote");
//
//        colorableLightRemote.setPowerState(PowerState.State.OFF).get();
//
//        UnitConfig colorableLightConfig = colorableLightRemote.getConfig();
//        UnitConfig.Builder lightConfig = UnitConfig.newBuilder().setLabel("LightUnit").setScope(colorableLightConfig.getScope()).setId(colorableLightConfig.getId()).setType(UnitTemplateType.UnitTemplate.UnitType.LIGHT);
//        lightConfig.getEnablingStateBuilder().setValue(EnablingState.State.ENABLED);
//        try {
//            LightRemote lightRemote = new LightRemote();
//            lightRemote.setSessionManager(SessionManager.getInstance());
//            lightRemote.init(lightConfig.build());
//            lightRemote.activate();
//            lightRemote.requestData().get();
//            lightRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED);
//            int j = 0;
//
//            lightRemote.setPowerState(PowerState.newBuilder().setValue(PowerState.State.ON).build()).get();
//            colorableLightRemote.requestData().get();
//            assertEquals("ColorableLightRemote and LightRemote don't have the same powerState!", colorableLightRemote.getPowerState().getValue(), lightRemote.getPowerState().getValue());
//            Thread.sleep(200);
//
//            lightRemote.addServiceStateObserver(ServiceType.POWER_STATE_SERVICE, (Observer<PowerState>) (Observable<PowerState> source, PowerState data) -> {
//                System.out.println("Received power update in lightRemote [" + data.getValue() + "]");
//            });
//            colorableLightRemote.addServiceStateObserver(ServiceType.POWER_STATE_SERVICE, (Observer<PowerState>) (Observable<PowerState> source, PowerState data) -> {
//                System.out.println("Received power update in colorableLightRemote [" + data.getValue() + "]");
//            });
//
//            PowerState.State powerState;
//            for (int a = 0; a <= 15; a++) {
//                System.out.println("Iteration [" + a + "]");
//                if ((a % 2) == 0) {
//                    powerState = PowerState.State.OFF;
//                } else {
//                    powerState = PowerState.State.ON;
//                }
//
//                lightRemote.setPowerState(powerState).get();
//                colorableLightRemote.requestData().get();
//                System.out.println("Before test!");
//                assertEquals("ColorableLightRemote and LightRemote don't have the same powerState!", colorableLightRemote.getPowerState().getValue(), lightRemote.getPowerState().getValue());
//            }
////            int i = 0;
////            lightRemote.setPowerState(PowerState.newBuilder().setValue(PowerState.State.OFF).build()).get();
////            colorableLightRemote.requestData().get();
////            System.out.println("Before test!");
////            assertEquals("ColorableLightRemote and LightRemote don't have the same powerState!", colorableLightRemote.getPowerState().getValue(), lightRemote.getPowerState().getValue());
//        } catch (Exception ex) {
//            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
//        }
//    }

    /**
     * Test of setColor method, of class ColorableLightRemote.
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
     * Test of setColor method, of class ColorableLightRemote with invalid service state.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColor_InvalidHSBColor() throws Exception {
        System.out.println("setColor");
        HSBColor color = HSBColor.newBuilder().setHue(370).setSaturation(111).setBrightness(122).build();
        try {
            colorableLightRemote.setColor(color);
            Assert.assertTrue("Exception does not occure if invalid service state is set!", false);
        } catch (VerificationFailedException ex) {
            // should occure!
        }
        Assert.assertNotEquals("Invalid color has been applied!", color, colorableLightRemote.getHSBColor());
    }

    /**
     * Test of getColor method, of class ColorableLightRemote.
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
     * Test of getColor method, of class ColorableLightRemote.
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
     * Test of setPowerState method, of class ColorableLightRemote.
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
     * Test of getPowerState method, of class ColorableLightRemote.
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
     * Test of setBrightness method, of class ColorableLightRemote.
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
     * Test of getBrightness method, of class ColorableLightRemote.
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
        assertEquals("Neutral white was not set to the default value!", ColorStateOperationService.DEFAULT_NEUTRAL_WHITE, colorableLightRemote.getColorState().getColor().getHsbColor());
    }

    private int powerStateObserverUpdateNumber = 0;

    @Test(timeout = 10000)
    public void testPowerStateObserver() throws Exception {
        System.out.println("testPowerStateObserver");

        // set initial state to on for this test
        if (colorableLightRemote.getPowerState().getValue() == PowerState.State.OFF) {
            colorableLightRemote.setPowerState(PowerState.State.ON).get();
        }

        colorableLightRemote.addServiceStateObserver(ServiceType.POWER_STATE_SERVICE, new Observer<PowerState>() {

            int updateNumber = 0;

            @Override
            public void update(Observable<PowerState> source, PowerState data) throws Exception {
                powerStateObserverUpdateNumber++;
                if (powerStateObserverUpdateNumber == 1 || powerStateObserverUpdateNumber == 3) {
                    assertEquals("Notified on unexpected PowerState in update[" + powerStateObserverUpdateNumber + "]!", PowerState.State.OFF, data.getValue());
                } else if (powerStateObserverUpdateNumber == 2) {
                    assertEquals("Notified on unexpected PowerState in update[" + powerStateObserverUpdateNumber + "]!", PowerState.State.ON, data.getValue());
                }
            }
        });

        colorableLightRemote.setPowerState(PowerState.State.OFF).get();
        colorableLightRemote.setPowerState(PowerState.State.ON).get();
        colorableLightRemote.setNeutralWhite().get();
        colorableLightRemote.setBrightnessState(BrightnessState.newBuilder().setBrightness(14).build());
        colorableLightRemote.setColor(Color.RED).get();
        colorableLightRemote.setPowerState(PowerState.State.OFF).get();
        colorableLightRemote.setPowerState(PowerState.State.OFF).get();
        assertEquals("PowerStateObserver wasn't notified the correct amount of times!", 3, powerStateObserverUpdateNumber);
    }
}
