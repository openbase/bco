/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.fibaro.F_MotionSensorController;
import de.citec.dal.hal.device.gira.GI_5142Controller;
import de.citec.dal.hal.unit.ButtonController;
import de.citec.dal.util.DALRegistry;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.VerificationFailedException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.states.ClickType;

/**
 *
 * @author thuxohl
 */
public class ButtonRemoteTest {

    private static final Location LOCATION = new Location("paradise");
    private static final String LABEL = "Button_Unit_Test";
    private static final String[] BUTTONS = {"Button_1", "Button_2", "Button_3", "Button_4"};

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ButtonRemoteTest.class);

    private ButtonRemote buttonRemote;
    private DALService dalService;

    public ButtonRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService(new ButtonRemoteTest.DeviceInitializerImpl());
        dalService.activate();

        buttonRemote = new ButtonRemote();
        buttonRemote.init(BUTTONS[0], LOCATION);
        buttonRemote.activate();
    }

    @After
    public void tearDown() {
        dalService.deactivate();
        try {
            buttonRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate button remote: ", ex);
        }
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
    @Test(timeout = 3000)
    public void testGetButtonState() throws Exception {
        System.out.println("getButtonState");
        ClickType.Click.ClickState state = ClickType.Click.ClickState.DOUBLE_CLICKED;
        ((ButtonController) dalService.getRegistry().getUnits(ButtonController.class).iterator().next()).updateButtonState(state);
        while (!buttonRemote.getButtonState().equals(state)) {
            Thread.yield();
        }
        assertTrue("The getter for the button returns the wrong value!", buttonRemote.getButtonState().equals(state));
    }

    public static class DeviceInitializerImpl implements de.citec.dal.util.DeviceInitializer {

        @Override
        public void initDevices(final DALRegistry registry) {

            try {
                registry.register(new GI_5142Controller("GI_5142_000", LABEL, BUTTONS, LOCATION));
            } catch (de.citec.jul.exception.InstantiationException | VerificationFailedException ex) {
                logger.warn("Could not initialize unit test device!", ex);
            }
        }
    }
}
