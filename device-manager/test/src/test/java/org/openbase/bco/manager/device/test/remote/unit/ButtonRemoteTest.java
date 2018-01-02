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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.unit.ButtonController;
import org.openbase.bco.dal.remote.unit.ButtonRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.extension.rst.processing.TimestampJavaTimeTransform;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.schedule.Stopwatch;
import rst.domotic.state.ButtonStateType.ButtonState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ButtonRemoteTest extends AbstractBCODeviceManagerTest {

    private static ButtonRemote buttonRemote;

    public ButtonRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        buttonRemote = Units.getUnitsByLabel(MockRegistry.BUTTON_LABEL, true, ButtonRemote.class).get(0);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
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
        ButtonState buttonState = TimestampProcessor.updateTimestampWithCurrentTime(ButtonState.newBuilder().setValue(ButtonState.State.PRESSED)).build();
        ((ButtonController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId())).applyDataUpdate(buttonState);
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
        ButtonState buttonState = TimestampProcessor.updateTimestampWithCurrentTime(ButtonState.newBuilder().setValue(ButtonState.State.DOUBLE_PRESSED)).build();
        ((ButtonController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId())).applyDataUpdate(buttonState);
        stopwatch.stop();
        buttonRemote.requestData().get();
        assertEquals("The getter for the button returns the wrong value!", buttonState.getValue(), buttonRemote.getButtonState().getValue());
        timestamp = TimestampJavaTimeTransform.transform(buttonRemote.getButtonState().getLastPressed());
        assertTrue("The timestamp of the button state has not been updated!", (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));

        // just to be safe that the next test does not set the motion state in the same millisecond 
        Thread.sleep(1);

        stopwatch.start();
        buttonState = TimestampProcessor.updateTimestampWithCurrentTime(ButtonState.newBuilder().setValue(ButtonState.State.PRESSED)).build();
        ((ButtonController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId())).applyDataUpdate(buttonState);
        stopwatch.stop();
        buttonRemote.requestData().get();
        assertEquals("The getter for the button returns the wrong value!", buttonState.getValue(), buttonRemote.getButtonState().getValue());
        timestamp = TimestampJavaTimeTransform.transform(buttonRemote.getButtonState().getLastPressed());
        assertTrue("The timestamp of the button state has not been updated!", (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));

        // just to be safe that the next test does not set the motion state in the same millisecond 
        Thread.sleep(1);

        stopwatch.start();
        buttonState = TimestampProcessor.updateTimestampWithCurrentTime(ButtonState.newBuilder().setValue(ButtonState.State.RELEASED)).build();
        ((ButtonController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId())).applyDataUpdate(buttonState);
        stopwatch.stop();
        buttonRemote.requestData().get();
        assertEquals("The getter for the button returns the wrong value!", buttonState.getValue(), buttonRemote.getButtonState().getValue());
        timestamp = TimestampJavaTimeTransform.transform(buttonRemote.getButtonState().getLastPressed());
        assertFalse("The timestamp of the button state has been updated even though it sould not!", (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));
    }
}
