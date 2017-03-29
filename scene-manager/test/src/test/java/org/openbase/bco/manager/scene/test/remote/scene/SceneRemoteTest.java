package org.openbase.bco.manager.scene.test.remote.scene;

/*
 * #%L
 * BCO Manager Scene Test
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.remote.service.ColorStateServiceRemote;
import org.openbase.bco.dal.remote.service.PowerStateServiceRemote;
import org.openbase.bco.dal.remote.unit.LightRemote;
import org.openbase.bco.dal.remote.unit.PowerSwitchRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.unit.scene.SceneRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.manager.location.core.LocationManagerLauncher;
import org.openbase.bco.manager.scene.core.SceneManagerLauncher;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionConfigType.ActionConfig;
import rst.domotic.action.ActionPriorityType.ActionPriority;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.scene.SceneConfigType.SceneConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;
import rst.vision.ColorType;
import rst.vision.HSBColorType.HSBColor;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneRemoteTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SceneRemoteTest.class);
    public static final ActivationState ACTIVATE = ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build();

    private static SceneManagerLauncher sceneManagerLauncher;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static LocationManagerLauncher locationManagerLauncher;
    private static MockRegistry registry;

    private static PowerStateServiceRemote powerStateServiceRemote;
    private static ColorStateServiceRemote colorStateServiceRemote;

    private static final PowerState.State POWER_ON = PowerState.State.ON;
    private static final PowerState.State POWER_OFF = PowerState.State.OFF;
    private static final PowerState POWER_STATE_ON = PowerState.newBuilder().setValue(POWER_ON).build();
    private static final PowerState POWER_STATE_OFF = PowerState.newBuilder().setValue(POWER_OFF).build();
    private static final HSBColor COLOR_VALUE = HSBColor.newBuilder().setBrightness(100).setSaturation(90).setHue(10).build();
    private static final double TEMPERATURE = 21.3;

    public static final String SCENE_TEST = "testScene";
    public static final String SCENE_ROOT_LOCATION = "locationTestScene";
    public static final String SCENE_ROOT_LOCATION_ALL_DEVICES_OFF = "locationDevicesOffTestScene";
    public static final String SCENE_ROOT_LOCATION_ALL_DEVICES_ON = "locationDevicesOnTestScene";
    public static final String SCENE_ROOT_LOCATION_OFF = "locationOffTestScene";
    public static final String SCENE_ROOT_LOCATION_ON = "locationOnTestScene";

    public SceneRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        try {
            JPService.setupJUnitTestMode();
            registry = MockRegistryHolder.newMockRegistry();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();
            deviceManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);

            locationManagerLauncher = new LocationManagerLauncher();
            locationManagerLauncher.launch();
            locationManagerLauncher.getLaunchable();

            sceneManagerLauncher = new SceneManagerLauncher();
            sceneManagerLauncher.launch();

            powerStateServiceRemote = new PowerStateServiceRemote();
            colorStateServiceRemote = new ColorStateServiceRemote();

            registerScenes();

            powerStateServiceRemote.activate();
            colorStateServiceRemote.activate();

        } catch (JPServiceException | CouldNotPerformException | InterruptedException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            if (sceneManagerLauncher != null) {
                sceneManagerLauncher.shutdown();
            }
            if (powerStateServiceRemote != null) {
                powerStateServiceRemote.shutdown();
            }
            if (colorStateServiceRemote != null) {
                colorStateServiceRemote.shutdown();
            }
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    private static void registerScenes() throws CouldNotPerformException {
        try {
            ServiceJSonProcessor serviceJSonProcessor = new ServiceJSonProcessor();
            UnitRegistryRemote unitRegistry = Registries.getUnitRegistry();

            List<ActionConfig> actionConfigList = new ArrayList<>();

            ActionConfig.Builder actionConfigBuilder = ActionConfig.newBuilder();
            actionConfigBuilder.setActionAuthority(ActionAuthority.newBuilder().setAuthority(ActionAuthority.Authority.USER)).setActionPriority(ActionPriority.newBuilder().setPriority(ActionPriority.Priority.NORMAL));

            PowerState powerState = PowerState.newBuilder().setValue(POWER_ON).build();
            actionConfigBuilder.setServiceType(ServiceType.POWER_STATE_SERVICE);
            actionConfigBuilder.setServiceAttribute(serviceJSonProcessor.serialize(powerState));
            actionConfigBuilder.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(powerState));
            for (UnitConfig unitConfig : unitRegistry.getDalUnitConfigs()) {
                if (unitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    continue;
                }

                if (unitRegistry.getSubUnitTypes(UnitType.LIGHT).contains(unitConfig.getType()) || unitConfig.getType() == UnitType.LIGHT || unitConfig.getType() == UnitType.POWER_SWITCH) {
                    actionConfigBuilder.clearUnitId();
                    actionConfigBuilder.setUnitId(unitConfig.getId());
                    actionConfigList.add(actionConfigBuilder.build());
                    powerStateServiceRemote.init(unitConfig);
                }
            }

            ColorType.Color color = ColorType.Color.newBuilder().setType(ColorType.Color.Type.HSB).setHsbColor(COLOR_VALUE).build();
            ColorState colorState = ColorState.newBuilder().setColor(color).build();
            actionConfigBuilder.setServiceType(ServiceType.COLOR_STATE_SERVICE);
            actionConfigBuilder.setServiceAttribute(serviceJSonProcessor.serialize(colorState));
            actionConfigBuilder.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(colorState));
            for (UnitConfig unitConfig : unitRegistry.getDalUnitConfigs()) {
                if (unitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    continue;
                }

                if (unitConfig.getType() == UnitType.COLORABLE_LIGHT) {
                    actionConfigBuilder.clearUnitId();
                    actionConfigBuilder.setUnitId(unitConfig.getId());
                    actionConfigList.add(actionConfigBuilder.build());
                    colorStateServiceRemote.init(unitConfig);
                }
            }

            String label = SCENE_TEST;
            PlacementConfig placementConfig = PlacementConfig.newBuilder().setLocationId(Registries.getLocationRegistry().getRootLocationConfig().getId()).build();
            SceneConfig sceneConfig = SceneConfig.newBuilder().addAllActionConfig(actionConfigList).build();
            UnitConfig unitConfig = UnitConfig.newBuilder().setLabel(label).setType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            unitRegistry.registerUnitConfig(unitConfig).get();

            actionConfigList.clear();
            TemperatureState temperatureState = TemperatureState.newBuilder().setTemperature(TEMPERATURE).setTemperatureDataUnit(TemperatureState.DataUnit.CELSIUS).build();
            actionConfigBuilder.setServiceType(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE);
            actionConfigBuilder.setServiceAttribute(serviceJSonProcessor.serialize(temperatureState));
            actionConfigBuilder.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(temperatureState));
            actionConfigBuilder.setUnitId(Registries.getLocationRegistry().getRootLocationConfig().getId());
            actionConfigList.add(actionConfigBuilder.build());

            label = SCENE_ROOT_LOCATION;
            sceneConfig = SceneConfig.newBuilder().addAllActionConfig(actionConfigList).build();
            unitConfig = UnitConfig.newBuilder().setLabel(label).setType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            unitRegistry.registerUnitConfig(unitConfig).get();

            label = SCENE_ROOT_LOCATION_ALL_DEVICES_ON;
            LocationRemote locationRemote = Units.getUnit(Registries.getLocationRegistry().getRootLocationConfig(), true, LocationRemote.class);
            locationRemote.setPowerState(POWER_ON).get();
            locationRemote.requestData().get();
            sceneConfig = SceneConfig.newBuilder().addAllActionConfig(locationRemote.recordSnapshot().get().getActionConfigList()).build();
            unitConfig = UnitConfig.newBuilder().setLabel(label).setType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            unitRegistry.registerUnitConfig(unitConfig).get();

            label = SCENE_ROOT_LOCATION_ALL_DEVICES_OFF;
            locationRemote.setPowerState(POWER_OFF).get();
            locationRemote.requestData().get();
            sceneConfig = SceneConfig.newBuilder().addAllActionConfig(locationRemote.recordSnapshot().get().getActionConfigList()).build();
            unitConfig = UnitConfig.newBuilder().setLabel(label).setType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            unitRegistry.registerUnitConfig(unitConfig).get();

            label = SCENE_ROOT_LOCATION_ON;
            actionConfigList.clear();
            actionConfigBuilder.clear();
            actionConfigBuilder.setServiceType(ServiceType.POWER_STATE_SERVICE);
            actionConfigBuilder.setServiceAttribute(serviceJSonProcessor.serialize(POWER_STATE_ON));
            actionConfigBuilder.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(POWER_STATE_ON));
            actionConfigBuilder.setUnitId(Registries.getLocationRegistry().getRootLocationConfig().getId());
            actionConfigList.add(actionConfigBuilder.build());
            sceneConfig = SceneConfig.newBuilder().addAllActionConfig(actionConfigList).build();
            unitConfig = UnitConfig.newBuilder().setLabel(label).setType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            unitRegistry.registerUnitConfig(unitConfig).get();

            label = SCENE_ROOT_LOCATION_OFF;
            actionConfigList.clear();
            actionConfigBuilder.clear();
            actionConfigBuilder.setServiceType(ServiceType.POWER_STATE_SERVICE);
            actionConfigBuilder.setServiceAttribute(serviceJSonProcessor.serialize(POWER_STATE_OFF));
            actionConfigBuilder.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(POWER_STATE_OFF));
            actionConfigBuilder.setUnitId(Registries.getLocationRegistry().getRootLocationConfig().getId());
            actionConfigList.add(actionConfigBuilder.build());
            sceneConfig = SceneConfig.newBuilder().addAllActionConfig(actionConfigList).build();
            unitConfig = UnitConfig.newBuilder().setLabel(label).setType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            unitRegistry.registerUnitConfig(unitConfig).get();

        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            throw new CouldNotPerformException("Could not register scene!", ex);
        }
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {

    }

    /**
     * Test triggering a scene per remote.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testTriggerScenePerRemote() throws Exception {
        System.out.println("testTriggerScenePerRemote");

        activateScene(SCENE_TEST);

        powerStateServiceRemote.requestData().get();
        colorStateServiceRemote.requestData().get();

        assertEquals("PowerState has not been updated by scene!", POWER_ON, powerStateServiceRemote.getPowerState().getValue());
        // the colorStateServiceRemote computes an average in the rgb space which is why the values have to be compared with a tolerance
        assertEquals("Brightness has not been updated by scene!", COLOR_VALUE.getBrightness(), colorStateServiceRemote.getColorState().getColor().getHsbColor().getBrightness(), 0.5);
        assertEquals("Hue has not been updated by scene!", COLOR_VALUE.getHue(), colorStateServiceRemote.getColorState().getColor().getHsbColor().getHue(), 0.5);
        assertEquals("Saturation has not been updated by scene!", COLOR_VALUE.getSaturation(), colorStateServiceRemote.getColorState().getColor().getHsbColor().getSaturation(), 0.5);
    }

    /**
     * Test triggering a scene with an action regarding a location per remote.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testTriggerSceneWithLocationActionPerRemote() throws Exception {
        System.out.println("testTriggerSceneWithLocationActionPerRemote");

        LocationRemote locationRemote = Units.getUnit(Registries.getLocationRegistry().getRootLocationConfig(), true, LocationRemote.class);
        assertTrue("LocationState has the correct temperature to begin with!", locationRemote.getTargetTemperatureState().getTemperature() != TEMPERATURE);

        activateScene(SCENE_ROOT_LOCATION);
        locationRemote.requestData().get();

        while (locationRemote.getTargetTemperatureState().getTemperature() != TEMPERATURE) {
            System.out.println("locationTemperature[" + locationRemote.getTargetTemperatureState().getTemperature() + "] differs!");
            Thread.sleep(50);
        }

        assertEquals("TemperatureState has not been updated in location by scene!", TEMPERATURE, locationRemote.getTargetTemperatureState().getTemperature(), 0.1);
    }

    /**
     * Test triggering a scene with an action regarding a location per remote and check if the change has affected units within the location.
     *
     * @throws Exception
     */
    @Test(timeout = 60000)
    public void testTriggerSceneWithAllDevicesOfLocationActionPerRemoteAndVerifiesUnitModification() throws Exception {
        System.out.println("testTriggerSceneWithLocationActionPerRemoteAndVerifiesUnitModification");

        LightRemote internalLight = Units.getUnitByLabel(MockRegistry.LIGHT_LABEL, true, Units.LIGHT);
        PowerSwitchRemote internalPowerSwitch = Units.getUnitByLabel(MockRegistry.POWER_SWITCH_LABEL, true, Units.POWER_SWITCH);

        internalLight.setPowerState(POWER_ON).get();
        internalPowerSwitch.setPowerState(POWER_ON).get();

        internalLight.requestData().get();
        internalPowerSwitch.requestData().get();
        assertTrue("internalLight has not switched off!", internalLight.getPowerState().getValue() == POWER_ON);
        assertTrue("internalPowerSwitch has not switched off!", internalPowerSwitch.getPowerState().getValue() == POWER_ON);

        LocationRemote locationRemote = Units.getUnit(Registries.getLocationRegistry().getRootLocationConfig(), true, LocationRemote.class);
        locationRemote.setPowerState(POWER_OFF).get();

        internalLight.requestData().get();
        internalPowerSwitch.requestData().get();
        assertTrue("internalLight has not switched off!", internalLight.getPowerState().getValue() == POWER_OFF);
        assertTrue("internalPowerSwitch has not switched off!", internalPowerSwitch.getPowerState().getValue() == POWER_OFF);

        int TEST_ITERATIONS = 10;
        for (int i = 0; i <= TEST_ITERATIONS; i++) {
            activateScene(SCENE_ROOT_LOCATION_ALL_DEVICES_ON);
            while (locationRemote.getPowerState().getValue() != POWER_ON) {
                System.out.println("location was not yet switched " + POWER_ON);
                Thread.sleep(100);
                locationRemote.requestData();
            }
            internalLight.requestData().get();
            internalPowerSwitch.requestData().get();
            assertTrue("internalLight has not switched on!", internalLight.getPowerState().getValue() == POWER_ON);
            assertTrue("internalPowerSwitch has not switched on!", internalPowerSwitch.getPowerState().getValue() == POWER_ON);
            Thread.sleep(100);
            activateScene(SCENE_ROOT_LOCATION_ALL_DEVICES_OFF);
            while (locationRemote.getPowerState().getValue() != POWER_OFF) {
                System.out.println("location was not yet switched " + POWER_OFF);
                Thread.sleep(100);
                locationRemote.requestData();
            }
            internalLight.requestData().get();
            internalPowerSwitch.requestData().get();
            assertTrue("internalLight has not switched off!", internalLight.getPowerState().getValue() == POWER_OFF);
            assertTrue("internalPowerSwitch has not switched off!", internalPowerSwitch.getPowerState().getValue() == POWER_OFF);

            System.out.println("=== " + (int) (((double) i / (double) TEST_ITERATIONS) * 100d) + "% passed with iteration " + i + " of location on off test.");
            Thread.sleep(1000);
        }
    }
    
    /**
     * Test triggering a scene with an action regarding a location per remote and check if the change has affected units within the location.
     *
     * @throws Exception
     */
    @Test(timeout = 60000)
    public void testTriggerSceneWithLocationActionPerRemoteAndVerifiesUnitModification() throws Exception {
        System.out.println("testTriggerSceneWithLocationActionPerRemoteAndVerifiesUnitModification");

        LightRemote internalLight = Units.getUnitByLabel(MockRegistry.LIGHT_LABEL, true, Units.LIGHT);
        PowerSwitchRemote internalPowerSwitch = Units.getUnitByLabel(MockRegistry.POWER_SWITCH_LABEL, true, Units.POWER_SWITCH);

        internalLight.setPowerState(POWER_ON).get();
        internalPowerSwitch.setPowerState(POWER_ON).get();

        internalLight.requestData().get();
        internalPowerSwitch.requestData().get();
        assertTrue("internalLight has not switched off!", internalLight.getPowerState().getValue() == POWER_ON);
        assertTrue("internalPowerSwitch has not switched off!", internalPowerSwitch.getPowerState().getValue() == POWER_ON);

        LocationRemote locationRemote = Units.getUnit(Registries.getLocationRegistry().getRootLocationConfig(), true, LocationRemote.class);
        locationRemote.setPowerState(POWER_OFF).get();

        internalLight.requestData().get();
        internalPowerSwitch.requestData().get();
        assertTrue("internalLight has not switched off!", internalLight.getPowerState().getValue() == POWER_OFF);
        assertTrue("internalPowerSwitch has not switched off!", internalPowerSwitch.getPowerState().getValue() == POWER_OFF);

        int TEST_ITERATIONS = 10;
        for (int i = 0; i <= TEST_ITERATIONS; i++) {
            activateScene(SCENE_ROOT_LOCATION_ON);
            while (locationRemote.getPowerState().getValue() != POWER_ON) {
                System.out.println("location was not yet switched " + POWER_ON);
                Thread.sleep(100);
                locationRemote.requestData();
            }
            internalLight.requestData().get();
            internalPowerSwitch.requestData().get();
            assertTrue("internalLight has not switched on!", internalLight.getPowerState().getValue() == POWER_ON);
            assertTrue("internalPowerSwitch has not switched on!", internalPowerSwitch.getPowerState().getValue() == POWER_ON);
            Thread.sleep(100);
            activateScene(SCENE_ROOT_LOCATION_OFF);
            while (locationRemote.getPowerState().getValue() != POWER_OFF) {
                System.out.println("location was not yet switched " + POWER_OFF);
                Thread.sleep(100);
                locationRemote.requestData();
            }
            internalLight.requestData().get();
            internalPowerSwitch.requestData().get();
            assertTrue("internalLight has not switched off!", internalLight.getPowerState().getValue() == POWER_OFF);
            assertTrue("internalPowerSwitch has not switched off!", internalPowerSwitch.getPowerState().getValue() == POWER_OFF);

            System.out.println("=== " + (int) (((double) i / (double) TEST_ITERATIONS) * 100d) + "% passed with iteration " + i + " of location on off test.");
            Thread.sleep(1000);
        }
    }

    public void activateScene(String sceneLabel) throws CouldNotPerformException, InterruptedException, ExecutionException {
        SceneRemote sceneRemote = Units.getUnitByLabel(sceneLabel, true, Units.SCENE);
        sceneRemote.addDataObserver(notifyChangeObserver);
        sceneRemote.setActivationState(ACTIVATE).get();
        waitForSceneExecution(sceneRemote);
        sceneRemote.requestData().get();
        assertEquals("Scene has not been deactivated after execution!", ActivationState.State.DEACTIVE, sceneRemote.getActivationState().getValue());
        sceneRemote.removeDataObserver(notifyChangeObserver);
    }

    final SyncObject LOCK = new SyncObject("waitForSceneExecution");
    final Observer notifyChangeObserver = new Observer() {

        @Override
        public void update(Observable source, Object data) throws Exception {
            synchronized (LOCK) {
                LOCK.notifyAll();
            }
        }
    };

    private void waitForSceneExecution(SceneRemote sceneRemote) throws CouldNotPerformException {
        synchronized (LOCK) {
            try {
                while (sceneRemote.getActivationState().getValue() != ActivationState.State.DEACTIVE) {
                    LOCK.wait();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
