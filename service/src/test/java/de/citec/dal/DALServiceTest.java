/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.util.ConnectionManager;
import de.citec.dal.registry.DeviceRegistry;
import de.citec.dal.registry.MockRegistry;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
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
    
    private static MockRegistry registry;
    public DALServiceTest() {
        
    }
    
    @BeforeClass
    public static void setUpClass() throws InstantiationException {
        registry = new MockRegistry();
    }
    
    @AfterClass
    public static void tearDownClass() {
        registry.shutdown();
    }
    
    @Before
    public void setUp() throws InitializationException, InstantiationException {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of activate method, of class DALService.
     */
    @Test
    public void testActivate() throws InitializationException, InstantiationException {
        System.out.println("activate");
        DALService instance = new DALService();
        instance.activate();
    }

    /**
     * Test of deactivate method, of class DALService.
     */
    @Test
    public void testDeactivate() throws InitializationException, InstantiationException {
        System.out.println("deactivate");
        DALService instance = new DALService();
        instance.activate();
        instance.deactivate();
    }

    /**
     * Test of getRegistry method, of class DALService.
     * @throws de.citec.jul.exception.InitializationException
     */
    @Test
    public void testGetRegistry() throws InitializationException, InstantiationException {
        System.out.println("getRegistry");
        DALService instance = new DALService();
        DeviceRegistry expResult = null;
        DeviceRegistry result = instance.getDeviceRegistry();
        assertEquals(expResult, result);
    }

    /**
     * Test of getHardwareManager method, of class DALService.
     */
    @Test
    public void testGetHardwareManager() throws InitializationException, InstantiationException {
        System.out.println("getHardwareManager");
        DALService instance = new DALService();
        ConnectionManager expResult = null;
        ConnectionManager result = instance.getConnectionManager();
        assertEquals(expResult, result);
    }

    /**
     * Test of main method, of class DALService.
     */
    @Test
    public void testMain() throws Throwable {
        System.out.println("main");
        String[] args = null;
        DALService.main(args);
    }
    
}
