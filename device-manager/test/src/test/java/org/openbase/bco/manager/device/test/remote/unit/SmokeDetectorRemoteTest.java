package org.openbase.bco.manager.device.test.remote.unit;

/*
 * #%L
 * BCO Manager Device Test
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.layer.unit.SmokeDetectorController;
import org.openbase.bco.dal.remote.unit.SmokeDetectorRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Remote;
import org.slf4j.LoggerFactory;
import rst.domotic.state.AlarmStateType.AlarmState;
import rst.domotic.state.SmokeStateType.SmokeState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SmokeDetectorRemoteTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SmokeDetectorRemoteTest.class);

    private static SmokeDetectorRemote smokeDetectorRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static String label;

    public SmokeDetectorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InstantiationException, CouldNotPerformException, InterruptedException, ExecutionException, JPServiceException {
        try {
            JPService.setupJUnitTestMode();
            JPService.registerProperty(JPHardwareSimulationMode.class, true);
            registry = MockRegistryHolder.newMockRegistry();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();
            deviceManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);

            label = MockRegistry.SMOKE_DETECTOR_LABEL;

            smokeDetectorRemote = new SmokeDetectorRemote();
            smokeDetectorRemote.initByLabel(label);
            smokeDetectorRemote.activate();
            smokeDetectorRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED);
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() {
        try {
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }
            if (smokeDetectorRemote != null) {
                smokeDetectorRemote.shutdown();
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
     * Test of notifyUpdated method, of class SmokeDetectorRemote.
     *
     * @throws java.lang.Exception
     */
    @Ignore
    public void testNotifyUpdated() throws Exception {
    }

    /**
     * Test of getSmokeAlarmState method, of class SmokeDetectorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetSmokeAlarmState() throws Exception {
        System.out.println("getSmokeAlarmState");
        AlarmState alarmState = AlarmState.newBuilder().setValue(AlarmState.State.ALARM).build();
        ((SmokeDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(smokeDetectorRemote.getId())).updateSmokeAlarmStateProvider(alarmState);
        smokeDetectorRemote.requestData().get();
        Assert.assertEquals("The getter for the smoke alarm state returns the wrong value!", alarmState.getValue(), smokeDetectorRemote.getSmokeAlarmState().getValue());
    }

    /**
     * Test of getSmokeState method, of class SmokeDetectorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetSmokeState() throws Exception {
        System.out.println("getSmokeState");
        SmokeState smokeState = SmokeState.newBuilder().setValue(SmokeState.State.SOME_SMOKE).setSmokeLevel(13d).build();
        ((SmokeDetectorController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(smokeDetectorRemote.getId())).updateSmokeStateProvider(smokeState);
        smokeDetectorRemote.requestData().get();
        Assert.assertEquals("The getter for the smoke state returns the wrong value!", smokeState.getValue(), smokeDetectorRemote.getSmokeState().getValue();
    }

}
