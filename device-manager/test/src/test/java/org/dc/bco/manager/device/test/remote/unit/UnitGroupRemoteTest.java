/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.test.remote.unit;

import java.util.ArrayList;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import java.util.List;
import org.dc.bco.dal.lib.layer.service.PowerService;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.bco.dal.remote.unit.UnitGroupRemote;
import org.dc.bco.manager.device.core.DeviceManagerLauncher;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.bco.registry.mock.MockRegistryHolder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.LoggerFactory;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;

/**
 *
 * @author thuxohl
 */


public class UnitGroupRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AmbientLightRemoteTest.class);

    private static DeviceManagerLauncher deviceManagerLauncher;
    private static UnitGroupRemote unitGroupRemote;
    private static final List<Unit> units = new ArrayList<>();
    private static MockRegistry registry;

    public UnitGroupRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, InstantiationException, CouldNotPerformException, JPServiceException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockRegistryHolder.newMockRegistry();

        deviceManagerLauncher = new DeviceManagerLauncher();
        deviceManagerLauncher.launch();

        unitGroupRemote = new UnitGroupRemote();
        UnitGroupConfig.Builder unitGroupConfig = UnitGroupConfig.newBuilder().addServiceType(ServiceType.POWER_SERVICE).setLabel("testGroup");
        for (Unit unit : deviceManagerLauncher.getDeviceManager().getUnitControllerRegistry().getEntries()) {
            for (ServiceConfig serviceConfig : unit.getConfig().getServiceConfigList()) {
                if (serviceConfig.getType() == ServiceType.POWER_SERVICE) {
                    units.add(unit);
                    unitGroupConfig.addMemberId(unit.getConfig().getId());
                }
            }
        }
        logger.info("Unit group [" + unitGroupConfig.build() + "]");
        unitGroupRemote.init(unitGroupConfig.build());
        unitGroupRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (deviceManagerLauncher != null) {
            deviceManagerLauncher.shutdown();
        }
        if (unitGroupRemote != null) {
            unitGroupRemote.shutdown();
        }
        if (registry != null) {
            MockRegistryHolder.shutdownMockRegistry();
        }
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {

    }

    /**
     * Test of setPowerState method, of class UnitGroupRemote.
     *
     * @throws java.lang.Exception
     */
    //    @Test(timeout = 60000)
    @Test
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        PowerState.State state = PowerState.State.ON;
        unitGroupRemote.setPower(state);

        for (Unit unit : units) {
            assertEquals("Power state of unit [" + unit.getConfig().getId() + "] has not been set on!", state, ((PowerService) unit).getPower().getValue());
        }

        state = PowerState.State.OFF;
        unitGroupRemote.setPower(state);
        for (Unit unit : units) {
            assertEquals("Power state of unit [" + unit.getConfig().getId() + "] has not been set on!", state, ((PowerService) unit).getPower().getValue());
        }
    }

    /**
     * Test of getPowerState method, of class UnitGroupRemote.
     *
     * @throws java.lang.Exception
     */
    //    @Test(timeout = 60000)
    @Test
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        PowerState.State state = PowerState.State.OFF;
        unitGroupRemote.setPower(state);
//        unitGroupRemote.requestStatus();
        assertEquals("Power state has not been set in time or the return value from the getter is different!", state, unitGroupRemote.getPower().getValue());
    }

    /**
     * Test of setBrightness method, of class UnitGroupRemote.
     *
     * @throws java.lang.Exception
     */
    //    @Test(timeout = 60000)
    @Test
    public void testSetBrightness() throws Exception {
        System.out.println("setBrightness");
        Double brightness = 75d;
        try {
            unitGroupRemote.setBrightness(brightness);
            fail("Brighntess service has been used even though the group config is only defined for power service");
        } catch (CouldNotPerformException ex) {
        }
    }
}
