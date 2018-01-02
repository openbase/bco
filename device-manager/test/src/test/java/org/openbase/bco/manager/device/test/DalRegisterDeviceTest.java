package org.openbase.bco.manager.device.test;

/*
 * #%L
 * BCO Manager Device Test
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.PowerSwitchRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Remote.ConnectionState;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DalRegisterDeviceTest extends AbstractBCODeviceManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DalRegisterDeviceTest.class);

    private static final String DEVICE_CONFIG_LABEL = "RunningDevice";

    public DalRegisterDeviceTest() {
    }

    @Before
    public void setUp() throws InitializationException, InstantiationException {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test registering a new device while the device manager is running.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testRegisterDeviceWhileRunning() throws Exception {
        System.out.println("testRegisterDeviceWhileRunning");

        // create a device class, save and remove the second unit template config
        DeviceClass.Builder deviceClassBuilder = MockRegistry.getDeviceClass("TestRegisterDeviceWhileRunnint", "DeviceManagerLauncherAndCoKG123456", "DeviceManagerLauncherAndCoKG", UnitType.COLORABLE_LIGHT, UnitType.POWER_SWITCH).toBuilder();
        UnitTemplateConfig powerSwitchTemplateConfig = deviceClassBuilder.getUnitTemplateConfig(1);
        deviceClassBuilder.removeUnitTemplateConfig(1);

        // register the device class
        DeviceClass deviceClass = Registries.getDeviceRegistry().registerDeviceClass(deviceClassBuilder.build()).get();
        mockRegistry.waitForDeviceClass(deviceClass);

        // register a device with that class and retrieve the colorable light
        UnitConfig deviceUnitConfig = Registries.getUnitRegistry().registerUnitConfig(MockRegistry.getDeviceConfig(DEVICE_CONFIG_LABEL, "DeviceManagerLauncherTestSerialNumber", deviceClass)).get();
        UnitConfig colorableLightConfig = Registries.getUnitRegistry().getUnitConfigById(deviceUnitConfig.getDeviceConfig().getUnitId(0));

        // start a unit remote to that colorable light
        ColorableLightRemote colorableLightRemote = Units.getUnit(colorableLightConfig, true, ColorableLightRemote.class);

        // test if the state of the light can be changed
        colorableLightRemote.setPowerState(PowerState.State.ON).get();
        assertEquals("Power state has not been set in time!", PowerState.State.ON, colorableLightRemote.getData().getPowerState().getValue());

        // add the previously removed unit template config again
        deviceClassBuilder = deviceClass.toBuilder();
        deviceClassBuilder.addUnitTemplateConfig(powerSwitchTemplateConfig);

        // update the device class
        deviceClass = Registries.getDeviceRegistry().updateDeviceClass(deviceClassBuilder.build()).get();
        mockRegistry.waitForDeviceClass(deviceClass);

        // wait up to half a second until the device is updated, is done in this way because it can fail but is a specific unit test failure
        long currentTime = System.currentTimeMillis();
        while (Registries.getUnitRegistry().getUnitConfigById(deviceUnitConfig.getId()).getDeviceConfig().getUnitIdCount() != deviceClass.getUnitTemplateConfigCount() && (System.currentTimeMillis() - currentTime) < 500) {
            Thread.sleep(10);
        }
        deviceUnitConfig = Registries.getUnitRegistry().getUnitConfigById(deviceUnitConfig.getId());
        assertEquals(deviceClass.getUnitTemplateConfigCount(), deviceUnitConfig.getDeviceConfig().getUnitIdCount());

        // get the second unit of the device
        UnitConfig powerSwitchConfig = Registries.getUnitRegistry().getUnitConfigById(deviceUnitConfig.getDeviceConfig().getUnitId(1));

        // start a unit remote for it
        PowerSwitchRemote powerSwitchRemote = Units.getUnit(powerSwitchConfig, true, PowerSwitchRemote.class);

        // test if both unit remotes can be used
        powerSwitchRemote.setPowerState(PowerState.State.ON).get();
        colorableLightRemote.setPowerState(PowerState.State.OFF).get();
        assertEquals("Power state has not been set in time!", PowerState.State.ON, powerSwitchRemote.getData().getPowerState().getValue());
        assertEquals("Power state has not been set in time!", PowerState.State.OFF, colorableLightRemote.getData().getPowerState().getValue());

        // remote the second unit template config again
        deviceClassBuilder = deviceClass.toBuilder();
        deviceClassBuilder.removeUnitTemplateConfig(1);

        deviceClass = Registries.getDeviceRegistry().updateDeviceClass(deviceClassBuilder.build()).get();
        mockRegistry.waitForDeviceClass(deviceClass);

        // wait up to half a second until the device is updated, is done in this way because it can fail but is a specific unit test failure
        currentTime = System.currentTimeMillis();
        while (Registries.getUnitRegistry().getUnitConfigById(deviceUnitConfig.getId()).getDeviceConfig().getUnitIdCount() != deviceClass.getUnitTemplateConfigCount() && (System.currentTimeMillis() - currentTime) < 500) {
            Thread.sleep(10);
        }
        assertTrue("Unit registry still contains the unit config which should have been removed", !Registries.getUnitRegistry().containsUnitConfigById(powerSwitchConfig.getId()));

        // wait up to half a second for the unit controller to be removed from the device manager
        currentTime = System.currentTimeMillis();
        while (deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().contains(powerSwitchConfig.getId()) && (System.currentTimeMillis() - currentTime) < 500) {
            Thread.sleep(10);
        }
        assertTrue("DeviceManager still contains removed unit controller", !deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().contains(powerSwitchConfig.getId()));

        // test if the colorable light can still be used
        colorableLightRemote.setPowerState(PowerState.State.OFF).get();
        assertEquals("Power state has not been set in time!", PowerState.State.OFF, colorableLightRemote.getData().getPowerState().getValue());

        // test if the power switch remote has lost its connection
        powerSwitchRemote.waitForConnectionState(ConnectionState.DISCONNECTED, 1000);
        assertEquals("Remote has not disconnected even though its config should have been removed!", ConnectionState.DISCONNECTED, powerSwitchRemote.getConnectionState());
    }

    boolean running = true;

    @Test(timeout = 15000)
    public void testRegisteringManyDevices() throws Exception {
        System.out.println("testRegisteringManyDevices");

        String deviceClassLabel = "SimpleDevice";
        String productNumber = "ab42-123g";
        String company = "SimpleManufacturing";
        DeviceClass.Builder deviceClassBuilder = MockRegistry.getDeviceClass(deviceClassLabel, productNumber, company, UnitType.COLORABLE_LIGHT, UnitType.COLORABLE_LIGHT, UnitType.POWER_SWITCH).toBuilder();

        DeviceClass deviceClass = Registries.getDeviceRegistry().registerDeviceClass(deviceClassBuilder.build()).get();
        mockRegistry.waitForDeviceClass(deviceClass);

        final List<UnitConfig> registeredUnitConfigs = new ArrayList<>();
        final SyncObject synchronizer = new SyncObject("synchronizer");

        GlobalCachedExecutorService.submit(() -> {
            Random random = new Random();
            while (running) {
                try {
                    synchronized (synchronizer) {
                        synchronizer.wait();
                    }
                    if (!running) {
                        break;
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                try {
                    UnitConfig.Builder toChange = registeredUnitConfigs.get(random.nextInt(registeredUnitConfigs.size())).toBuilder();
//                    if (random.nextDouble() < 0.7) {
                    Registries.getUnitRegistry().updateUnitConfig(toChange.setBoundToUnitHost(!toChange.getBoundToUnitHost()).build()).get();
//                    } else {
//                        Registries.getUnitRegistry().updateUnitConfig(toChange.setLabel(toChange.getLabel() + "-").build()).get();
//                    }
                } catch (CouldNotPerformException | ExecutionException ex) {
                    ExceptionPrinter.printHistory(ex, LOGGER);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        String deviceConfigLabel = "SimpleDevice";
        try {
            for (int i = 0; i < 10; ++i) {
                String serialNumber = productNumber + "-" + i;
                System.out.println("Register device");
                UnitConfig deviceUnitConfig = Registries.getUnitRegistry().registerUnitConfig(MockRegistry.getDeviceConfig(deviceConfigLabel + "_" + i, serialNumber, deviceClass)).get();
                assertTrue("DeviceUnitConfig[" + deviceUnitConfig.getLabel() + "] is not available after registration!", Registries.getUnitRegistry().containsUnitConfigById(deviceUnitConfig.getId()));
                UnitConfig colorableLightConfig1 = Registries.getUnitRegistry().getUnitConfigById(deviceUnitConfig.getDeviceConfig().getUnitId(0));
                UnitConfig colorableLightConfig2 = Registries.getUnitRegistry().getUnitConfigById(deviceUnitConfig.getDeviceConfig().getUnitId(1));
                UnitConfig powerSwitchConfig = Registries.getUnitRegistry().getUnitConfigById(deviceUnitConfig.getDeviceConfig().getUnitId(2));

                System.out.println("Add to list");
                registeredUnitConfigs.add(deviceUnitConfig);
                registeredUnitConfigs.add(colorableLightConfig1);
                registeredUnitConfigs.add(colorableLightConfig2);
                registeredUnitConfigs.add(powerSwitchConfig);

                synchronized (synchronizer) {
                    synchronizer.notifyAll();
                }

                System.out.println("GetColorRemote1");
                ColorableLightRemote colorableLightRemote1 = Units.getUnit(colorableLightConfig1, true, ColorableLightRemote.class);
                System.out.println("GetColorRemote1");
                ColorableLightRemote colorableLightRemote2 = Units.getUnit(colorableLightConfig2, true, ColorableLightRemote.class);
                System.out.println("GetPowerRemote");
                PowerSwitchRemote powerSwitchRemote = Units.getUnit(powerSwitchConfig, true, PowerSwitchRemote.class);

                System.out.println("SetPowerState1");
                colorableLightRemote1.setPowerState(PowerState.State.ON).get();
                System.out.println("SetPowerState2");
                colorableLightRemote2.setPowerState(PowerState.State.OFF).get();
                System.out.println("SetPowerState3");
                powerSwitchRemote.setPowerState(PowerState.State.ON).get();
                assertEquals("Power state has not been set in time!", PowerState.State.ON, colorableLightRemote1.getData().getPowerState().getValue());
                assertEquals("Power state has not been set in time!", PowerState.State.OFF, colorableLightRemote2.getData().getPowerState().getValue());
                assertEquals("Power state has not been set in time!", PowerState.State.ON, powerSwitchRemote.getData().getPowerState().getValue());
            }
        } catch (CouldNotPerformException | ExecutionException | InterruptedException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        } finally {
            running = false;
            synchronized (synchronizer) {
                synchronizer.notifyAll();
            }
        }
    }
}
