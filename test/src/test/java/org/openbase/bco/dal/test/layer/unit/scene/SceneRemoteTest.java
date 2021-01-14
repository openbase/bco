package org.openbase.bco.dal.test.layer.unit.scene;

/*
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.junit.*;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.dal.control.layer.unit.device.DeviceManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.location.LocationManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.scene.SceneManagerLauncher;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.provider.ColorStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.lib.state.States;
import org.openbase.bco.dal.lib.state.States.Activation;
import org.openbase.bco.dal.lib.state.States.Color;
import org.openbase.bco.dal.lib.state.States.Power;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.service.ColorStateServiceRemote;
import org.openbase.bco.dal.remote.layer.service.PowerStateServiceRemote;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.LightRemote;
import org.openbase.bco.dal.remote.layer.unit.PowerSwitchRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.layer.unit.scene.SceneRemote;
import org.openbase.bco.dal.remote.layer.unit.unitgroup.UnitGroupRemote;
import org.openbase.bco.dal.test.AbstractBCOTest;
import org.openbase.bco.dal.visual.action.BCOActionInspector;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.bco.registry.remote.session.BCOSessionImpl;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPLogLevel;
import org.openbase.jps.preset.JPLogLevel.LogLevel;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.action.ActionReferenceType.ActionReference;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActionStateType;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState.State;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.scene.SceneConfigType.SceneConfig;
import org.openbase.type.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;
import org.openbase.type.language.LabelType.Label;
import org.openbase.type.spatial.PlacementConfigType.PlacementConfig;
import org.openbase.type.vision.ColorType;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneRemoteTest extends AbstractBCOTest {

    public static final String SCENE_TEST = "testScene";
    public static final String SCENE_ROOT_LOCATION = "locationTestScene";
    public static final String SCENE_ROOT_LOCATION_ALL_DEVICES_OFF = "locationDevicesOffTestScene";
    public static final String SCENE_ROOT_LOCATION_ALL_DEVICES_ON = "locationDevicesOnTestScene";
    public static final String SCENE_ROOT_LOCATION_OFF = "locationOffTestScene";
    public static final String SCENE_ROOT_LOCATION_ON = "locationOnTestScene";
    public static final String SCENE_RED = "RedTestScene";
    public static final String SCENE_BLUE = "BlueTestScene";
    public static final String SCENE_GROUP = "GroupTriggerScene";
    public static final String COLORABLE_LIGHT_GROUP = "AllColorableLights";

    public static final ActionParameter SCENE_ACTION_PARAM = ActionParameter.newBuilder().setExecutionTimePeriod(TimeUnit.SECONDS.toMicros(10)).build();

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SceneRemoteTest.class);
    private static final PowerState.State POWER_ON = PowerState.State.ON;
    private static final PowerState.State POWER_OFF = PowerState.State.OFF;
    private static final PowerState POWER_STATE_ON = PowerState.newBuilder().setValue(POWER_ON).build();
    private static final PowerState POWER_STATE_OFF = PowerState.newBuilder().setValue(POWER_OFF).build();
    private static final HSBColor COLOR_VALUE = HSBColor.newBuilder().setHue(10).setSaturation(.9d).setBrightness(1d).build();
    private static final HSBColor GROUP_COLOR_VALUE = HSBColor.newBuilder().setHue(110).setSaturation(.55d).setBrightness(.95d).build();
    private static final double TEMPERATURE = 21.3;
    private static SceneManagerLauncher sceneManagerLauncher;
    private static DeviceManagerLauncher deviceManagerLauncher;
    private static LocationManagerLauncher locationManagerLauncher;
    private static PowerStateServiceRemote powerStateServiceRemote;
    private static ColorStateServiceRemote colorStateServiceRemote;

    public SceneRemoteTest() {
        // uncomment to enable debug mode
//         JPService.registerProperty(JPDebugMode.class, true);
//         JPService.registerProperty(JPLogLevel.class, LogLevel.DEBUG);

        // uncomment to visualize action inspector during tests
//        String[] args = {};
//        new Thread(() -> {
//            try {
//                Registries.waitForData();
//                BCOActionInspector.main(args);
//            } catch (CouldNotPerformException e) {
//                e.printStackTrace();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }).start();
//
//        // wait some time so we can select the unit to observe via the action inspector.
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            AbstractBCOTest.setUpClass();
            //JPService.registerProperty(JPLogLevel.class, LogLevel.DEBUG);
            JPService.setupJUnitTestMode();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch().get();

            locationManagerLauncher = new LocationManagerLauncher();
            locationManagerLauncher.launch().get();
            locationManagerLauncher.getLaunchable();

            sceneManagerLauncher = new SceneManagerLauncher();
            sceneManagerLauncher.launch().get();

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
    public static void tearDownClass() throws Throwable {
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
            if (locationManagerLauncher != null) {
                locationManagerLauncher.shutdown();
            }
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }

            AbstractBCOTest.tearDownClass();
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    private static void registerScenes() throws CouldNotPerformException {
        try {
            ServiceJSonProcessor serviceJSonProcessor = new ServiceJSonProcessor();

            List<ServiceStateDescription> serviceStateDescriptionList = new ArrayList<>();

            ServiceStateDescription.Builder serviceStateDescription = ServiceStateDescription.newBuilder();

            ColorType.Color color = ColorType.Color.newBuilder().setType(ColorType.Color.Type.HSB).setHsbColor(COLOR_VALUE).build();
            ColorState colorState = ColorState.newBuilder().setColor(color).build();
            serviceStateDescription.setServiceType(ServiceType.COLOR_STATE_SERVICE);
            serviceStateDescription.setServiceState(serviceJSonProcessor.serialize(colorState));
            serviceStateDescription.setServiceStateClassName(Services.getServiceStateClassName(colorState));
            for (UnitConfig unitConfig : Registries.getUnitRegistry().getDalUnitConfigs()) {

                if (unitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    continue;
                }

                if (unitConfig.getUnitType() == UnitType.COLORABLE_LIGHT) {
                    serviceStateDescription.clearUnitId();
                    serviceStateDescription.setUnitId(unitConfig.getId());
                    serviceStateDescriptionList.add(serviceStateDescription.build());
                    colorStateServiceRemote.init(unitConfig);
                }
            }

            PowerState powerState = PowerState.newBuilder().setValue(POWER_ON).build();
            serviceStateDescription.setServiceType(ServiceType.POWER_STATE_SERVICE);
            serviceStateDescription.setServiceState(serviceJSonProcessor.serialize(powerState));
            serviceStateDescription.setServiceStateClassName(Services.getServiceStateClassName(powerState));
            for (UnitConfig unitConfig : Registries.getUnitRegistry().getDalUnitConfigs()) {
                if (unitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    continue;
                }

                if (Registries.getTemplateRegistry().getSubUnitTypes(UnitType.LIGHT).contains(unitConfig.getUnitType()) || unitConfig.getUnitType() == UnitType.LIGHT || unitConfig.getUnitType() == UnitType.POWER_SWITCH) {
                    serviceStateDescription.clearUnitId();
                    serviceStateDescription.setUnitId(unitConfig.getId());
                    serviceStateDescriptionList.add(serviceStateDescription.build());
                    powerStateServiceRemote.init(unitConfig);
                }
            }

            String label = SCENE_TEST;
            PlacementConfig placementConfig = PlacementConfig.newBuilder().setLocationId(Registries.getUnitRegistry().getRootLocationConfig().getId()).build();
            SceneConfig sceneConfig = SceneConfig.newBuilder().addAllOptionalServiceStateDescription(serviceStateDescriptionList).build();
            UnitConfig unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();

            serviceStateDescriptionList.clear();
            TemperatureState temperatureState = TemperatureState.newBuilder().setTemperature(TEMPERATURE).setTemperatureDataUnit(TemperatureState.DataUnit.CELSIUS).build();
            serviceStateDescription.setServiceType(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE);
            serviceStateDescription.setServiceState(serviceJSonProcessor.serialize(temperatureState));
            serviceStateDescription.setServiceStateClassName(Services.getServiceStateClassName(temperatureState));
            serviceStateDescription.setUnitId(Registries.getUnitRegistry().getRootLocationConfig().getId());
            serviceStateDescriptionList.add(serviceStateDescription.build());

            label = SCENE_ROOT_LOCATION;
            sceneConfig = SceneConfig.newBuilder().addAllRequiredServiceStateDescription(serviceStateDescriptionList).build();
            unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();

            label = SCENE_ROOT_LOCATION_ALL_DEVICES_ON;
            LocationRemote locationRemote = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), false, LocationRemote.class);
            locationRemote.waitForData();
            locationRemote.setPowerState(POWER_ON).get();
            locationRemote.requestData().get();
            locationRemote.getChildLocationList(true);
            sceneConfig = SceneConfig.newBuilder().addAllRequiredServiceStateDescription(locationRemote.recordSnapshot().get().getServiceStateDescriptionList()).build();
            unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();

            label = SCENE_ROOT_LOCATION_ALL_DEVICES_OFF;
            locationRemote.setPowerState(POWER_OFF).get();
            locationRemote.requestData().get();
            sceneConfig = SceneConfig.newBuilder().addAllRequiredServiceStateDescription(locationRemote.recordSnapshot().get().getServiceStateDescriptionList()).build();
            unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();

            label = SCENE_ROOT_LOCATION_ON;
            serviceStateDescriptionList.clear();
            serviceStateDescription.clear();
            serviceStateDescription.setServiceType(ServiceType.POWER_STATE_SERVICE);
            serviceStateDescription.setServiceState(serviceJSonProcessor.serialize(POWER_STATE_ON));
            serviceStateDescription.setServiceStateClassName(Services.getServiceStateClassName(POWER_STATE_ON));
            serviceStateDescription.setUnitId(Registries.getUnitRegistry().getRootLocationConfig().getId());
            serviceStateDescriptionList.add(serviceStateDescription.build());
            sceneConfig = SceneConfig.newBuilder().addAllRequiredServiceStateDescription(serviceStateDescriptionList).build();
            unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();

            label = SCENE_ROOT_LOCATION_OFF;
            serviceStateDescriptionList.clear();
            serviceStateDescription.clear();
            serviceStateDescription.setServiceType(ServiceType.POWER_STATE_SERVICE);
            serviceStateDescription.setServiceState(serviceJSonProcessor.serialize(POWER_STATE_OFF));
            serviceStateDescription.setServiceStateClassName(Services.getServiceStateClassName(POWER_STATE_OFF));
            serviceStateDescription.setUnitId(Registries.getUnitRegistry().getRootLocationConfig().getId());
            serviceStateDescriptionList.add(serviceStateDescription.build());
            sceneConfig = SceneConfig.newBuilder().addAllRequiredServiceStateDescription(serviceStateDescriptionList).build();
            unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();

            // Register colorful scenes
            label = SCENE_BLUE;
            serviceStateDescriptionList.clear();
            serviceStateDescription.clear();
            serviceStateDescription.setServiceType(ServiceType.COLOR_STATE_SERVICE);
            serviceStateDescription.setServiceState(serviceJSonProcessor.serialize(Color.BLUE));
            serviceStateDescription.setServiceStateClassName(Services.getServiceStateClassName(Color.BLUE));
            serviceStateDescription.setUnitId(Registries.getUnitRegistry().getRootLocationConfig().getId());
            serviceStateDescriptionList.add(serviceStateDescription.build());
            sceneConfig = SceneConfig.newBuilder().addAllOptionalServiceStateDescription(serviceStateDescriptionList).build();
            unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();

            label = SCENE_RED;
            serviceStateDescriptionList.clear();
            serviceStateDescription.clear();
            serviceStateDescription.setServiceType(ServiceType.COLOR_STATE_SERVICE);
            serviceStateDescription.setServiceState(serviceJSonProcessor.serialize(Color.RED));
            serviceStateDescription.setServiceStateClassName(Services.getServiceStateClassName(Color.RED));
            serviceStateDescription.setUnitId(Registries.getUnitRegistry().getRootLocationConfig().getId());
            serviceStateDescriptionList.add(serviceStateDescription.build());
            sceneConfig = SceneConfig.newBuilder().addAllOptionalServiceStateDescription(serviceStateDescriptionList).build();
            unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();

            // Register Scene which changes a unitGroup
            String unitGroupId = registerUnitGroup();
            color = ColorType.Color.newBuilder().setType(ColorType.Color.Type.HSB).setHsbColor(GROUP_COLOR_VALUE).build();
            colorState = ColorState.newBuilder().setColor(color).build();

            label = SCENE_GROUP;
            serviceStateDescriptionList.clear();
            serviceStateDescription.clear();
            serviceStateDescription.setServiceType(ServiceType.COLOR_STATE_SERVICE);
            serviceStateDescription.setServiceState(serviceJSonProcessor.serialize(colorState));
            serviceStateDescription.setServiceStateClassName(Services.getServiceStateClassName(colorState));
            serviceStateDescription.setUnitId(unitGroupId);
            serviceStateDescriptionList.add(serviceStateDescription.build());
            sceneConfig = SceneConfig.newBuilder().addAllRequiredServiceStateDescription(serviceStateDescriptionList).build();
            unitConfig = UnitConfig.newBuilder().setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, label)).setUnitType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            throw new CouldNotPerformException("Could not register scene!", ex);
        }
    }

    private static String registerUnitGroup() throws CouldNotPerformException {
        try {
            UnitConfig.Builder unitConfig = UnitConfig.newBuilder().setUnitType(UnitType.UNIT_GROUP).setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, COLORABLE_LIGHT_GROUP));
            UnitGroupConfig.Builder unitGroup = unitConfig.getUnitGroupConfigBuilder();

            for (UnitConfig unit : Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.COLORABLE_LIGHT)) {
                // filter disabled units
                if (unit.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    continue;
                }
                unitGroup.addMemberId(unit.getId());
            }

            return Registries.getUnitRegistry().registerUnitConfig(unitConfig.build()).get().getId();
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            throw new CouldNotPerformException("Could not register unit groups!", ex);
        }
    }

    /**
     * Test triggering a scene per remote.
     *
     * @throws Exception
     */
    @Test(timeout = 30000)
    public void testTriggerScenePerRemote() throws Exception {
        System.out.println("testTriggerScenePerRemote");

        SceneRemote sceneRemote = Units.getUnitsByLabel(SCENE_TEST, true, Units.SCENE).get(0);
        waitForExecution(sceneRemote.setActivationState(State.ACTIVE));

        powerStateServiceRemote.requestData().get();
        colorStateServiceRemote.requestData().get();

        assertEquals("PowerState has not been updated by scene!", POWER_ON, powerStateServiceRemote.getPowerState().getValue());

        // the colorStateServiceRemote computes an average in the rgb space which is why the values have to be compared with a tolerance
        assertEquals("Brightness has not been updated by scene!", COLOR_VALUE.getBrightness(), colorStateServiceRemote.getColorState().getColor().getHsbColor().getBrightness(), 0.01);
        assertEquals("Hue has not been updated by scene!", COLOR_VALUE.getHue(), colorStateServiceRemote.getColorState().getColor().getHsbColor().getHue(), 0.2);
        assertEquals("Saturation has not been updated by scene!", COLOR_VALUE.getSaturation(), colorStateServiceRemote.getColorState().getColor().getHsbColor().getSaturation(), 0.01);
    }

    /**
     * Test triggering a scene with an action regarding a location per remote.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testTriggerSceneWithLocationActionPerRemote() throws Exception {
        System.out.println("testTriggerSceneWithLocationActionPerRemote");

        LocationRemote locationRemote = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, LocationRemote.class);
        assertTrue("LocationState has the correct temperature to begin with!", locationRemote.getTargetTemperatureState().getTemperature() != TEMPERATURE);

        final SceneRemote sceneRemote = Units.getUnitsByLabel(SCENE_ROOT_LOCATION, true, Units.SCENE).get(0);
        waitForExecution(sceneRemote.setActivationState(State.ACTIVE));
        locationRemote.requestData().get();

        while (locationRemote.getTargetTemperatureState().getTemperature() != TEMPERATURE) {
            System.out.println("locationTemperature[" + locationRemote.getTargetTemperatureState().getTemperature() + "] differs!");
            Thread.sleep(50);
        }

        assertEquals("TemperatureState has not been updated in location by scene!", TEMPERATURE, locationRemote.getTargetTemperatureState().getTemperature(), 0.1);
    }

    /**
     * Test triggering a unit group with a scene.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testTriggerUnitGroupByScene() throws Exception {
        System.out.println("testTriggerUnitGroupByScene");

        final UnitGroupRemote unitGroupRemote = Units.getUnitsByLabel(COLORABLE_LIGHT_GROUP, true, UnitGroupRemote.class).get(0);
        final SceneRemote sceneRemote = Units.getUnitsByLabel(SCENE_GROUP, true, Units.SCENE).get(0);

        final List<ColorableLightRemote> colorableLightRemotes = new ArrayList<>();
        for (String memberId : unitGroupRemote.getConfig().getUnitGroupConfig().getMemberIdList()) {
            colorableLightRemotes.add(Units.getUnit(memberId, true, ColorableLightRemote.class));
        }
        waitForExecution(sceneRemote.setActivationState(State.ACTIVE));

        for (ColorableLightRemote colorableLightRemote : colorableLightRemotes) {
            assertEquals("ColorState has not been set for light[" + colorableLightRemote.getLabel() + "]", GROUP_COLOR_VALUE, colorableLightRemote.getColorState().getColor().getHsbColor());
        }

        while (!(unitGroupRemote.getColorState().getColor().getHsbColor().getBrightness() > GROUP_COLOR_VALUE.getBrightness() - 0.001 && unitGroupRemote.getColorState().getColor().getHsbColor().getBrightness() < GROUP_COLOR_VALUE.getBrightness() + 0.001)) {
            Thread.sleep(10);
        }

        unitGroupRemote.requestData().get();

        // for the group the values can be slightly modified because of computing averages
        assertEquals("Brightness in unitGroupRemote has not been set", GROUP_COLOR_VALUE.getBrightness(), unitGroupRemote.getColorState().getColor().getHsbColor().getBrightness(), 0.01);
        assertEquals("Hue in unitGroupRemote has not been set", GROUP_COLOR_VALUE.getHue(), unitGroupRemote.getColorState().getColor().getHsbColor().getHue(), 0.01);
        assertEquals("Saturation in unitGroupRemote has not been set", GROUP_COLOR_VALUE.getSaturation(), unitGroupRemote.getColorState().getColor().getHsbColor().getSaturation(), 0.01);
    }

    /**
     * Test triggering a scene with an action regarding a location per remote and check if the change has affected units within the location.
     *
     * @throws Exception
     */
    @Test(timeout = 20000)
    public void testTriggerSceneWithAllDevicesOfLocationActionPerRemoteAndVerifiesUnitModification() throws Exception {
        System.out.println("testTriggerSceneWithAllDevicesOfLocationActionPerRemoteAndVerifiesUnitModification");

        LightRemote internalLight = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.LIGHT), true, Units.LIGHT);
        PowerSwitchRemote internalPowerSwitch = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.POWER_SWITCH), true, Units.POWER_SWITCH);

        waitForExecution(internalLight.setPowerState(POWER_ON));
        waitForExecution(internalPowerSwitch.setPowerState(POWER_ON));

        assertTrue("internalLight has not switched on!", internalLight.getPowerState().getValue() == POWER_ON);
        assertTrue("internalPowerSwitch has not switched on!", internalPowerSwitch.getPowerState().getValue() == POWER_ON);

        LocationRemote locationRemote = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, LocationRemote.class);
        waitForExecution(locationRemote.setPowerState(POWER_OFF));

        internalLight.requestData().get();
        internalPowerSwitch.requestData().get();
        assertTrue("internalLight has not switched off!", internalLight.getPowerState().getValue() == POWER_OFF);
        assertTrue("internalPowerSwitch has not switched off!", internalPowerSwitch.getPowerState().getValue() == POWER_OFF);

        final SceneRemote sceneRemoteDevicesOn = Units.getUnitsByLabel(SCENE_ROOT_LOCATION_ALL_DEVICES_ON, true, Units.SCENE).get(0);
        final SceneRemote sceneRemoteDevicesOff = Units.getUnitsByLabel(SCENE_ROOT_LOCATION_ALL_DEVICES_OFF, true, Units.SCENE).get(0);

        waitForExecution(sceneRemoteDevicesOn.setActivationState(State.INACTIVE, SCENE_ACTION_PARAM));
        waitForExecution(sceneRemoteDevicesOff.setActivationState(State.INACTIVE, SCENE_ACTION_PARAM));

        int TEST_ITERATIONS = 3;
        System.out.println("----------------- start iteration");
        for (int i = 0; i <= TEST_ITERATIONS; i++) {

            System.out.println("----------------- set on");
            waitForExecution(sceneRemoteDevicesOn.setActivationState(State.ACTIVE, SCENE_ACTION_PARAM));
            internalLight.requestData().get();
            internalPowerSwitch.requestData().get();
            assertTrue("internalLight has not switched on!", internalLight.getPowerState().getValue() == POWER_ON);
            assertTrue("internalPowerSwitch has not switched on!", internalPowerSwitch.getPowerState().getValue() == POWER_ON);
            assertEquals("Devices on scene is not active", State.ACTIVE, sceneRemoteDevicesOn.getActivationState().getValue());
            assertEquals("Devices off scene is not inactive", State.INACTIVE, sceneRemoteDevicesOff.getActivationState().getValue());

            System.out.println("----------------- set off");
            waitForExecution(sceneRemoteDevicesOff.setActivationState(State.ACTIVE, SCENE_ACTION_PARAM));
            internalLight.requestData().get();
            internalPowerSwitch.requestData().get();

            assertTrue("internalLight has not switched off at interaction "+i, internalLight.getPowerState().getValue() == POWER_OFF);
            assertTrue("internalPowerSwitch has not switched off at interaction "+i, internalPowerSwitch.getPowerState().getValue() == POWER_OFF);

            assertEquals("Devices off scene is not active at interaction "+i, State.ACTIVE, sceneRemoteDevicesOff.getActivationState().getValue());
            assertEquals("Devices on scene is not inactive at interaction "+i, State.INACTIVE, sceneRemoteDevicesOn.getActivationState().getValue());

            System.out.println("=== " + (int) (((double) i / (double) TEST_ITERATIONS) * 100d) + "% passed with iteration " + i + " of location on off test.");
        }
    }

    /**
     * Test triggering a scene with an action regarding a location per remote and check if the change has affected units within the location.
     *
     * @throws Exception
     */
    @Test(timeout = 20000)
    public void testTriggerSceneWithLocationActionPerRemoteAndVerifiesUnitModification() throws Exception {
        System.out.println("testTriggerSceneWithLocationActionPerRemoteAndVerifiesUnitModification");

        LightRemote internalLight = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.LIGHT), true, Units.LIGHT);
        PowerSwitchRemote internalPowerSwitch = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.POWER_SWITCH), true, Units.POWER_SWITCH);

        waitForExecution(internalLight.setPowerState(POWER_ON));
        waitForExecution(internalPowerSwitch.setPowerState(POWER_ON));

        internalLight.requestData().get();
        internalPowerSwitch.requestData().get();
        assertTrue("internalLight has not switched on!", internalLight.getPowerState().getValue() == POWER_ON);
        assertTrue("internalPowerSwitch has not switched on!", internalPowerSwitch.getPowerState().getValue() == POWER_ON);

        LocationRemote locationRemote = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, LocationRemote.class);
        waitForExecution(locationRemote.setPowerState(POWER_OFF));

        internalLight.requestData().get();
        internalPowerSwitch.requestData().get();
        assertTrue("internalLight has not switched off!", internalLight.getPowerState().getValue() == POWER_OFF);
        assertTrue("internalPowerSwitch has not switched off!", internalPowerSwitch.getPowerState().getValue() == POWER_OFF);

        final SceneRemote sceneRemoteOn = Units.getUnitsByLabel(SCENE_ROOT_LOCATION_ON, true, Units.SCENE).get(0);
        final SceneRemote sceneRemoteOff = Units.getUnitsByLabel(SCENE_ROOT_LOCATION_OFF, true, Units.SCENE).get(0);

        waitForExecution(sceneRemoteOn.setActivationState(State.INACTIVE, SCENE_ACTION_PARAM));
        waitForExecution(sceneRemoteOff.setActivationState(State.INACTIVE, SCENE_ACTION_PARAM));

        int TEST_ITERATIONS = 3;
        for (int i = 0; i <= TEST_ITERATIONS; i++) {
            System.out.println("Current iteration: " + i);
            waitForExecution(sceneRemoteOn.setActivationState(State.ACTIVE, SCENE_ACTION_PARAM));

            internalLight.requestData().get();
            internalPowerSwitch.requestData().get();
            assertTrue("internalLight has not switched on!", internalLight.getPowerState().getValue() == POWER_ON);
            assertTrue("internalPowerSwitch has not switched on!", internalPowerSwitch.getPowerState().getValue() == POWER_ON);
            assertEquals("Location on scene is not active", State.ACTIVE, sceneRemoteOn.getActivationState().getValue());
            assertEquals("Location off scene is not inactive", State.INACTIVE, sceneRemoteOff.getActivationState().getValue());

            waitForExecution(sceneRemoteOff.setActivationState(State.ACTIVE, SCENE_ACTION_PARAM));
            internalLight.requestData().get();
            internalPowerSwitch.requestData().get();
            assertTrue("internalLight has not switched off!", internalLight.getPowerState().getValue() == POWER_OFF);
            assertTrue("internalPowerSwitch has not switched off!", internalPowerSwitch.getPowerState().getValue() == POWER_OFF);
            assertEquals("Location on scene is not inactive", State.INACTIVE, sceneRemoteOn.getActivationState().getValue());
            assertEquals("Location off scene is not active", State.ACTIVE, sceneRemoteOff.getActivationState().getValue());

            System.out.println("=== " + (int) (((double) i / (double) TEST_ITERATIONS) * 100d) + "% passed with iteration " + i + " of location on off test.");
        }
    }

    /**
     * Test triggering a scene which only refer to intermediary actions and makes sure all impacted action are canceled when the scene gets deactivated.
     *
     * @throws Exception
     */
    @Test(timeout = 20000)
    public void testTestIntermediaryActionCancellationOnSceneDeactivation() throws Exception {
        System.out.println("testTestIntermediaryActionCancellationOnSceneDeactivation");

        LightRemote internalLight = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.LIGHT), true, Units.LIGHT);
        PowerSwitchRemote internalPowerSwitch = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.POWER_SWITCH), true, Units.POWER_SWITCH);

        waitForExecution(internalLight.setPowerState(POWER_ON));
        waitForExecution(internalPowerSwitch.setPowerState(POWER_ON));

        internalLight.requestData().get();
        internalPowerSwitch.requestData().get();

        assertTrue("internalLight has not switched on!", internalLight.getPowerState().getValue() == POWER_ON);
        assertTrue("internalPowerSwitch has not switched on!", internalPowerSwitch.getPowerState().getValue() == POWER_ON);

        final SceneRemote sceneRemoteOff = Units.getUnitsByLabel(SCENE_ROOT_LOCATION_OFF, true, Units.SCENE).get(0);
        final RemoteAction sceneAction = waitForExecution(sceneRemoteOff.setActivationState(State.ACTIVE, SCENE_ACTION_PARAM));

        Assert.assertEquals("Scene action is marked as intermediary one!", false, sceneAction.getActionDescription().getIntermediary());

        Assert.assertNotEquals("Scene did not impact any action!", 0, sceneAction.getActionDescription().getActionImpactList().size());


        // resolve action impact
        List<RemoteAction> actionImpactList = new ArrayList<>();
        for (ActionReference actionReference : sceneAction.getActionDescription().getActionImpactList()) {
            System.out.println("action impact of scene: " + Units.getUnit(actionReference.getServiceStateDescription().getUnitId(), true).getLabel());
//            actionImpactList.add(new RemoteAction(Units.getUnit(actionReference.getServiceStateDescription().getUnitId(), true).resolveRelatedActionDescription(sceneAction.getActionId())));
        }

        for (RemoteAction actionImpact : actionImpactList) {
            actionImpact.waitForRegistration();
            Assert.assertEquals("Impacted action not executing!", ActionStateType.ActionState.State.EXECUTING, actionImpact.getActionState());
        }

        assertTrue("internalLight has not switched off by scene!", internalLight.getPowerState().getValue() == POWER_OFF);
        assertTrue("internalPowerSwitch has not switched off by scene!", internalPowerSwitch.getPowerState().getValue() == POWER_OFF);

        waitForExecution(sceneRemoteOff.setActivationState(State.INACTIVE, SCENE_ACTION_PARAM));

        for (RemoteAction actionImpact : actionImpactList) {
            actionImpact.waitUntilDone();
            Assert.assertEquals("Impacted " + actionImpact + " not canceled!", ActionStateType.ActionState.State.CANCELED, actionImpact.getActionState());
        }

        for (ActionDescriptionType.ActionDescription actionDescription : internalLight.getActionList()) {

            // filter termination action
            if (actionDescription.getPriority() == Priority.TERMINATION) {
                continue;
            }

            System.out.println("Action on stack: " + actionDescription.getActionState().getValue().name() + " = " + MultiLanguageTextProcessor.getBestMatch(actionDescription.getDescription()));
            assertTrue("internalLight has an ongoing action on its stack!", new RemoteAction(actionDescription).isDone());
        }

        for (ActionDescriptionType.ActionDescription actionDescription : internalPowerSwitch.getActionList()) {

            // filter termination action
            if (actionDescription.getPriority() == Priority.TERMINATION) {
                continue;
            }

            System.out.println("Action on stack: " + actionDescription.getActionState().getValue().name() + " = " + MultiLanguageTextProcessor.getBestMatch(actionDescription.getDescription()));
            assertTrue("internalPowerSwitch has an ongoing action on its stack!", new RemoteAction(actionDescription).isDone());
        }
    }

    @Test(timeout = 20000)
    public void testActionCancellationViaScene() throws Exception {

        final LocationRemote rootLocationRemote = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);

        // trigger all off action to use its service description as prototype for the all off scene.
        final RemoteAction initialAllOffAction = waitForExecution(rootLocationRemote.setPowerState(Power.OFF));
        final ServiceStateDescription allOffAction = initialAllOffAction.getActionDescription().getServiceStateDescription();
        final Builder allOffSceneConfig = UnitConfig.newBuilder().setUnitType(UnitType.SCENE);

        // setup all off scene
        allOffSceneConfig.getSceneConfigBuilder().addOptionalServiceStateDescription(allOffAction);
        final UnitConfig unitConfig = Registries.getUnitRegistry().registerUnitConfig(allOffSceneConfig.build()).get();
        final SceneRemote allOffScene = Units.getUnit(unitConfig, true, Units.SCENE);

        // cancel action prototype to not interfere with further test actions.
        initialAllOffAction.cancel().get();

        // query all lights
        final List<? extends ColorableLightRemote> colorableLights = rootLocationRemote.getUnits(UnitType.COLORABLE_LIGHT, true, Units.COLORABLE_LIGHT);

        // validate that all lights are initially off
        for (ColorableLightRemote colorableLight : colorableLights) {
            colorableLight.requestData().get();
            Assert.assertEquals("Light still on!", Power.OFF.getValue(), colorableLight.getPowerState().getValue());
        }

        // switch all lights on via another authority
        final BCOSessionImpl session = new BCOSessionImpl(new SessionManager());
        session.loginUserViaUsername("admin", "admin", true);
        RemoteAction colorableLightRedRemoteAction = null;
        for (ColorableLightRemote colorableLight : colorableLights) {
            final ColorableLightRemote adminsColorableLightRemote = new ColorableLightRemote();
            adminsColorableLightRemote.setSession(session);
            adminsColorableLightRemote.init(colorableLight.getConfig());
            adminsColorableLightRemote.activate();
            adminsColorableLightRemote.waitForData();
            colorableLightRedRemoteAction = waitForExecution(adminsColorableLightRemote.setColorState(States.Color.RED, ActionParameter.newBuilder().setPriority(Priority.LOW).build()), session);
            adminsColorableLightRemote.shutdown();
        }

        Assert.assertTrue("Manual color action does not exist!", colorableLightRedRemoteAction != null);
        Assert.assertEquals("Manual color action not executing!", ActionState.State.EXECUTING, colorableLightRedRemoteAction.getActionState());

        // validate cancellation and make sure all lights are RED
        for (ColorableLightRemote colorableLight : colorableLights) {
            colorableLight.requestData().get();
            Assert.assertEquals("Color not restored!", States.Color.RED.getColor(), colorableLight.getColorState().getColor());
        }

        // switch all off via scene
        final RemoteAction allOffSceneAction = waitForExecution(allOffScene.setActivationState(Activation.ACTIVE));

        Assert.assertEquals("Manual color action not executing!", ActionState.State.SCHEDULED, colorableLightRedRemoteAction.getActionState());

        // validate all off and store responsible action
        final ArrayList<ActionDescription> allOffActionDescriptionList = new ArrayList<>();
        for (ColorableLightRemote colorableLight : colorableLights) {
            colorableLight.requestData().get();
            final PowerState powerState = colorableLight.getPowerState();
            Assert.assertEquals("Light still on!", Power.OFF.getValue(), powerState.getValue());
            allOffActionDescriptionList.add(powerState.getResponsibleAction());
        }

        // make sure each discovered action is listed as impact
        for (ActionDescription colorableLightAction : allOffActionDescriptionList) {
            boolean found = false;
            for (ActionReference actionImpact : allOffSceneAction.getActionDescription().getActionImpactList()) {
                if (colorableLightAction.getServiceStateDescription().equals(actionImpact.getServiceStateDescription())) {
                    found = true;
                }
            }
            Assert.assertTrue("Impact not registered!", found);
        }

        Assert.assertEquals("Manual color action not executing!", ActionState.State.SCHEDULED, colorableLightRedRemoteAction.getActionState());

        // cancel all off
        allOffSceneAction.cancel().get();

        colorableLightRedRemoteAction.waitForActionState(ActionState.State.EXECUTING);
        Assert.assertEquals("Manual color action not executing!", ActionState.State.EXECUTING, colorableLightRedRemoteAction.getActionState());

        // validate cancellation and make sure all lights are RED again
        for (ColorableLightRemote colorableLight : colorableLights) {
            colorableLight.requestData().get();
            Assert.assertEquals("Color not restored! Current action is: "+MultiLanguageTextProcessor.getBestMatch(colorableLight.getColorState().getResponsibleAction().getDescription()) , States.Color.RED.getColor(), colorableLight.getColorState().getColor());
        }
        session.logout();
    }


    @Test(timeout = 20000)
    public void testThatScenesDoNotInterfereEachOther() throws Exception {

        final long AGGREGATION_TIME = 50;
        final LocationRemote rootLocationRemote = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);
        final SceneRemote blueSceneRemote = Units.getUnitsByLabel(SCENE_BLUE, true, Units.SCENE).get(0);
        final SceneRemote redSceneRemote = Units.getUnitsByLabel(SCENE_RED, true, Units.SCENE).get(0);
        final List<? extends ColorableLightRemote> colorableLights = rootLocationRemote.getUnits(UnitType.COLORABLE_LIGHT, true, Units.COLORABLE_LIGHT);

        // validate that all lights are initially off
        for (ColorableLightRemote colorableLight : colorableLights) {
            colorableLight.requestData().get();
            Assert.assertEquals("Light is not off but: "+ MultiLanguageTextProcessor.getBestMatch(colorableLight.getColorState().getResponsibleAction().getDescription(), "?"), Power.OFF.getValue(), colorableLight.getPowerState().getValue());
        }

        // validate that the room is initially off
        Thread.sleep(AGGREGATION_TIME);  // let us wait some time to let the aggregation takes place.
        rootLocationRemote.requestData().get();
        Assert.assertEquals("Room is not off!", Power.OFF.getValue(), rootLocationRemote.getPowerState().getValue());

        // activate blue
        observe(blueSceneRemote.setActivationState(Activation.ACTIVE)).waitForActionState(ActionState.State.EXECUTING);

        // validate that all lights are blue
        for (ColorableLightRemote colorableLight : colorableLights) {
            colorableLight.requestData().get();
            Assert.assertEquals("Light is not blue!", Color.BLUE.getColor(), colorableLight.getColorState().getColor());
        }

        // validate the room is blue
        Thread.sleep(AGGREGATION_TIME);  // let us wait some time to let the aggregation takes place.
        rootLocationRemote.requestData().get();
        Assert.assertTrue("Scene does not apply its color, expected: " + Color.BLUE.getColor() + " but was: " + rootLocationRemote.getColorState().getColor() + "!", ColorStateProviderService.equalServiceStates(rootLocationRemote.getColorState(), Color.BLUE));

        // activate red
        observe(redSceneRemote.setActivationState(Activation.ACTIVE)).waitForActionState(ActionState.State.EXECUTING);

        // validate that all lights are red
        for (ColorableLightRemote colorableLight : colorableLights) {
            colorableLight.requestData().get();
            Assert.assertEquals("Light is not red!", Color.RED.getColor(), colorableLight.getColorState().getColor());
        }

        // validate the room is red
        Thread.sleep(AGGREGATION_TIME);  // let us wait some time to let the aggregation takes place.
        rootLocationRemote.requestData().get();
        Assert.assertTrue("Scene does not apply its color, expected: " + Color.RED.getColor() + " but was: " + rootLocationRemote.getColorState().getColor() + "!", ColorStateProviderService.equalServiceStates(rootLocationRemote.getColorState(), Color.RED));

        // deactivate blue
        observe(blueSceneRemote.setActivationState(Activation.INACTIVE)).waitForActionState(ActionState.State.EXECUTING);

        // validate that all lights are still red
        for (ColorableLightRemote colorableLight : colorableLights) {
            colorableLight.requestData().get();
            Assert.assertEquals("Light is not red!", Color.RED.getColor(), colorableLight.getColorState().getColor());
        }

        // validate the room is still red
        Thread.sleep(AGGREGATION_TIME);  // let us wait some time to let the aggregation takes place.
        rootLocationRemote.requestData().get();
        Assert.assertTrue("Scene does not apply its color, expected: " + Color.RED.getColor() + " but was: " + rootLocationRemote.getColorState().getColor() + "!", ColorStateProviderService.equalServiceStates(rootLocationRemote.getColorState(), Color.RED));

        // activate blue
        observe(blueSceneRemote.setActivationState(Activation.ACTIVE)).waitForActionState(ActionState.State.EXECUTING);

        // validate that all lights are blue
        for (ColorableLightRemote colorableLight : colorableLights) {
            colorableLight.requestData().get();
            Assert.assertEquals("Light is not blue!", Color.BLUE.getColor(), colorableLight.getColorState().getColor());
        }

        // validate the room is blue
        Thread.sleep(AGGREGATION_TIME);  // let us wait some time to let the aggregation takes place.
        rootLocationRemote.requestData().get();
        Assert.assertTrue("Scene does not apply its color, expected: " + Color.BLUE.getColor() + " but was: " + rootLocationRemote.getColorState().getColor() + "!", ColorStateProviderService.equalServiceStates(rootLocationRemote.getColorState(), Color.BLUE));

        // deactivate blue
        observe(blueSceneRemote.setActivationState(Activation.INACTIVE)).waitForActionState(ActionState.State.EXECUTING);

        // validate that all lights are red again
        for (ColorableLightRemote colorableLight : colorableLights) {
            colorableLight.requestData().get();
            Assert.assertEquals("Light is not red but: "+ MultiLanguageTextProcessor.getBestMatch(colorableLight.getColorState().getResponsibleAction().getDescription(), "?"), Color.RED.getColor(), colorableLight.getColorState().getColor());
        }

        // validate the room is red again
        Thread.sleep(AGGREGATION_TIME);  // let us wait some time to let the aggregation takes place.
        rootLocationRemote.requestData().get();
        Assert.assertTrue("Scene does not apply its color, expected: " + Color.RED.getColor() + " but was: " + rootLocationRemote.getColorState().getColor() + "!", ColorStateProviderService.equalServiceStates(rootLocationRemote.getColorState(), Color.RED));


        // deactivate red and validate the room is off
        observe(redSceneRemote.setActivationState(Activation.INACTIVE)).waitForActionState(ActionState.State.EXECUTING);

        // validate that all lights are off again
        for (ColorableLightRemote colorableLight : colorableLights) {
            colorableLight.requestData().get();
            Assert.assertEquals("Light is not off!", Power.OFF.getValue(), colorableLight.getPowerState().getValue());
        }

        // validate that the room is off
        Thread.sleep(AGGREGATION_TIME);  // let us wait some time to let the aggregation takes place.
        rootLocationRemote.requestData().get();
        Assert.assertEquals("Room is not off!", Power.OFF.getValue(), rootLocationRemote.getPowerState().getValue());

    }
}
