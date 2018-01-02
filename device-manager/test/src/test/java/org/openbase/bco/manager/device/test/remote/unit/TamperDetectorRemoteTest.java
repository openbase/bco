package org.openbase.bco.manager.device.test.remote.unit;

/*
 * #%L
 * BCO Manager Device Test
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.unit.TamperDetectorController;
import org.openbase.bco.dal.remote.unit.TamperDetectorRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.extension.rst.processing.TimestampJavaTimeTransform;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.schedule.Stopwatch;
import rst.domotic.state.TamperStateType.TamperState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TamperDetectorRemoteTest extends AbstractBCODeviceManagerTest {

    private static TamperDetectorRemote tamperDetectorRemote;

    public TamperDetectorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        tamperDetectorRemote = Units.getUnitsByLabel(MockRegistry.TAMPER_DETECTOR_LABEL, true, TamperDetectorRemote.class).get(0);
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
        TamperState tamperState = TimestampProcessor.updateTimestampWithCurrentTime(TamperState.newBuilder().setValue(TamperState.State.TAMPER)).build();
        ((TamperDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(tamperDetectorRemote.getId())).applyDataUpdate(tamperState);
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
        System.out.println("testGetTamperStateTimestamp");
        long timestamp;
        Stopwatch stopwatch = new Stopwatch();

        stopwatch.start();
        TamperState tamperState = TimestampProcessor.updateTimestampWithCurrentTime(TamperState.newBuilder().setValue(TamperState.State.TAMPER)).build();
        ((TamperDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(tamperDetectorRemote.getId())).applyDataUpdate(tamperState);
        stopwatch.stop();
        tamperDetectorRemote.requestData().get();
        assertEquals("The getter for the tamper switch state returns the wrong value!", tamperState.getValue(), tamperDetectorRemote.getTamperState().getValue());
        timestamp = TimestampJavaTimeTransform.transform(tamperDetectorRemote.getTamperState().getLastDetection());
        String comparision = "Timestamp: " + timestamp + ", interval: [" + stopwatch.getStartTime() + ", " + stopwatch.getEndTime() + "]";
        assertTrue("The last detection timestamp has not been updated! " + comparision, (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));

        // just to be safe that the next test does not set the motion state in the same millisecond 
        Thread.sleep(1);

        stopwatch.start();
        tamperState = TimestampProcessor.updateTimestampWithCurrentTime(TamperState.newBuilder().setValue(TamperState.State.NO_TAMPER)).build();
        ((TamperDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(tamperDetectorRemote.getId())).applyDataUpdate(tamperState);
        stopwatch.stop();
        tamperDetectorRemote.requestData().get();
        assertEquals("The getter for the tamper switch state returns the wrong value!", tamperState.getValue(), tamperDetectorRemote.getTamperState().getValue());
        timestamp = TimestampJavaTimeTransform.transform(tamperDetectorRemote.getTamperState().getLastDetection());
        comparision = "Timestamp: " + timestamp + ", interval: [" + stopwatch.getStartTime() + ", " + stopwatch.getEndTime() + "]";
        assertFalse("The last detection timestamp has been updated even though it sould not! " + comparision, (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));
    }
}
