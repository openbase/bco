/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.dal.hal;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import de.citec.dal.hal.device.plugwise.PW_PowerPlugController;
import de.citec.dal.hal.service.ServiceFactory;

/**
 *
 * @author nuc
 */
public class AbstractHardwareControllerTest {
    
    public AbstractHardwareControllerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of parseHardwareId method, of class AbstractHardwareController.
     */
    @Test
    public void testParseHardwareId() throws Exception {
        System.out.println("parseHardwareId");
        String id = "PW_PowerPlug_014";
        Class<? extends AbstractDeviceController> hardware = PW_PowerPlugController.class;
        String expResult = "PW_PowerPlug";
        String result = AbstractDeviceController.parseDeviceId(id, hardware);
        assertEquals(expResult, result);
    }

    /**
     * Test of parseInstanceId method, of class AbstractHardwareController.
     */
    @Test
    public void testParseInstanceId() throws Exception {
        System.out.println("parseInstanceId");
        String id = "PW_PowerPlug_014";
        String expResult = "014";
        String result = AbstractDeviceController.parseInstanceId(id);
        assertEquals(expResult, result);
    }

    /**
     * Test of register method, of class AbstractHardwareController.
     */
    @Test
    public void testRegister() {
//        System.out.println("register");
//        AbstractHALController hardware = null;
//        AbstractHardwareController instance = null;
//        instance.register(hardware);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of getLocation method, of class AbstractHardwareController.
     */
    @Test
    public void testGetLocation() {
//        System.out.println("getLocation");
//        AbstractHardwareController instance = null;
//        Location expResult = null;
//        Location result = instance.getLocation();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of internalReceiveUpdate method, of class AbstractHardwareController.
     */
    @Test
    public void testInternalReceiveUpdate() {
//        System.out.println("internalReceiveUpdate");
//        String itemName = "";
//        State newState = null;
//        AbstractHardwareController instance = null;
//        instance.internalReceiveUpdate(itemName, newState);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of getId method, of class AbstractHardwareController.
     */
    @Test
    public void testGetId() {
//        System.out.println("getId");
//        AbstractHardwareController instance = null;
//        String expResult = "";
//        String result = instance.getId();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of activate method, of class AbstractHardwareController.
     */
    @Test
    public void testActivate() throws Exception {
//        System.out.println("activate");
//        AbstractHardwareController instance = null;
//        instance.activate();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of deactivate method, of class AbstractHardwareController.
     */
    @Test
    public void testDeactivate() throws Exception {
//        System.out.println("deactivate");
//        AbstractHardwareController instance = null;
//        instance.deactivate();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of registerMethods method, of class AbstractHardwareController.
     */
    @Test
    public void testRegisterMethods() {
//        System.out.println("registerMethods");
//        LocalServer server = null;
//        AbstractHardwareController instance = null;
//        instance.registerMethods(server);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of postCommand method, of class AbstractHardwareController.
     */
    @Test
    public void testPostCommand() throws Exception {
//        System.out.println("postCommand");
//        String itemName = "";
//        Command command = null;
//        AbstractHardwareController instance = null;
//        instance.postCommand(itemName, command);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of sendCommand method, of class AbstractHardwareController.
     */
    @Test
    public void testSendCommand() throws Exception {
//        System.out.println("sendCommand");
//        String itemName = "";
//        Command command = null;
//        AbstractHardwareController instance = null;
//        instance.sendCommand(itemName, command);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of getHardware_id method, of class AbstractHardwareController.
     */
    @Test
    public void testGetHardware_id() {
//        System.out.println("getHardware_id");
//        AbstractHardwareController instance = null;
//        String expResult = "";
//        String result = instance.getHardware_id();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of getInstance_id method, of class AbstractHardwareController.
     */
    @Test
    public void testGetInstance_id() {
//        System.out.println("getInstance_id");
//        AbstractHardwareController instance = null;
//        String expResult = "";
//        String result = instance.getInstance_id();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of initHardwareMapping method, of class AbstractHardwareController.
     */
    @Test
    public void testInitHardwareMapping() throws Exception {
//        System.out.println("initHardwareMapping");
//        AbstractHardwareController instance = null;
//        instance.initHardwareMapping();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    public class AbstractHardwareControllerImpl extends AbstractDeviceController {

        public AbstractHardwareControllerImpl() throws Exception {
            super("", null, null, null);
        }

        public void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        }

        @Override
        public ServiceFactory getDefaultServiceFactory() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
    
}
