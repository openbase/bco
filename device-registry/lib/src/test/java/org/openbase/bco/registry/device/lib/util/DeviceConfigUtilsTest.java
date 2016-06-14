package org.openbase.bco.registry.device.lib.util;

/*
 * #%L
 * REM DeviceRegistry Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
