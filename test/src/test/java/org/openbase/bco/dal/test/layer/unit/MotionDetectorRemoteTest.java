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
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.remote.layer.unit.MotionDetectorRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.extension.type.processing.TimestampJavaTimeTransform;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.schedule.Stopwatch;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.MotionStateType.MotionState;
import org.openbase.type.domotic.state.MotionStateType.MotionState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class MotionDetectorRemoteTest extends AbstractBCODeviceManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MotionDetectorRemoteTest.class);

    private static MotionDetectorRemote motionDetectorRemote;

    public MotionDetectorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();
        motionDetectorRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.MOTION_DETECTOR), true, MotionDetectorRemote.class);
    }

    /**
     * Test of getMotionState method, of class MotionSenorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetMotionState() throws Exception {

        System.out.println("getMotionState");
        MotionState motion = MotionState.newBuilder().setValue(MotionState.State.MOTION).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(motionDetectorRemote.getId()).applyServiceState(motion, ServiceType.MOTION_STATE_SERVICE);
        motionDetectorRemote.requestData().get();
        Assert.assertEquals("The getter for the motion state returns the wrong value!", motion.getValue(), motionDetectorRemote.getMotionState().getValue());
    }

    /**
     * Test if the timestamp in the motionState is set correctly.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetMotionStateTimestamp() throws Exception {
        LOGGER.debug("testGetMotionStateTimestamp");
        long timestamp;
        Stopwatch stopwatch = new Stopwatch();

        stopwatch.start();
        MotionState motion = MotionState.newBuilder().setValue(MotionState.State.MOTION).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(motionDetectorRemote.getId()).applyServiceState(motion, ServiceType.MOTION_STATE_SERVICE);
        stopwatch.stop();
        motionDetectorRemote.requestData().get();
        Assert.assertEquals("The getter for the motion state returns the wrong value!", motion.getValue(), motionDetectorRemote.getMotionState().getValue());
        timestamp = TimestampJavaTimeTransform.transform(Services.getLatestValueOccurrence(State.MOTION, motionDetectorRemote.getMotionState()));
        String comparision = "Timestamp: " + timestamp + ", interval: [" + stopwatch.getStartTime() + ", " + stopwatch.getEndTime() + "]";
        assertTrue("The last motion timestamp has not been updated! " + comparision, (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));

        // just to be safe that the next test does not set the motion state in the same millisecond
        Thread.sleep(1);

        stopwatch.start();
        motion = MotionState.newBuilder().setValue(MotionState.State.NO_MOTION).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(motionDetectorRemote.getId()).applyServiceState(motion, ServiceType.MOTION_STATE_SERVICE);
        stopwatch.stop();
        motionDetectorRemote.requestData().get();
        Assert.assertEquals("The getter for the motion state returns the wrong value!", motion.getValue(), motionDetectorRemote.getMotionState().getValue());
        timestamp = TimestampJavaTimeTransform.transform(Services.getLatestValueOccurrence(State.MOTION, motionDetectorRemote.getMotionState()));
        comparision = "Timestamp: " + timestamp + ", interval: [" + stopwatch.getStartTime() + ", " + stopwatch.getEndTime() + "]";
        assertFalse("The last motion timestamp has been updated even though it sould not! " + comparision, (timestamp >= stopwatch.getStartTime() && timestamp <= stopwatch.getEndTime()));
    }
}
