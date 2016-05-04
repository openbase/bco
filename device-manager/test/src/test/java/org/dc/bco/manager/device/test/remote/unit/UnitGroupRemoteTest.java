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
import java.util.ArrayList;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import java.util.List;
import org.dc.bco.dal.lib.layer.service.operation.PowerOperationService;
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
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        unitGroupRemote.setPower(state);

        for (Unit unit : units) {
            assertEquals("Power state of unit [" + unit.getConfig().getId() + "] has not been set on!", state, ((PowerOperationService) unit).getPower());
        }

        state = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        unitGroupRemote.setPower(state);
        for (Unit unit : units) {
            assertEquals("Power state of unit [" + unit.getConfig().getId() + "] has not been set on!", state, ((PowerOperationService) unit).getPower());
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
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        unitGroupRemote.setPower(state);
//        unitGroupRemote.requestStatus();
        assertEquals("Power state has not been set in time or the return value from the getter is different!", state, unitGroupRemote.getPower());
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
