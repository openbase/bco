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

import org.junit.*;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.remote.layer.unit.ButtonRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.extension.type.processing.TimestampJavaTimeTransform;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.schedule.Stopwatch;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ButtonStateType.ButtonState;
import org.openbase.type.domotic.state.ButtonStateType.ButtonState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ButtonRemoteTest extends AbstractBCODeviceManagerTest {

    private static ButtonRemote buttonRemote;

    public ButtonRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();
        buttonRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.BUTTON), true, ButtonRemote.class);
    }

    /**
     * Test of notifyUpdated method, of class ButtonRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getButtonState method, of class ButtonRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetButtonState() throws Exception {
        System.out.println("getButtonState");
        ButtonState buttonState = ButtonState.newBuilder().setValue(ButtonState.State.PRESSED).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId()).applyServiceState(buttonState, ServiceType.BUTTON_STATE_SERVICE);
        buttonRemote.requestData().get();
        assertEquals("The getter for the button returns the wrong value!", buttonState.getValue(), buttonRemote.getButtonState().getValue());
    }

    /**
     * Test if the timestamp in the buttonState is set correctly.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetButtonStateTimestamp() throws Exception {
        System.out.println("testGetButtonStateTimestamp");
        long timestamp;
        Stopwatch stopwatch = new Stopwatch();

        stopwatch.start();
        ButtonState buttonState = ButtonState.newBuilder().setValue(ButtonState.State.DOUBLE_PRESSED).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId()).applyServiceState(buttonState, ServiceType.BUTTON_STATE_SERVICE);
        stopwatch.stop();
        buttonRemote.requestData().get();
        assertEquals("The getter for the button returns the wrong value!", buttonState.getValue(), buttonRemote.getButtonState().getValue());
        timestamp = TimestampJavaTimeTransform.transform(Services.getLatestValueOccurrence(State.DOUBLE_PRESSED, buttonRemote.getButtonState()));
        assertTrue("The timestamp of the button state has not been updated!", (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));

        // just to be safe that the next test does not set the motion state in the same millisecond
        Thread.sleep(1);

        stopwatch.start();
        buttonState = ButtonState.newBuilder().setValue(ButtonState.State.PRESSED).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId()).applyServiceState(buttonState, ServiceType.BUTTON_STATE_SERVICE);
        stopwatch.stop();
        buttonRemote.requestData().get();
        assertEquals("The getter for the button returns the wrong value!", buttonState.getValue(), buttonRemote.getButtonState().getValue());
        timestamp = TimestampJavaTimeTransform.transform(Services.getLatestValueOccurrence(State.PRESSED, buttonRemote.getButtonState()));
        assertTrue("The timestamp of the button state has not been updated!", (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));

        // just to be safe that the next test does not set the motion state in the same millisecond
        Thread.sleep(1);

        stopwatch.start();
        buttonState = ButtonState.newBuilder().setValue(ButtonState.State.RELEASED).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId()).applyServiceState(buttonState, ServiceType.BUTTON_STATE_SERVICE);
        stopwatch.stop();
        buttonRemote.requestData().get();
        assertEquals("The getter for the button returns the wrong value!", buttonState.getValue(), buttonRemote.getButtonState().getValue());
        timestamp = TimestampJavaTimeTransform.transform(Services.getLatestValueOccurrence(State.PRESSED, buttonRemote.getButtonState()));
        assertFalse("The timestamp of the button state has been updated even though it should not!", (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));
    }
}
