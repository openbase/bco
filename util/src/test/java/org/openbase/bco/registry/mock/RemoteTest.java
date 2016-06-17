/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.registry.mock;

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

    private static MockRegistry registry;

    public RemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();
        registry = MockRegistryHolder.newMockRegistry();
    }

    @AfterClass
    public static void tearDownClass() {
        registry.shutdown();
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
        deviceRemoteAlwaysOn.waitForConnectionState(Remote.RemoteConnectionState.CONNECTED);

        DeviceRegistryRemote deviceRemoteToggle = new DeviceRegistryRemote();
        deviceRemoteToggle.init();
        deviceRemoteToggle.activate();
        deviceRemoteToggle.waitForConnectionState(Remote.RemoteConnectionState.CONNECTED);

        int testNumber = 100;
        for (int i = 0; i < testNumber; ++i) {
            deviceRemoteToggle.shutdown();
            deviceRemoteToggle.waitForConnectionState(Remote.RemoteConnectionState.DISCONNECTED);

            assertEquals("Remote has been shutdown with another in the [" + i + "]s try!", Remote.RemoteConnectionState.CONNECTED, deviceRemoteAlwaysOn.getConnectionState());
            deviceRemoteAlwaysOn.requestData().get();
            
            deviceRemoteToggle.activate();
            deviceRemoteToggle.waitForConnectionState(Remote.RemoteConnectionState.CONNECTED);
        }

        deviceRemoteAlwaysOn.shutdown();
        deviceRemoteToggle.shutdown();
    }
}
