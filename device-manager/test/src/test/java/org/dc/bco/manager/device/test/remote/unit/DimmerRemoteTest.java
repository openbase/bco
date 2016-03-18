/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.test.remote.unit;

/*
 * #%L
 * COMA DeviceManager Test
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.bco.dal.lib.data.Location;
import org.dc.bco.dal.lib.layer.unit.DimmerController;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.dc.jps.core.JPService;
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.dal.remote.unit.DimmerRemote;
import org.dc.bco.manager.device.core.DeviceManagerLauncher;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author thuxohl
 */
public class DimmerRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DimmerRemoteTest.class);

    private static DimmerRemote dimmerRemote;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static MockRegistry registry;
    private static Location location;

    public DimmerRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, org.dc.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();

        location = new Location(registry.getLocation());

        dimmerRemote = new DimmerRemote();
        dimmerRemote.init(MockRegistry.DIMMER_LABEL, location);
        dimmerRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (dimmerRemote != null) {
            dimmerRemote.shutdown();
        }
        if (registry != null) {
            MockRegistryHolder.shutdownMockRegistry();
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of notifyUpdated method, of class DimmerRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of setPower method, of class DimmerRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testSetPower() throws Exception {
        System.out.println("setPowerState");
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        dimmerRemote.setPower(state);
        dimmerRemote.requestStatus();
        assertEquals("Power has not been set in time!", state, dimmerRemote.getData().getPowerState());
    }

    /**
     * Test of getPower method, of class DimmerRemote.
     */
    @Test(timeout = 60000)
    public void testGetPower() throws Exception {
        System.out.println("getPowerState");
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        ((DimmerController) deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().get(dimmerRemote.getId())).updatePower(state);
        dimmerRemote.requestStatus();
        assertEquals("Power has not been set in time!", state, dimmerRemote.getPower());
    }

    /**
     * Test of setDimm method, of class DimmerRemote.
     */
    @Test(timeout = 60000)
    public void testSetDim() throws Exception {
        System.out.println("setDim");
        Double dim = 66d;
        dimmerRemote.setDim(dim);
        dimmerRemote.requestStatus();
        assertEquals("Dimm has not been set in time!", dim, dimmerRemote.getData().getValue(), 0.1);
    }

    /**
     * Test of getDimm method, of class DimmerRemote.
     */
    @Test(timeout = 60000)
    public void testGetDimm() throws Exception {
        System.out.println("getDimm");
        Double dimm = 70.0d;
        ((DimmerController) deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().get(dimmerRemote.getId())).updateDim(dimm);
        dimmerRemote.requestStatus();
        assertEquals("Dimm has not been set in time!", dimm, dimmerRemote.getDim());
    }
}
