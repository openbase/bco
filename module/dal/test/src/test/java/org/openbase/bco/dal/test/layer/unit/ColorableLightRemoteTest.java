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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.LightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPLogLevel;
import org.openbase.jul.communication.jp.JPComLegacyMode;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observer;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
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



/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ColorableLightRemoteTest extends AbstractBCODeviceManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColorableLightRemoteTest.class);

    private static ColorableLightRemote colorableLightRemote;

    @BeforeAll
    public static void loadUnits() throws Throwable {
        colorableLightRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.COLORABLE_LIGHT), true, ColorableLightRemote.class);
    }

    @AfterAll
    public static void tearDownTest() throws Throwable {
        JPService.registerProperty(JPComLegacyMode.class, false);
    }

    /**
     * Test of setColor method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testSetColor_Color() throws Exception {
        System.out.println("setColor");
        HSBColor color = HSBColor.newBuilder().setBrightness(0.50).setSaturation(0.70).setHue(150).build();
        waitForExecution(colorableLightRemote.setColor(color));
        assertEquals(color, colorableLightRemote.getData().getColorState().getColor().getHsbColor(), "Color has not been set in time!");
    }

    /**
     * Test of setColor method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testSetColor_HSBColor() throws Exception {
        System.out.println("setColor");
        HSBColor color = HSBColor.newBuilder().setHue(50).setSaturation(0.50).setBrightness(0.50).build();
        waitForExecution(colorableLightRemote.setColor(color));
        assertEquals(color, colorableLightRemote.getHSBColor(), "Color has not been set in time!");
    }

    /**
     * Test of setColor method, of class ColorableLightRemote with invalid service state.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testSetColor_InvalidHSBColor() throws Exception {
        System.out.println("setColor");

        // invalid because hue value is to high.
        HSBColor color = HSBColor.newBuilder().setHue(370).setSaturation(0.3d).setBrightness(0.8d).build();
        try {
            colorableLightRemote.setColor(color).get();
            assertTrue(false, "Exception does not occur if invalid service state is set!");
        } catch (ExecutionException ex) {
            // should occur!
        }
        assertNotEquals(color.getHue(), colorableLightRemote.getHSBColor().getHue(), "Invalid hue color has been applied!");
        assertNotEquals(color.getSaturation(), colorableLightRemote.getHSBColor().getSaturation(), "Parts of an invalid color has been applied!");
        assertNotEquals(color.getBrightness(), colorableLightRemote.getHSBColor().getBrightness(), "Parts of an invalid color has been applied!");

        colorableLightRemote.requestData().get();
        for (ActionDescription actionDescription : colorableLightRemote.getActionList()) {

            // filter termination action
            if (actionDescription.getPriority() == Priority.TERMINATION) {
                continue;
            }

            // validate if done
            final RemoteAction remoteAction = new RemoteAction(actionDescription);
            assertTrue(remoteAction.isDone(), remoteAction + " is not done!");
        }
    }

    /**
     * Test of getColor method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testRemoteGetColor() throws Exception {
        System.out.println("getColor");
        HSBColor color = HSBColor.newBuilder().setHue(66).setSaturation(0.63).setBrightness(0.33).build();
        waitForExecution(colorableLightRemote.setColor(color));
        assertEquals(color, colorableLightRemote.getHSBColor(), "Color has not been set in time or the return value from the getter is different!");
    }

    /**
     * Test of setPowerState method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        waitForExecution(colorableLightRemote.setPowerState(state));
        assertEquals(state.getValue(), colorableLightRemote.getData().getPowerState().getValue(), "Power state has not been set in time!");
    }

    /**
     * Test of getPowerState method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        waitForExecution(colorableLightRemote.setPowerState(state));
        assertEquals(state.getValue(), colorableLightRemote.getPowerState().getValue(), "Power state has not been set in time or the return value from the getter is different!");
    }

    /**
     * Test of setBrightness method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testSetBrightness() throws Exception {
        System.out.println("setBrightness");
        Double brightness = 0.75d;
        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).build();
        waitForExecution(colorableLightRemote.setBrightnessState(brightnessState));
        assertEquals(brightness, colorableLightRemote.getHSBColor().getBrightness(), 0.001, "Brightness has not been set in time!");
    }

    /**
     * Test of getBrightness method, of class ColorableLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetBrightness() throws Exception {
        System.out.println("getBrightness");
        Double brightness = 0.25d;
        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).build();
        waitForExecution(colorableLightRemote.setBrightnessState(brightnessState));
        assertEquals(brightnessState.getBrightness(), colorableLightRemote.getBrightnessState().getBrightness(), 0.001, "Brightness has not been set in time or the return value from the getter is different!");
    }

    @Test
    @Timeout(10)
    public void testSetNeutralWhite() throws Exception {
        System.out.println("testSetNeutralWhite");
        waitForExecution(colorableLightRemote.setNeutralWhite());
        assertEquals(ColorStateOperationService.DEFAULT_NEUTRAL_WHITE, colorableLightRemote.getColorState().getColor().getHsbColor(), "Neutral white was not set to the default value!");
    }

    private int powerStateObserverUpdateNumber = 0;

    /**
     * Deactivated because timestamps are not filtered anymore.
     *
     * @throws Exception if something fails.
     */
    @Test
    @Timeout(15)
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

        assertEquals(3, powerStateObserverUpdateNumber, "PowerStateObserver wasn't notified the correct amount of times!");

        colorableLightRemote.removeServiceStateObserver(ServiceType.POWER_STATE_SERVICE, powerStateObserver);
    }
}
