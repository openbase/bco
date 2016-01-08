/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.test.remote.unit;

import org.dc.bco.manager.device.core.DeviceManagerLauncher;
import org.dc.bco.dal.lib.data.Location;
import org.dc.bco.dal.lib.layer.unit.ButtonController;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.dc.jps.core.JPService;
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.dal.remote.unit.ButtonRemote;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.ButtonStateType.ButtonState;

/**
 *
 * @author thuxohl
 */
public class ButtonRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ButtonRemoteTest.class);

    private static ButtonRemote buttonRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public ButtonRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, org.dc.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();
        

        location = new Location(registry.getLocation());
        label = MockRegistry.BUTTON_LABEL;

        buttonRemote = new ButtonRemote();
        buttonRemote.init(label, location);
        buttonRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (buttonRemote != null) {
            buttonRemote.shutdown();
        }
        if (registry != null) {
            MockRegistryHolder.shutdownMockRegistry();
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
    @Test(timeout = 60000)
    public void testGetButtonState() throws Exception {
        logger.debug("getButtonState");
        ButtonState buttonState = ButtonState.newBuilder().setValue(ButtonState.State.CLICKED).build();
        ((ButtonController) deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().get(buttonRemote.getId())).updateButton(buttonState);
        buttonRemote.requestStatus();
        assertEquals("The getter for the button returns the wrong value!", buttonState.getValue(), buttonRemote.getButton().getValue());
    }
}
