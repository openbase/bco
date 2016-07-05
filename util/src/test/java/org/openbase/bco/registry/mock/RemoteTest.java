/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.registry.mock;

/*
 * #%L
 * REM Utility
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jul.pattern.Remote;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class RemoteTest {

    public RemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();
        MockRegistryHolder.newMockRegistry();
    }

    @AfterClass
    public static void tearDownClass() {
        MockRegistryHolder.shutdownMockRegistry();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test shutting down a device registry remote several times while another
     * one stays active to test remote interference.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testRestartingDeviceRegistryRemotes() throws Exception {
        System.out.println("testRestartingDeviceRegistryRemotes");
        DeviceRegistryRemote deviceRemoteAlwaysOn = new DeviceRegistryRemote();
        deviceRemoteAlwaysOn.init();
        deviceRemoteAlwaysOn.activate();
        deviceRemoteAlwaysOn.waitForConnectionState(Remote.ConnectionState.CONNECTED);

        DeviceRegistryRemote deviceRemoteToggle = new DeviceRegistryRemote();
        deviceRemoteToggle.init();
        deviceRemoteToggle.activate();
        deviceRemoteToggle.waitForConnectionState(Remote.ConnectionState.CONNECTED);

        int testNumber = 100;
        for (int i = 0; i < testNumber; ++i) {
            deviceRemoteToggle.shutdown();
            deviceRemoteToggle.waitForConnectionState(Remote.ConnectionState.DISCONNECTED);

            assertEquals("Remote has been shutdown with another in the [" + i + "]s try!", Remote.ConnectionState.CONNECTED, deviceRemoteAlwaysOn.getConnectionState());
            deviceRemoteAlwaysOn.requestData().get();
            
            deviceRemoteToggle.activate();
            deviceRemoteToggle.waitForConnectionState(Remote.ConnectionState.CONNECTED);
        }

        deviceRemoteAlwaysOn.shutdown();
        deviceRemoteToggle.shutdown();
    }
}
