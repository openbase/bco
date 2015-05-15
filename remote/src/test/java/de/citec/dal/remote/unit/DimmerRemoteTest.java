/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.registry.MockRegistry;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.DimmerController;
import de.citec.dal.registry.MockFactory;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InvalidStateException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author thuxohl
 */
public class DimmerRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DimmerRemoteTest.class);

    private static DimmerRemote dimmerRemote;
    private static DALService dalService;
    private static MockRegistry registry;
    private static Location location;

    public DimmerRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, de.citec.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockFactory.newMockRegistry();

        dalService = new DALService();
        dalService.init();
        dalService.activate();

        location = new Location(registry.getLocation());

        dimmerRemote = new DimmerRemote();
        dimmerRemote.init(MockRegistry.DIMMER_LABEL, location);
        dimmerRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (dalService != null) {
            dalService.shutdown();
        }
        if (dimmerRemote != null) {
            dimmerRemote.shutdown();
        }
        if (registry != null) {
            MockFactory.shutdownMockRegistry();
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
    @Test
    public void testSetPower() throws Exception {
        System.out.println("setPowerState");
        PowerState.State state = PowerState.State.ON;
        dimmerRemote.setPower(state);
        dimmerRemote.requestStatus();
        assertEquals("Power has not been set in time!", state, dimmerRemote.getData().getPowerState().getValue());
    }

    /**
     * Test of getPower method, of class DimmerRemote.
     */
    @Test
    public void testGetPower() throws Exception {
        System.out.println("getPowerState");
        PowerState.State state = PowerState.State.OFF;
        ((DimmerController) dalService.getUnitRegistry().get(dimmerRemote.getId())).updatePower(state);
        dimmerRemote.requestStatus();
        assertEquals("Power has not been set in time!", state, dimmerRemote.getPower().getValue());
    }

    /**
     * Test of setDimm method, of class DimmerRemote.
     */
    @Test
    public void testSetDimm() throws Exception {
        System.out.println("setDimm");
        Double dimm = 66d;
        dimmerRemote.setDim(dimm);
        dimmerRemote.requestStatus();
        assertEquals("Dimm has not been set in time!", dimm, dimmerRemote.getData().getValue(), 0.1);
    }

    /**
     * Test of getDimm method, of class DimmerRemote.
     */
    @Test
    public void testGetDimm() throws Exception {
        System.out.println("getDimm");
        Double dimm = 70.0d;
        ((DimmerController) dalService.getUnitRegistry().get(dimmerRemote.getId())).updateDim(dimm);
        dimmerRemote.requestStatus();
        assertEquals("Dimm has not been set in time!", dimm, dimmerRemote.getDim());
    }
}
