package org.openbase.bco.registry.unit.test;

/*-
 * #%L
 * BCO Registry Unit Test
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.concurrent.Future;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.schedule.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.domotic.unit.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class TestWaitUntilReady {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestWaitUntilReady.class);

    public TestWaitUntilReady() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();
            MockRegistryHolder.newMockRegistry();
        } catch (JPServiceException | InstantiationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    private boolean waitedUntilReady = false;

    @Test(timeout = 10000)
    public void testWaitUntilReady() throws Exception {
        System.out.println("testWaitUntilReady");

        final String deviceClassLabel = "ReadyClass";
        final String productNumber = "ReadyProductNumber";
        final String company = "ReadyCompany";

        DeviceClass deviceClass = MockRegistry.getDeviceClass(deviceClassLabel, productNumber, company, UnitType.POWER_SWITCH, UnitType.BUTTON, UnitType.HANDLE);
        deviceClass = Registries.getDeviceRegistry().registerDeviceClass(deviceClass).get();

        Registries.getUnitRegistry().addDataObserver((Observable<UnitRegistryData> source, UnitRegistryData data) -> {
            if (waitedUntilReady) {
                System.out.println("Received an update even though waitUntilReady has returned!");
                assert false;
            } else {
                System.out.println("update received");
            }
        });

        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        final String deviceLabel = "ReadyDevice ";
        final int iterations = 10;
        for (int i = 0; i < iterations; ++i) {
            waitedUntilReady = false;
//            UnitConfig deviceUnitConfig = getBasicDevice(deviceLabel + i, deviceClass);
            UnitConfig deviceUnitConfig = MockRegistry.getDeviceConfig(deviceLabel + i, String.valueOf(i), deviceClass);
            System.out.println("Trigger device registration!");
            Future<UnitConfig> registerUnitConfig = Registries.getUnitRegistry().registerUnitConfig(deviceUnitConfig);
//            DeviceConfig.Builder device = test.getDeviceConfigBuilder();
//            device.clearUnitId();
//            Registries.getUnitRegistry().updateUnitConfig(test.build());
            System.out.println("Wait until ready");
            Thread.sleep(20);
            stopwatch.restart();
            Registries.waitUntilReady();
            stopwatch.stop();
            System.out.println("Waiting returned after " + stopwatch.getTime() + "ms");
            stopwatch.restart();
            registerUnitConfig.get();
            stopwatch.stop();
            if (stopwatch.getTime() >= 1) {
                Assert.assertTrue("Get did not return immediatly but took " + stopwatch.getTime() + "ms!", false);
            }
            waitedUntilReady = true;
        }

        Thread.sleep(1000);
    }

    private UnitConfig getBasicDevice(String label, DeviceClass deviceClass) {
        UnitConfig.Builder deviceUnitConfig = UnitConfig.newBuilder();
        DeviceConfig.Builder deviceConfig = deviceUnitConfig.getDeviceConfigBuilder();

        deviceUnitConfig.setLabel(label);
        deviceConfig.setDeviceClassId(deviceClass.getId());

        return deviceUnitConfig.build();
    }
}
