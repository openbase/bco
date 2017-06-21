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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.UnitRegistryController;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.storage.registry.ConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class TestWaitUntilReady {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestWaitUntilReady.class);

    private static boolean deplayConsistencyCheck = false;

    public TestWaitUntilReady() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();
            MockRegistryHolder.newMockRegistry();
            ((UnitRegistryController) MockRegistry.getUnitRegistry()).getDeviceUnitConfigRegistry().registerConsistencyHandler(new ConsistencyHandler<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder>, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder>>() {

                boolean alreadyDelayed = false;
                int counter = 0;

                @Override
                public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
                    if (!registry.isSandbox() && !alreadyDelayed && deplayConsistencyCheck) {
                        try {
                            Thread.sleep(10);
//                            LOGGER.info("counter : " + counter++);
                        } catch (InterruptedException ex) {
                            java.util.logging.Logger.getLogger(TestWaitUntilReady.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        alreadyDelayed = true;
                    }
                }

                @Override
                public void reset() {
                    alreadyDelayed = false;
                }

                @Override
                public void shutdown() {
                }
            });
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

    @Test(timeout = 30000)
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
//                System.out.println("update received");
            }
        });

        final String deviceLabel = "ReadyDevice ";
        final int iterations = 5;
        deplayConsistencyCheck = true;
        for (int i = 0; i < iterations; ++i) {
            waitedUntilReady = false;
            UnitConfig deviceUnitConfig = MockRegistry.getDeviceConfig(deviceLabel + i, String.valueOf(i), deviceClass);
//            System.out.println("Trigger device registration!");
            Future<UnitConfig> registerUnitConfig = Registries.getUnitRegistry().registerUnitConfig(deviceUnitConfig);
//            System.out.println("Wait until ready");

            // needed to make sure the registry is processing the registration task.
            Thread.sleep(5);
            Registries.waitUntilReady();
            Assert.assertTrue("Test failed because registry is not consistent after wait until done returned.", Registries.getUnitRegistry().getData().getUnitConfigRegistryConsistent());
            try {
                registerUnitConfig.get(2, TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                Assert.assertTrue("Test failed because registration result is not available", false);
            }
            waitedUntilReady = true;
        }
        deplayConsistencyCheck = false;
    }
}
