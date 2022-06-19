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

import org.junit.jupiter.api.Timeout;

/**
 * If reactivated implement with abstract classes, since no real virtual registry currently exists
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class VirtualRegistrySyncTest {

//    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualRegistrySyncTest.class);
//
//    private static final String ROOT_LOCATION_LABEL = RootLocationPlugin.DEFAULT_ROOT_LOCATION_NAME;
//    private static UnitConfig ROOT_LOCATION;
//
//    private static final String DEVICE_CLASS_LABEL = "syncTestDeviceClass";
//    private static final String DEVICE_CLASS_COMPANY = "syncTestCompany";
//    private static final String DEVICE_CLASS_PRODUCT_NUMBER = "12344321";
//    private static DeviceClass DEVICE_CLASS;
//
//    private static ClassRegistryController deviceRegistry;
//    private static UnitRegistryController unitRegistry;
//    private static AppRegistryController appRegistry;
//    private static AgentRegistryController agentRegistry;
//    private static LocationRegistryController locationRegistry;
//
//    private static AuthenticatorController authenticatorController;
//
//    @BeforeAll
//    public static void setUpClass() throws Exception {
//        try {
//            JPService.setupJUnitTestMode();
//
//            authenticatorController = new AuthenticatorController();
//            authenticatorController.init();
//            authenticatorController.activate();
//            authenticatorController.waitForActivation();
//
//            deviceRegistry = new ClassRegistryController();
//            unitRegistry = new UnitRegistryController();
//            appRegistry = new AppRegistryController();
//            agentRegistry = new AgentRegistryController();
//            locationRegistry = new LocationRegistryController();
//
//            deviceRegistry.init();
//            unitRegistry.init();
//            appRegistry.init();
//            agentRegistry.init();
//            locationRegistry.init();
//
//            Thread deviceRegistryThread = new Thread(new Runnable() {
//
//                @Override
//                public void run() {
//                    try {
//                        deviceRegistry.activate();
//                    } catch (CouldNotPerformException | InterruptedException ex) {
//                        ExceptionPrinter.printHistory(ex, LOGGER);
//                    }
//                }
//            });
//
//            Thread unitRegistryThread = new Thread(new Runnable() {
//
//                @Override
//                public void run() {
//                    try {
//                        unitRegistry.activate();
//                    } catch (CouldNotPerformException | InterruptedException ex) {
//                        ExceptionPrinter.printHistory(ex, LOGGER);
//                    }
//                }
//            });
//
//            Thread appRegistryThread = new Thread(new Runnable() {
//
//                @Override
//                public void run() {
//                    try {
//                        appRegistry.activate();
//                    } catch (CouldNotPerformException | InterruptedException ex) {
//                        ExceptionPrinter.printHistory(ex, LOGGER);
//                    }
//                }
//            });
//
//            Thread agentRegistryThread = new Thread(new Runnable() {
//
//                @Override
//                public void run() {
//                    try {
//                        agentRegistry.activate();
//                    } catch (CouldNotPerformException | InterruptedException ex) {
//                        ExceptionPrinter.printHistory(ex, LOGGER);
//                    }
//                }
//            });
//
//            Thread locationRegistryThread = new Thread(new Runnable() {
//
//                @Override
//                public void run() {
//                    try {
//                        locationRegistry.activate();
//                    } catch (CouldNotPerformException | InterruptedException ex) {
//                        ExceptionPrinter.printHistory(ex, LOGGER);
//                    }
//                }
//            });
//
//            deviceRegistryThread.start();
//            unitRegistryThread.start();
//            appRegistryThread.start();
//            agentRegistryThread.start();
//            locationRegistryThread.start();
//
//            deviceRegistryThread.join();
//            unitRegistryThread.join();
//            appRegistryThread.join();
//            agentRegistryThread.join();
//            locationRegistryThread.join();
//
//            locationRegistry.waitUntilReady();
//            Registries.getUnitRegistry().waitForData();
//            Registries.getUnitRegistry().waitForData();
//
//            ROOT_LOCATION = locationRegistry.getRootLocationConfig();
//            UnitTemplateConfig unitTemplateConfig = UnitTemplateConfig.newBuilder().setUnitType(UnitType.LIGHT).build();
//            DeviceClass deviceClass = DeviceClass.newBuilder().setLabel(DEVICE_CLASS_LABEL).setCompany(DEVICE_CLASS_COMPANY).setProductNumber(DEVICE_CLASS_PRODUCT_NUMBER).addUnitTemplateConfig(unitTemplateConfig).build();
//            DEVICE_CLASS = deviceRegistry.registerDeviceClass(deviceClass).get();
//
//            while (!unitRegistry.getDeviceRegistryRemote().containsDeviceClass(DEVICE_CLASS)) {
//                Thread.sleep(50);
//            }
//        } catch (CouldNotPerformException | ExecutionException | InterruptedException | JPServiceException ex) {
//            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
//        }
//    }
//
//    @AfterAll
//    public static void tearDownClass() throws Exception {
//        try {
//            if (unitRegistry != null) {
//                unitRegistry.shutdown();
//            }
//            if (deviceRegistry != null) {
//                deviceRegistry.shutdown();
//            }
//            if (appRegistry != null) {
//                appRegistry.shutdown();
//            }
//            if (agentRegistry != null) {
//                agentRegistry.shutdown();
//            }
//            if (locationRegistry != null) {
//                locationRegistry.shutdown();
//            }
//
//            Registries.shutdown();
//
//            authenticatorController.shutdown();
//        } catch (Exception ex) {
//            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
//        }
//    }
//
//    private DeviceConfig deviceConfig = DeviceConfig.getDefaultInstance();
//    private UnitConfig deviceUnitConfig = UnitConfig.getDefaultInstance();
//
//    private final Stopwatch deviceStopWatch = new Stopwatch();
//    private final Stopwatch locationStopWatch = new Stopwatch();
//
//    private final SyncObject DEVICE_LOCK = new SyncObject("deviceRegistryLock");
//    private final SyncObject LOCATION_LOCK = new SyncObject("locationRegistryLock");
//
//    @Test
//    @Timeout(15)
//    public void testVirtualRegistrySync() throws Exception {
//        Stopwatch stopwatch = new Stopwatch();
//        stopwatch.start();
//        PlacementConfig rootPlacement = PlacementConfig.newBuilder().setLocationId(ROOT_LOCATION.getId()).build();
//        InventoryState inventoryState = InventoryState.newBuilder().setValue(InventoryState.State.INSTALLED).build();
//
//        String label = "syncTestDevice - ";
//        String serialNumber = "0000-";
//
//        final Observer deviceRegistryObserver = (Observer<DeviceRegistryData>) (Observable<DeviceRegistryData> source, DeviceRegistryData data) -> {
//            synchronized (DEVICE_LOCK) {
////                LOGGER.info("DeviceRegistry notification...");
//                DEVICE_LOCK.notifyAll();
//            }
//        };
//        deviceRegistry.addDataObserver(deviceRegistryObserver);
//        Thread waitForDeviceUpdateThread;
//
//        final Observer locationRegistryObserver = (Observer<UnitRegistryData>) (Observable<UnitRegistryData> source, UnitRegistryData data) -> {
//            synchronized (LOCATION_LOCK) {
////                LOGGER.info("LocationRegistry notification...");
//                LOCATION_LOCK.notifyAll();
//            }
//        };
//        locationRegistry.addDataObserver(locationRegistryObserver);
//        Thread waitForLocationUpdateThread;
//
//        double totalDeviceSyncTime = 0.0;
//        double totalLocationSyncTime = 0.0;
//        int iterations = 10;
//        for (int i = 0; i < iterations; ++i) {
//            waitForDeviceUpdateThread = getDeviceThread();
//            waitForLocationUpdateThread = getLocationThread();
//
//            deviceConfig = DeviceConfig.newBuilder().setDeviceClassId(DEVICE_CLASS.getId()).setSerialNumber(serialNumber + i).setInventoryState(inventoryState).build();
//            deviceUnitConfig = UnitConfig.newBuilder().setType(UnitType.DEVICE).setLabel(label + i).setPlacementConfig(rootPlacement).setDeviceConfig(deviceConfig).build();
//
//            waitForDeviceUpdateThread.start();
//            waitForLocationUpdateThread.start();
//
//            deviceStopWatch.restart();
//            locationStopWatch.restart();
//
//            try {
//                if ((i % 2) == 0) {
//                    deviceUnitConfig = unitRegistry.registerUnitConfig(deviceUnitConfig).get();
//                } else {
//                    deviceUnitConfig = deviceRegistry.registerUnitConfig(deviceUnitConfig).get();
//                }
//            } catch (ExecutionException ex) {
//                throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
//            }
//
//            waitForDeviceUpdateThread.join();
//            waitForLocationUpdateThread.join();
//
//            totalDeviceSyncTime += deviceStopWatch.getTime();
//            totalLocationSyncTime += locationStopWatch.getTime();
//            LOGGER.info(deviceStopWatch.getTime() + " ms until device[" + deviceUnitConfig.getLabel() + "] registered in classRegistry!");
//            LOGGER.info(locationStopWatch.getTime() + " ms until device[" + deviceUnitConfig.getLabel() + "] registered in root location!");
//        }
//
//        deviceRegistry.removeDataObserver(deviceRegistryObserver);
//        locationRegistry.removeDataObserver(locationRegistryObserver);
//
//        LOGGER.info("Average time for classRegistry synchronisation " + (totalDeviceSyncTime / iterations) + "ms");
//        LOGGER.info("Average time for templateRegistry synchronisation " + (totalLocationSyncTime / iterations) + "ms");
//    }
//
//    private Thread getDeviceThread() {
//        return new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                synchronized (DEVICE_LOCK) {
//
//                    try {
//                        while (!deviceUnitConfig.hasId() && !containsByLabel(new ArrayList<>(deviceRegistry.getDeviceConfigs()))) {
////                            LOGGER.info("Device not [" + deviceUnitConfig.getLabel() + "] not contained in device registry");
//                            DEVICE_LOCK.wait();
////                            LOGGER.info("DeviceThread awoke from waiting....");
//                        }
////                        LOGGER.info("Device is contained in classRegistry");
//                        deviceStopWatch.stop();
//                    } catch (CouldNotPerformException | InterruptedException ex) {
//                        ExceptionPrinter.printHistory(ex, LOGGER);
//                    }
//                }
//            }
//        });
//    }
//
//    private boolean containsByLabel(final List<UnitConfig> deviceUnitConfigList) {
//        return deviceUnitConfigList.stream().anyMatch((unitConfig) -> (unitConfig.getLabel().equals(deviceUnitConfig.getLabel())));
//    }
//
//    private Thread getLocationThread() {
//        return new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                synchronized (LOCATION_LOCK) {
//                    try {
//                        List<UnitConfig> deviceUnitConfigList = new ArrayList<>();
////                        String units = "";
//                        for (String id : locationRegistry.getRootLocationConfig().getLocationConfig().getUnitIdList()) {
////                            UnitConfig unitConfig = unitRegistry.getUnitConfigById(id);
//                            deviceUnitConfigList.add(unitRegistry.getUnitConfigById(id));
////                            units += "[" + unitConfig.getLabel() + ", " + unitConfig.getUnitType() + "]\n";
//                        }
////                        LOGGER.info("Devices registered in location:\n" + units);
//                        try {
//                            while (!containsByLabel(deviceUnitConfigList)) {
////                                LOGGER.info("Device not [" + deviceUnitConfig.getLabel() + "] not contained in location");
//                                LOCATION_LOCK.wait();
////                                LOGGER.info("LocationThread awoke from waiting....");
//                                deviceUnitConfigList.clear();
//                                for (String id : locationRegistry.getRootLocationConfig().getLocationConfig().getUnitIdList()) {
//                                    deviceUnitConfigList.add(unitRegistry.getUnitConfigById(id));
//                                }
//                            }
//                            locationStopWatch.stop();
////                            LOGGER.info("Device is contained in templateRegistry");
//                        } catch (CouldNotPerformException | InterruptedException ex) {
//                            ExceptionPrinter.printHistory(ex, LOGGER);
//                        }
//                    } catch (CouldNotPerformException ex) {
//                        ExceptionPrinter.printHistory(ex, LOGGER);
//                    }
//                }
//            }
//        });
//    }
}
