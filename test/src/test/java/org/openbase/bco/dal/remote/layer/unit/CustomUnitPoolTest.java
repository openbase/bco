package org.openbase.bco.dal.remote.layer.unit;

/*-
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import com.google.protobuf.Message;
import org.junit.*;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class CustomUnitPoolTest extends AbstractBCODeviceManagerTest {
    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getBatteryLevel method, of class BatteryRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testUnitPool() throws Exception {
        final CustomUnitPool customUnitPool = new CustomUnitPool();
        assertEquals("pool is active while never activated", false, customUnitPool.isActive());

        customUnitPool.activate();

        customUnitPool.init(
                unitConfig -> unitConfig.getUnitType() != UnitType.BUTTON,
                unitConfig -> {
                    return false;
                });

        customUnitPool.activate();

        for (UnitRemote<? extends Message> unitRemote : customUnitPool.getInternalUnitList()) {
            assertEquals("pool contains actually filtered entry!", UnitType.BUTTON, unitRemote.getUnitType());
        }

        customUnitPool.shutdown();
    }
}
