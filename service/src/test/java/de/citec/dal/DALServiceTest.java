/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.util.ConnectionManager;
import de.citec.dal.util.DALRegistry;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mpohling
 */
public class DALServiceTest {
    
    private DALService dALService;
    
    public DALServiceTest() {
        
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        dALService = new DALService();
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of activate method, of class DALService.
     */
    @Test
    public void testActivate() {
        System.out.println("activate");
        DALService instance = new DALService();
        instance.activate();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of deactivate method, of class DALService.
     */
    @Test
    public void testDeactivate() {
        System.out.println("deactivate");
        DALService instance = new DALService();
        instance.deactivate();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getRegistry method, of class DALService.
     */
    @Test
    public void testGetRegistry() {
        System.out.println("getRegistry");
        DALService instance = new DALService();
        DALRegistry expResult = null;
        DALRegistry result = instance.getRegistry();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getHardwareManager method, of class DALService.
     */
    @Test
    public void testGetHardwareManager() {
        System.out.println("getHardwareManager");
        DALService instance = new DALService();
        ConnectionManager expResult = null;
        ConnectionManager result = instance.getHardwareManager();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of main method, of class DALService.
     */
    @Test
    public void testMain() {
        System.out.println("main");
        String[] args = null;
        DALService.main(args);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
