/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.registry.MockRegistry;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.ButtonController;
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

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ButtonRemoteTest.class);

    private static ButtonRemote buttonRemote;
    private static DALService dalService;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public ButtonRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, de.citec.jul.exception.InstantiationException, CouldNotPerformException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = new MockRegistry();
        
        dalService = new DALService();
        dalService.init();
        dalService.activate();

        location = new Location(registry.getLocation());
        label = MockRegistry.BUTTON_LABEL;

        buttonRemote = new ButtonRemote();
        buttonRemote.init(label, location);
        buttonRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        dalService.shutdown();
        try {
            buttonRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate button remote: ", ex);
        }
        registry.shutdown();
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
        ((ButtonController) dalService.getUnitRegistry().get(buttonRemote.getId())).updateButton(state);
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
}
