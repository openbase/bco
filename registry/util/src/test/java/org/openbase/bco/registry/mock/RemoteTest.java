package org.openbase.bco.registry.mock;

/*
 * #%L
 * BCO Registry Utility
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.registry.clazz.remote.ClassRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.state.ConnectionStateType;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class RemoteTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RemoteTest.class);

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();
            MockRegistryHolder.newMockRegistry();
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() {
        try {
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    /**
     * Test shutting down a device registry remote several times while another
     * one stays active to test remote interference.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 15000)
    public void testRestartingDeviceRegistryRemotes() throws Exception {
        System.out.println("testRestartingDeviceRegistryRemotes");
        ClassRegistryRemote deviceRemoteAlwaysOn = new ClassRegistryRemote();
        deviceRemoteAlwaysOn.init();
        deviceRemoteAlwaysOn.activate();
        deviceRemoteAlwaysOn.waitForConnectionState(ConnectionState.State.CONNECTED);

        ClassRegistryRemote deviceRemoteToggle = new ClassRegistryRemote();
        deviceRemoteToggle.init();
        deviceRemoteToggle.activate();
        deviceRemoteToggle.waitForConnectionState(ConnectionState.State.CONNECTED);

        int testNumber = 1;
        for (int i = 0; i < testNumber; ++i) {
            deviceRemoteToggle.deactivate();
            deviceRemoteToggle.waitForConnectionState(ConnectionState.State.DISCONNECTED);

            assertEquals("Remote has been shutdown with another in the [" + i + "]s try!", ConnectionStateType.ConnectionState.State.CONNECTED, deviceRemoteAlwaysOn.getConnectionState());
            deviceRemoteAlwaysOn.requestData().get();

            deviceRemoteToggle.activate();
            deviceRemoteToggle.waitForConnectionState(ConnectionState.State.CONNECTED);
        }

        deviceRemoteAlwaysOn.shutdown();
        deviceRemoteToggle.shutdown();
    }
}
