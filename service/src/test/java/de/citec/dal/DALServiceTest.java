/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.registry.MockRegistry;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class DALServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(DALServiceTest.class);

    private static MockRegistry registry;

    public DALServiceTest() {

    }

    @BeforeClass
    public static void setUpClass() throws InstantiationException {
        registry = new MockRegistry();
    }

    @AfterClass
    public static void tearDownClass() {
        if (registry != null) {
            registry.shutdown();
        }
    }

    @Before
    public void setUp() throws InitializationException, InstantiationException {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of deactivate method, of class DALService.
     */
    @Test
    public void testDeactivate() throws InitializationException, InstantiationException, CouldNotPerformException, Exception {
        System.out.println("deactivate");
        DALService instance = new DALService();
        try {
            instance.init();
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistory(logger, ex);
        }
        instance.activate();
        instance.deactivate();
    }

}
