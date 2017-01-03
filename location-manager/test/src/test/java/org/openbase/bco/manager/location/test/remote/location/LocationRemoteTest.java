package org.openbase.bco.manager.location.test.remote.location;

/*
 * #%L
 * COMA DeviceManager Test
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.TemperatureControllerController;
import org.openbase.bco.dal.lib.layer.unit.TemperatureSensorController;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.manager.location.core.LocationManagerLauncher;
import org.openbase.bco.registry.location.lib.LocationRegistry;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Remote;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LocationRemoteTest.class);

    private static DeviceManagerLauncher deviceManagerLauncher;
    private static LocationManagerLauncher locationManagerLauncher;
    private static MockRegistry registry;

    private static LocationRegistry locationRegistry;
    private static UnitRegistry unitRegistry;

    private static LocationRemote locationRemote;

    public LocationRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, InstantiationException, CouldNotPerformException, JPServiceException, InterruptedException {
        try {
            JPService.setupJUnitTestMode();
            JPService.registerProperty(JPHardwareSimulationMode.class, true);
            registry = MockRegistryHolder.newMockRegistry();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();
            deviceManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);

            locationManagerLauncher = new LocationManagerLauncher();
            locationManagerLauncher.launch();

            locationRegistry = CachedLocationRegistryRemote.getRegistry();
            unitRegistry = CachedUnitRegistryRemote.getRegistry();

            locationRemote = new LocationRemote();
            locationRemote.init(locationRegistry.getRootLocationConfig());
            locationRemote.activate();
            locationRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED);
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        try {
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }
            if (locationRemote != null) {
                locationRemote.shutdown();
            }
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {
    }

    @Test(timeout = 5000)
    public void testLocationToUnitPipeline() throws Exception {
        System.out.println("testLocationToUnitPipeline");

        List<PowerStateOperationService> powerServiceList = new ArrayList<>();
        for (UnitConfig dalUnitConfig : unitRegistry.getDalUnitConfigs()) {
            if (unitHasService(dalUnitConfig, ServiceType.POWER_STATE_SERVICE, ServicePattern.OPERATION)) {
                UnitController unitController = deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dalUnitConfig.getId());
                powerServiceList.add((PowerStateOperationService) unitController);
            }
        }

        PowerState powerOn = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        PowerState powerOff = PowerState.newBuilder().setValue(PowerState.State.OFF).build();

        locationRemote.setPowerState(powerOn).get();
        for (PowerStateOperationService powerStateService : powerServiceList) {
            Assert.assertEquals("PowerState of unit [" + ((UnitController) powerStateService).getLabel() + "] has not been updated by the loationRemote!", powerOn.getValue(), powerStateService.getPowerState().getValue());
        }

        locationRemote.setPowerState(powerOff).get();
        for (PowerStateOperationService powerStateService : powerServiceList) {
            Assert.assertEquals("PowerState of unit [" + ((UnitController) powerStateService).getLabel() + "] has not been updated by the loationRemote!", powerOff.getValue(), powerStateService.getPowerState().getValue());
        }
    }

    private boolean unitHasService(UnitConfig unitConfig, ServiceType serviceType, ServicePattern servicePattern) throws CouldNotPerformException {
        for (ServiceTemplate serviceTemplate : unitRegistry.getUnitTemplateByType(unitConfig.getType()).getServiceTemplateList()) {
            if (serviceTemplate.getType() == serviceType && serviceTemplate.getPattern() == servicePattern) {
                return true;
            }
        }
        return false;
    }

    @Test(timeout = 5000)
    public void testUnitToLocationPipeline() throws Exception {
        System.out.println("testUnitToLocationPipeline");

        List<TemperatureSensorController> temperatureSensorList = new ArrayList<>();
        List<TemperatureControllerController> temperatureControllerList = new ArrayList<>();
        for (UnitConfig dalUnitConfig : unitRegistry.getDalUnitConfigs()) {
            UnitController unitController = deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dalUnitConfig.getId());
            if (unitController instanceof TemperatureSensorController) {
                temperatureSensorList.add((TemperatureSensorController) unitController);
            } else if (unitController instanceof TemperatureControllerController) {
                temperatureControllerList.add((TemperatureControllerController) unitController);
            }
        }

        double temperature = 21;
        TemperatureState temperatureState = TemperatureState.newBuilder().setTemperature(temperature).build();
        for (TemperatureSensorController temperatureSensor : temperatureSensorList) {
            temperatureSensor.updateTemperatureStateProvider(temperatureState);
        }
        for (TemperatureControllerController temperatureController : temperatureControllerList) {
            temperatureController.updateTemperatureStateProvider(temperatureState);
        }

        locationRemote.requestData().get();
        while (locationRemote.getTemperatureState().getTemperature() != temperature) {
            Thread.sleep(50);
        }
        Assert.assertEquals("Temperature of the location has not been updated!", temperature, locationRemote.getTemperatureState().getTemperature(), 0.01);
    }

}
