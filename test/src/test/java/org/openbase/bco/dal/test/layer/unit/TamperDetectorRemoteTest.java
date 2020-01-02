package org.openbase.bco.dal.test.layer.unit;

/*
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.junit.*;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.remote.layer.unit.TamperDetectorRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.extension.type.processing.TimestampJavaTimeTransform;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.schedule.Stopwatch;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.TamperStateType.TamperState;
import org.openbase.type.domotic.state.TamperStateType.TamperState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TamperDetectorRemoteTest extends AbstractBCODeviceManagerTest {

    private static TamperDetectorRemote tamperDetectorRemote;

    public TamperDetectorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        tamperDetectorRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.TAMPER_DETECTOR), true, TamperDetectorRemote.class);
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
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(tamperDetectorRemote.getId()).applyServiceState(tamperState, ServiceType.TAMPER_STATE_SERVICE);
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
        TamperState tamperState = TamperState.newBuilder().setValue(TamperState.State.TAMPER).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(tamperDetectorRemote.getId()).applyServiceState(tamperState, ServiceType.TAMPER_STATE_SERVICE);
        stopwatch.stop();
        tamperDetectorRemote.requestData().get();
        assertEquals("The getter for the tamper switch state returns the wrong value!", tamperState.getValue(), tamperDetectorRemote.getTamperState().getValue());
        timestamp = TimestampJavaTimeTransform.transform(Services.getLatestValueOccurrence(State.TAMPER, tamperDetectorRemote.getTamperState()));
        String comparision = "Timestamp: " + timestamp + ", interval: [" + stopwatch.getStartTime() + ", " + stopwatch.getEndTime() + "]";
        assertTrue("The last detection timestamp has not been updated! " + comparision, (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));

        // just to be safe that the next test does not set the motion state in the same millisecond
        Thread.sleep(1);

        stopwatch.start();
        tamperState = TamperState.newBuilder().setValue(TamperState.State.NO_TAMPER).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(tamperDetectorRemote.getId()).applyServiceState(tamperState, ServiceType.TAMPER_STATE_SERVICE);
        stopwatch.stop();
        tamperDetectorRemote.requestData().get();
        assertEquals("The getter for the tamper switch state returns the wrong value!", tamperState.getValue(), tamperDetectorRemote.getTamperState().getValue());
        timestamp = TimestampJavaTimeTransform.transform(Services.getLatestValueOccurrence(State.TAMPER, tamperDetectorRemote.getTamperState()));
        comparision = "Timestamp: " + timestamp + ", interval: [" + stopwatch.getStartTime() + ", " + stopwatch.getEndTime() + "]";
        assertFalse("The last detection timestamp has been updated even though it sould not! " + comparision, (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));
    }
}
