package org.openbase.bco.manager.device.test.remote.unit;

/*
 * #%L
 * COMA DeviceManager Test
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.layer.unit.TamperDetectorController;
import org.openbase.bco.dal.remote.unit.TamperDetectorRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Remote;
import org.openbase.jul.schedule.Stopwatch;
import org.slf4j.LoggerFactory;
import rst.domotic.state.TamperStateType.TamperState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TamperDetectorRemoteTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TamperDetectorRemoteTest.class);

    private static TamperDetectorRemote tamperDetectorRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static String label;

    public TamperDetectorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();
            JPService.registerProperty(JPHardwareSimulationMode.class, true);
            registry = MockRegistryHolder.newMockRegistry();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();
            deviceManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);

            label = MockRegistry.TAMPER_DETECTOR_LABEL;

            tamperDetectorRemote = new TamperDetectorRemote();
            tamperDetectorRemote.initByLabel(label);
            tamperDetectorRemote.activate();
            tamperDetectorRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED);
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }
            if (tamperDetectorRemote != null) {
                tamperDetectorRemote.shutdown();
            }
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of notifyUpdated method, of class TamperSwtichRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getTamperState method, of class TamperSwtichRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetTamperState() throws Exception {
        System.out.println("getTamperState");
        TamperState tamperState = TamperState.newBuilder().setValue(TamperState.State.TAMPER).build();
        ((TamperDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(tamperDetectorRemote.getId())).updateTamperStateProvider(tamperState);
        tamperDetectorRemote.requestData().get();
        assertEquals("The getter for the tamper switch state returns the wrong value!", tamperState.getValue(), tamperDetectorRemote.getTamperState().getValue());
    }

    /**
     * Test if the timestamp in the tamperState is set correctly.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetTamperStateTimestamp() throws Exception {
        LOGGER.debug("testGetTamperStateTimestamp");
        long timestamp;
        TamperState tamperState = TamperState.newBuilder().setValue(TamperState.State.TAMPER).build();
        Stopwatch stopwatch = new Stopwatch();

        stopwatch.start();
        ((TamperDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(tamperDetectorRemote.getId())).updateTamperStateProvider(tamperState);
        stopwatch.stop();
        tamperDetectorRemote.requestData().get();
        assertEquals("The getter for the tamper switch state returns the wrong value!", tamperState.getValue(), tamperDetectorRemote.getTamperState().getValue());
        timestamp = tamperDetectorRemote.getTamperState().getLastDetection().getTime();
        String comparision = "Timestamp: " + timestamp + ", interval: [" + stopwatch.getStartTime() + ", " + stopwatch.getEndTime() + "]";
        assertTrue("The last detection timestamp has not been updated! " + comparision, (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));

        // just to be safe that the next test does not set the motion state in the same millisecond 
        Thread.sleep(1);

        tamperState = TamperState.newBuilder().setValue(TamperState.State.NO_TAMPER).build();
        stopwatch.start();
        ((TamperDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(tamperDetectorRemote.getId())).updateTamperStateProvider(tamperState);
        stopwatch.stop();
        tamperDetectorRemote.requestData().get();
        assertEquals("The getter for the tamper switch state returns the wrong value!", tamperState.getValue(), tamperDetectorRemote.getTamperState().getValue());
        timestamp = tamperDetectorRemote.getTamperState().getLastDetection().getTime();
        comparision = "Timestamp: " + timestamp + ", interval: [" + stopwatch.getStartTime() + ", " + stopwatch.getEndTime() + "]";
        assertFalse("The last detection timestamp has been updated even though it sould not! " + comparision, (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));
    }
}
