package org.openbase.bco.registry.unit.test;

/*
 * #%L
 * BCO Registry Unit Test
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.communication.jp.JPComLegacyMode;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.*;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DeviceRegistryLegacyTest extends AbstractBCORegistryTest {

    @BeforeClass
    public static void setupProperties() throws Exception {
        JPService.registerProperty(JPComLegacyMode.class, true);
    }

    @AfterClass
    public static void removeProperties() throws Exception {
        JPService.registerProperty(JPComLegacyMode.class, false);
    }

    /**
     * Test of testRegisterTwoDevicesWithSameLabel method, of class
     * DeviceRegistryImpl.
     */
    @Test(timeout = 5000)
    public void testRegisterTwoDevicesWithSameLabel() throws Exception {
        System.out.println("testRegisterTwoDevicesWithSameLabel");
        String serialNumber1 = "FIRST_DEV";
        String serialNumber2 = "BAD_DEV";
        String deviceLabel = "SameLabelSameLocation";

        DeviceClass clazz = Registries.getClassRegistry().registerDeviceClass(generateDeviceClass("WithoutLabel", "xyz", "HuxGMBH")).get();
        UnitConfig deviceWithLabel1 = generateDeviceUnitConfig(deviceLabel, serialNumber1, clazz);
        UnitConfig deviceWithLabel2 = generateDeviceUnitConfig(deviceLabel, serialNumber2, clazz);

        Registries.getUnitRegistry().registerUnitConfig(deviceWithLabel1).get();
        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            Registries.getUnitRegistry().registerUnitConfig(deviceWithLabel2).get();
            fail("There was no exception thrown even though two devices with the same label [" + deviceLabel + "] where registered in the same location");
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            assertTrue(true);
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }
}
