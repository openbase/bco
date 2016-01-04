package org.dc.bco.registry.device.lib.util;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitTemplateType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class DeviceConfigUtilsTest {

    public DeviceConfigUtilsTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of checkDuplicatedUnitType method, of class UnitBoundsToDeviceConsistencyHandler.
     */
    @Test
    public void testCheckDuplicatedUnitType() {
        System.out.println("checkDuplicatedUnitType");
        DeviceConfigType.DeviceConfig.Builder deviceConfig = DeviceConfigType.DeviceConfig.newBuilder();
        boolean expResult;

        expResult = false;
        deviceConfig.addUnitConfig(UnitConfigType.UnitConfig.newBuilder().setType(UnitTemplateType.UnitTemplate.UnitType.LIGHT));
        assertEquals(expResult, DeviceConfigUtils.checkDuplicatedUnitType(deviceConfig.build()));

        expResult = true;
        deviceConfig.addUnitConfig(UnitConfigType.UnitConfig.newBuilder().setType(UnitTemplateType.UnitTemplate.UnitType.LIGHT));
        assertEquals(expResult, DeviceConfigUtils.checkDuplicatedUnitType(deviceConfig.build()));
    }
}
