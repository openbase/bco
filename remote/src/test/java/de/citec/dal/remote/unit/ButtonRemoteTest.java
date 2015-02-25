/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.gira.GI_5142Controller;
import de.citec.dal.hal.unit.ButtonController;
import de.citec.dal.registry.UnitRegistry;
import de.citec.dal.registry.DeviceRegistry;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.NotAvailableException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.ClickType;

/**
 *
 * @author thuxohl
 */
public class ButtonRemoteTest {

    private static final Location LOCATION = new Location("paradise");
    public static final String LABEL = "Button_Unit_Test";
    public static final String[] BUTTONS = {"Button_1", "Button_2", "Button_3", "Button_4"};

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ButtonRemoteTest.class);

    private static ButtonRemote buttonRemote;
    private static DALService dalService;

    public ButtonRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService(new DeviceInitializerImpl());
        dalService.activate();

        buttonRemote = new ButtonRemote();
        buttonRemote.init(BUTTONS[0], LOCATION);
        buttonRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException {
//        dalService.shutdown();
//        try {
//            buttonRemote.deactivate();
//        } catch (InterruptedException ex) {
//            logger.warn("Could not deactivate button remote: ", ex);
//        }
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
    @Test(timeout = 3000)
    public void testGetButtonState() throws Exception {
        logger.debug("getButtonState");
        ClickType.Click.ClickState state = ClickType.Click.ClickState.DOUBLE_CLICKED;
        ((ButtonController) dalService.getUnitRegistry().getUnit(BUTTONS[0], LOCATION, ButtonController.class)).updateButton(state);
        while (true) {
            try {
                if (buttonRemote.getButton().equals(state)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("The getter for the button returns the wrong value!", buttonRemote.getButton().equals(state));
    }

    public static class DeviceInitializerImpl implements de.citec.dal.util.DeviceInitializer {

        @Override
        public void initDevices(final DeviceRegistry registry) {
            try {
                registry.register(new GI_5142Controller(LABEL, BUTTONS, LOCATION));
            } catch (CouldNotPerformException ex) {
                logger.warn("Could not initialize unit test device!", ex);
            }
        }
    }
}
