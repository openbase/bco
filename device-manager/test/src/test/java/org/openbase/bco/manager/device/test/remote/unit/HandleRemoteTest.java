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
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.unit.HandleController;
import org.openbase.bco.dal.remote.unit.HandleRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Remote;
import org.slf4j.LoggerFactory;
import rst.domotic.state.HandleStateType.HandleState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class HandleRemoteTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HandleRemoteTest.class);

    private static HandleRemote handleRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;

    public HandleRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();
            registry = MockRegistryHolder.newMockRegistry();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();
            Registries.getUnitRegistry().waitForData(30, TimeUnit.SECONDS);

            handleRemote = new HandleRemote();
            handleRemote.initByLabel(MockRegistry.HANDLE_LABEL);
            handleRemote.activate();
            handleRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED);
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
            if (handleRemote != null) {
                handleRemote.shutdown();
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
     * Test of notifyUpdated method, of class HandleSensorRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getRotaryHandleState method, of class HandleSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetRotaryHandleState() throws Exception {
        System.out.println("getRotaryHandleState");
        HandleState handlestate = HandleState.newBuilder().setPosition(90).build();
        ((HandleController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(handleRemote.getId())).updateHandleStateProvider(handlestate);
        handleRemote.requestData().get();
        Assert.assertEquals("The getter for the handle state returns the wrong value!", handlestate, handleRemote.getHandleState());
    }
}
