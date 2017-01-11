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
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.layer.unit.ButtonController;
import org.openbase.bco.dal.remote.unit.ButtonRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Remote;
import org.openbase.jul.schedule.Stopwatch;
import org.slf4j.LoggerFactory;
import rst.domotic.state.ButtonStateType.ButtonState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ButtonRemoteTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ButtonRemoteTest.class);

    private static ButtonRemote buttonRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static String label;

    public ButtonRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();
            JPService.registerProperty(JPHardwareSimulationMode.class, true);
            registry = MockRegistryHolder.newMockRegistry();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();
            deviceManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);

            label = MockRegistry.BUTTON_LABEL;

            buttonRemote = new ButtonRemote();
            buttonRemote.initByLabel(label);
            buttonRemote.activate();
            buttonRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED);
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
            if (buttonRemote != null) {
                buttonRemote.shutdown();
            }
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
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
        LOGGER.debug("getButtonState");
        ButtonState buttonState = ButtonState.newBuilder().setValue(ButtonState.State.PRESSED).build();
        ((ButtonController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId())).updateButtonStateProvider(buttonState);
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
        LOGGER.debug("testGetButtonStateTimestamp");
        long timestamp;
        ButtonState buttonState = ButtonState.newBuilder().setValue(ButtonState.State.DOUBLE_PRESSED).build();
        Stopwatch stopwatch = new Stopwatch();

        stopwatch.start();
        ((ButtonController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId())).updateButtonStateProvider(buttonState);
        stopwatch.stop();
        buttonRemote.requestData().get();
        assertEquals("The getter for the button returns the wrong value!", buttonState.getValue(), buttonRemote.getButtonState().getValue());
        timestamp = buttonRemote.getButtonState().getLastPressed().getTime();
        assertTrue("The timestamp of the button state has not been updated!", (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));

        // just to be safe that the next test does not set the motion state in the same millisecond 
        Thread.sleep(1);

        buttonState = ButtonState.newBuilder().setValue(ButtonState.State.PRESSED).build();
        stopwatch.start();
        ((ButtonController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId())).updateButtonStateProvider(buttonState);
        stopwatch.stop();
        buttonRemote.requestData().get();
        assertEquals("The getter for the button returns the wrong value!", buttonState.getValue(), buttonRemote.getButtonState().getValue());
        timestamp = buttonRemote.getButtonState().getLastPressed().getTime();
        assertTrue("The timestamp of the button state has not been updated!", (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));

        // just to be safe that the next test does not set the motion state in the same millisecond 
        Thread.sleep(1);

        buttonState = ButtonState.newBuilder().setValue(ButtonState.State.RELEASED).build();
        stopwatch.start();
        ((ButtonController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(buttonRemote.getId())).updateButtonStateProvider(buttonState);
        stopwatch.stop();
        buttonRemote.requestData().get();
        assertEquals("The getter for the button returns the wrong value!", buttonState.getValue(), buttonRemote.getButtonState().getValue());
        timestamp = buttonRemote.getButtonState().getLastPressed().getTime();
        assertFalse("The timestamp of the button state has been updated even though it sould not!", (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));
    }
}
