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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.remote.layer.unit.ButtonRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.extension.type.processing.TimestampJavaTimeTransform;
import org.openbase.jul.schedule.Stopwatch;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ButtonStateType.ButtonState;
import org.openbase.type.domotic.state.ButtonStateType.ButtonState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ButtonRemoteTest extends AbstractBCODeviceManagerTest {

    private static ButtonRemote buttonRemote;

    public ButtonRemoteTest() {
    }

    @BeforeAll
    public static void loadUnits() throws Throwable {
        buttonRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.BUTTON), true, ButtonRemote.class);
    }

    /**
     * Test of getButtonState method, of class ButtonRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetButtonState() throws Exception {
        System.out.println("getButtonState");
        ButtonState buttonState = ButtonState.newBuilder().setValue(ButtonState.State.PRESSED).build();
        ButtonState controllerButtonState = deviceManagerLauncher
                .getLaunchable()
                .getUnitControllerRegistry()
                .get(buttonRemote.getId())
                .applyServiceState(buttonState, ServiceType.BUTTON_STATE_SERVICE);
        buttonRemote.requestData().get();
        assertEquals(buttonState.getValue(), controllerButtonState.getValue(), "The getter for the button returns the wrong value!");
        assertEquals(buttonState.getValue(), buttonRemote.getButtonState().getValue(), "The getter for the button returns the wrong value!");
    }

    /**
     * Test if the timestamp in the buttonState is set correctly.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetButtonStateTimestamp() throws Exception {
        System.out.println("testGetButtonStateTimestamp");
        long timestamp;
        Stopwatch stopwatch = new Stopwatch();

        stopwatch.start();
        ButtonState buttonState = ButtonState.newBuilder().setValue(ButtonState.State.DOUBLE_PRESSED).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId()).applyServiceState(buttonState, ServiceType.BUTTON_STATE_SERVICE);
        stopwatch.stop();
        buttonRemote.requestData().get();
        assertEquals(buttonState.getValue(), buttonRemote.getButtonState().getValue(), "The getter for the button returns the wrong value!");
        timestamp = TimestampJavaTimeTransform.transform(Services.getLatestValueOccurrence(State.DOUBLE_PRESSED, buttonRemote.getButtonState()));
        assertTrue((timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()), "The timestamp of the button state has not been updated!");

        // just to be safe that the next test does not set the motion state in the same millisecond
        Thread.sleep(1);

        stopwatch.start();
        buttonState = ButtonState.newBuilder().setValue(ButtonState.State.PRESSED).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId()).applyServiceState(buttonState, ServiceType.BUTTON_STATE_SERVICE);
        stopwatch.stop();
        buttonRemote.requestData().get();
        assertEquals(buttonState.getValue(), buttonRemote.getButtonState().getValue(), "The getter for the button returns the wrong value!");
        timestamp = TimestampJavaTimeTransform.transform(Services.getLatestValueOccurrence(State.PRESSED, buttonRemote.getButtonState()));
        assertTrue((timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()), "The timestamp of the button state has not been updated!");

        // just to be safe that the next test does not set the motion state in the same millisecond
        Thread.sleep(1);

        stopwatch.start();
        buttonState = ButtonState.newBuilder().setValue(ButtonState.State.RELEASED).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId()).applyServiceState(buttonState, ServiceType.BUTTON_STATE_SERVICE);
        stopwatch.stop();
        buttonRemote.requestData().get();
        assertEquals(buttonState.getValue(), buttonRemote.getButtonState().getValue(), "The getter for the button returns the wrong value!");
        timestamp = TimestampJavaTimeTransform.transform(Services.getLatestValueOccurrence(State.PRESSED, buttonRemote.getButtonState()));
        assertFalse((timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()), "The timestamp of the button state has been updated even though it should not!");
    }
}
