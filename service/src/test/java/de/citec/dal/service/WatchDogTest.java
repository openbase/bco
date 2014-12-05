/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import de.citec.dal.service.rsb.WatchDog;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import rsb.Activatable;
import rsb.RSBException;

/**
 *
 * @author mpohling
 */
public class WatchDogTest {
    
    public WatchDogTest() {
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
     * Test of activate method, of class WatchDog.
     */
    @Test
    public void testActivate() throws Exception {
        System.out.println("isActive");
        WatchDog instance = new WatchDog(new TestService(), "TestService");
        boolean expResult = true;
        instance.activate();
        boolean result = instance.isActive();
        assertEquals(expResult, result);
    }

    /**
     * Test of deactivate method, of class WatchDog.
     */
    @Test
    public void testDeactivate() throws Exception {
        System.out.println("deactivate");
         WatchDog instance = new WatchDog(new TestService(), "TestService");
        boolean expResult = false;
        instance.activate();
        instance.deactivate();
        boolean result = instance.isActive();        
        assertEquals(expResult, result);
    }

    /**
     * Test of isActive method, of class WatchDog.
     */
    @Test
    public void testIsActive() throws RSBException, InterruptedException {
        System.out.println("isActive");
        WatchDog instance = new WatchDog(new TestService(), "TestService");
        assertEquals(instance.isActive(), false);
        instance.activate();
        assertEquals(instance.isActive(), true);
        instance.deactivate();
        assertEquals(instance.isActive(), false);
    }
    
    /**
     * Test of service error handling.
     */
    @Test
    public void testServiceErrorHandling() throws RSBException, InterruptedException {
        System.out.println("isActive");
        WatchDog instance = new WatchDog(new TestService(), "TestService");
        assertEquals(instance.isActive(), false);
        instance.activate();
        assertEquals(instance.isActive(), true);
        instance.deactivate();
        assertEquals(instance.isActive(), false);
    }
    
    class TestService implements Activatable {

        private boolean active;
        
        @Override
        public void activate() throws RSBException {
            active = true;
        }

        @Override
        public void deactivate() throws RSBException, InterruptedException {
            active = false;
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
