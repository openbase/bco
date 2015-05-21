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
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.MotionStateType;

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
    
    @Test
    public void testProtobuf() {
        MotionStateType.MotionState.Builder builder = MotionStateType.MotionState.newBuilder();
        builder.setValue(MotionStateType.MotionState.State.UNKNOWN);
        Assert.assertTrue(builder.getValue() == MotionStateType.MotionState.State.UNKNOWN);
        Assert.assertTrue(builder.build().getValue() == MotionStateType.MotionState.State.UNKNOWN);
        builder.setValue(MotionStateType.MotionState.State.MOVEMENT);
        Assert.assertTrue(builder.getValue() == MotionStateType.MotionState.State.MOVEMENT);
        Assert.assertTrue(builder.build().getValue() == MotionStateType.MotionState.State.MOVEMENT);
        builder.build();
        
        MotionStateType.MotionState.Builder clone = builder.clone();
        
        builder.setValue(MotionStateType.MotionState.State.NO_MOVEMENT);
        Assert.assertTrue(builder.getValue() == MotionStateType.MotionState.State.NO_MOVEMENT);
        Assert.assertTrue(builder.build().getValue() == MotionStateType.MotionState.State.NO_MOVEMENT);
        Assert.assertTrue(clone.getValue() == MotionStateType.MotionState.State.MOVEMENT);
        Assert.assertTrue(clone.build().getValue() == MotionStateType.MotionState.State.MOVEMENT);
        clone.setValue(MotionStateType.MotionState.State.UNKNOWN);
        Assert.assertTrue(builder.getValue() == MotionStateType.MotionState.State.NO_MOVEMENT);
        Assert.assertTrue(builder.build().getValue() == MotionStateType.MotionState.State.NO_MOVEMENT);
        Assert.assertTrue(clone.getValue() == MotionStateType.MotionState.State.UNKNOWN);
        Assert.assertTrue(clone.build().getValue() == MotionStateType.MotionState.State.UNKNOWN);
        
        
    }

}
