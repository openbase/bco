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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.remote.service.ColorStateServiceRemote;
import org.openbase.bco.dal.remote.service.PowerStateServiceRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.scene.SceneRemote;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
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
import org.openbase.jul.extension.rsb.com.jp.JPRSBTransport;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionConfigType.ActionConfig;
import rst.domotic.action.ActionPriorityType.ActionPriority;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.PowerStateType.PowerState;
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

    private static SceneManagerLauncher sceneManagerLauncher;
    private static SceneRemote sceneRemote;
    private static MockRegistry registry;
    private static UnitConfig unitSceneConfig;
    private static DeviceManagerLauncher deviceManagerLauncher;

    private static PowerStateServiceRemote powerStateServiceRemote;
    private static ColorStateServiceRemote colorStateServiceRemote;

    private static final PowerState.State powerValue = PowerState.State.ON;
    private static final HSBColor colorValue = HSBColor.newBuilder().setBrightness(100).setSaturation(90).setHue(10).build();

    public SceneRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        try {
            JPService.setupJUnitTestMode();
            JPService.registerProperty(JPHardwareSimulationMode.class, true);
            JPService.registerProperty(JPRSBTransport.class, JPRSBTransport.TransportType.SPREAD);
            registry = MockRegistryHolder.newMockRegistry();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();
            deviceManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);

            sceneManagerLauncher = new SceneManagerLauncher();
            sceneManagerLauncher.launch();
//            sceneManagerLauncher.getLaunchable().waitForInit(30, TimeUnit.SECONDS);

            powerStateServiceRemote = new PowerStateServiceRemote();
            colorStateServiceRemote = new ColorStateServiceRemote();

            registerScene();

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

    private static void registerScene() throws CouldNotPerformException {
        try {
            ServiceJSonProcessor serviceJSonProcessor = new ServiceJSonProcessor();
            UnitRegistryRemote unitRegistry = Registries.getUnitRegistry();

            List<ActionConfig> actionConfigList = new ArrayList<>();

            ActionConfig.Builder actionConfigBuilder = ActionConfig.newBuilder();
            actionConfigBuilder.setActionAuthority(ActionAuthority.newBuilder().setAuthority(ActionAuthority.Authority.USER)).setActionPriority(ActionPriority.newBuilder().setPriority(ActionPriority.Priority.NORMAL));

            PowerState powerState = PowerState.newBuilder().setValue(powerValue).build();
            actionConfigBuilder.setServiceType(ServiceType.POWER_STATE_SERVICE);
            actionConfigBuilder.setServiceAttribute(serviceJSonProcessor.serialize(powerState));
            actionConfigBuilder.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(powerState));
            for (UnitConfig unitConfig : unitRegistry.getDalUnitConfigs()) {
                if (unitRegistry.getSubUnitTypes(UnitType.LIGHT).contains(unitConfig.getType()) || unitConfig.getType() == UnitType.LIGHT || unitConfig.getType() == UnitType.POWER_SWITCH) {
                    actionConfigBuilder.clearUnitId();
                    actionConfigBuilder.setUnitId(unitConfig.getId());
                    actionConfigList.add(actionConfigBuilder.build());
                    powerStateServiceRemote.init(unitConfig);
                }
            }

            ColorType.Color color = ColorType.Color.newBuilder().setType(ColorType.Color.Type.HSB).setHsbColor(colorValue).build();
            ColorState colorState = ColorState.newBuilder().setColor(color).build();
            actionConfigBuilder.setServiceType(ServiceType.COLOR_STATE_SERVICE);
            actionConfigBuilder.setServiceAttribute(serviceJSonProcessor.serialize(colorState));
            actionConfigBuilder.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(colorState));
            for (UnitConfig unitConfig : unitRegistry.getDalUnitConfigs()) {
                if (unitConfig.getType() == UnitType.COLORABLE_LIGHT) {
                    actionConfigBuilder.clearUnitId();
                    actionConfigBuilder.setUnitId(unitConfig.getId());
                    actionConfigList.add(actionConfigBuilder.build());
                    colorStateServiceRemote.init(unitConfig);
                }
            }

            String label = "testScene";
            PlacementConfig placementConfig = PlacementConfig.newBuilder().setLocationId(Registries.getLocationRegistry().getRootLocationConfig().getId()).build();
            SceneConfig sceneConfig = SceneConfig.newBuilder().addAllActionConfig(actionConfigList).build();
            UnitConfig unitConfig = UnitConfig.newBuilder().setLabel(label).setType(UnitType.SCENE).setSceneConfig(sceneConfig).setPlacementConfig(placementConfig).build();
            unitSceneConfig = unitRegistry.registerUnitConfig(unitConfig).get();
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
//    @Test
    /** (timeout = 10000)* */
    public void testTriggerScenePerRemote() throws Exception {
        System.out.println("testTriggerScenePerRemote");

        System.out.println("Connecting SceneRemote...");
        System.out.println(unitSceneConfig.getScope());
        sceneRemote = Units.getUnit(unitSceneConfig, true, SceneRemote.class);
        System.out.println("SceneRemote connected!");

        ActivationState activationState = ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build();
        sceneRemote.setActivationState(activationState).get();

        System.out.println("Wait for powerStateServiceRemote");
        powerStateServiceRemote.waitForData();
        System.out.println("Wait for colorStateServiceRemote");
        colorStateServiceRemote.waitForData();
        System.out.println("Waiting finished!");

        assertEquals("Scene has not been deactivated after execution!", ActivationState.State.DEACTIVE, sceneRemote.getActivationState().getValue());
        assertEquals("PowerState have not been updated by scene!", powerValue, powerStateServiceRemote.getPowerState().getValue());
        assertEquals("ColorState have not been updated by scene!", colorValue, colorStateServiceRemote.getColorState().getColor().getHsbColor());
    }
}
