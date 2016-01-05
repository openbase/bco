package org.dc.bco.coma.dem.test;


import org.dc.bco.coma.dem.core.DeviceManagerLauncher;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.jps.exception.JPServiceException;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.MotionStateType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class LauncherTest {

    private static final Logger logger = LoggerFactory.getLogger(LauncherTest.class);

    private static MockRegistry registry;

    public LauncherTest() {
    }

    @BeforeClass
    public static void setUpClass() throws org.dc.jul.exception.InstantiationException, JPServiceException {
        registry = new MockRegistry();
    }

    @AfterClass
    public static void tearDownClass() {
        if (registry != null) {
            registry.shutdown();
        }
    }

    @Before
    public void setUp() throws InitializationException, org.dc.jul.exception.InstantiationException {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of deactivate method, of class DALService.
     */
    @Test
    public void testShutdown() throws InitializationException, org.dc.jul.exception.InstantiationException, CouldNotPerformException, Exception {
        System.out.println("deactivate");
        DeviceManagerLauncher instance = new DeviceManagerLauncher();
        try {
            instance.init();
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        instance.shutdown();
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
