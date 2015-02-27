/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.hager.HA_TYA663AController;
import de.citec.dal.hal.unit.DimmerController;
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
import rst.homeautomation.state.PowerType;

/**
 *
 * @author thuxohl
 */
public class DimmerRemoteTest {

    public static final String LABEL = "Dimmer_Unit_Test";
    public static final String[] UNITS = {"Dimmer_1", "Dimmer_2", "Dimmer_3"};
    public static final Location LOCATION = new Location("paradise");

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DimmerRemoteTest.class);

    private static DimmerRemote dimmerRemote;
    private static DALService dalService;

    public DimmerRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InvalidStateException, InitializationException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService(new DimmerRemoteTest.DeviceInitializerImpl());
        dalService.activate();

        dimmerRemote = new DimmerRemote();
        dimmerRemote.init(UNITS[0], LOCATION);
        dimmerRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException {
        dalService.shutdown();
        try {
            dimmerRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate dimmer remote: ", ex);
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of notifyUpdated method, of class DimmerRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of setPower method, of class DimmerRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testSetPower() throws Exception {
        System.out.println("setPowerState");
        PowerType.Power.PowerState state = PowerType.Power.PowerState.ON;
        dimmerRemote.setPower(state);
        while (true) {
            try {
                if (dimmerRemote.getData().getPowerState().getState().equals(state)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Power has not been set in time!", dimmerRemote.getData().getPowerState().getState().equals(state));
    }

    /**
     * Test of getPower method, of class DimmerRemote.
     */
    @Test(timeout = 3000)
    public void testGetPower() throws Exception {
        System.out.println("getPowerState");
        PowerType.Power.PowerState state = PowerType.Power.PowerState.OFF;
        ((DimmerController) dalService.getUnitRegistry().getUnit(UNITS[0], LOCATION, DimmerController.class)).updatePower(state);
        while (true) {
            try {
                if (dimmerRemote.getPower().equals(state)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Power has not been set in time!", dimmerRemote.getPower().equals(state));
    }

    /**
     * Test of setDimm method, of class DimmerRemote.
     */
    @Test(timeout = 3000)
    public void testSetDimm() throws Exception {
        System.out.println("setDimm");
        Double dimm = 66d;
        dimmerRemote.setDimm(dimm);
        while (true) {
            try {
                if (dimmerRemote.getData().getValue() == dimm) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Dimm has not been set in time!", dimmerRemote.getData().getValue() == dimm);
    }

    /**
     * Test of getDimm method, of class DimmerRemote.
     */
    @Test(timeout = 3000)
    public void testGetDimm() throws Exception {
        System.out.println("getDimm");
        Double dimm = 70.0D;
        ((DimmerController) dalService.getUnitRegistry().getUnit(UNITS[0], LOCATION, DimmerController.class)).updateDimm(dimm);
        while (true) {
            try {
                if (dimmerRemote.getDimm().equals(dimm)) {
                    break;
                }
            } catch (NotAvailableException ex) {
                logger.debug("Not ready yet");
            }
            Thread.yield();
        }
        assertTrue("Dimm has not been set in time!", dimmerRemote.getDimm().equals(dimm));
    }

    public static class DeviceInitializerImpl implements de.citec.dal.util.DeviceInitializer {

        @Override
        public void initDevices(final DeviceRegistry registry) {

            try {
                registry.register(new HA_TYA663AController(LABEL, UNITS, LOCATION));
            } catch (CouldNotPerformException ex) {
                logger.warn("Could not initialize unit test device!", ex);
            }
        }
    }
}
