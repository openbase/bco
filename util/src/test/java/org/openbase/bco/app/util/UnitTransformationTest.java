package org.openbase.bco.app.util;/*-
 * #%L
 * BCO App Utility
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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

import org.junit.*;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.geometry.PoseType;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitTransformationTest extends AbstractBCOManagerTest {

    private final Logger LOGGER = LoggerFactory.getLogger(UnitTransformationTest.class);

    public UnitTransformationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCOManagerTest.setUpClass();
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        AbstractBCOManagerTest.tearDownClass();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test(timeout = 30000)
    public void testUnitTransformation() throws Exception {
        System.out.println("testUnitTransformation");
        try {
            Registries.waitUntilReady();
            Registries.waitForData();

            // load bound to unithost unit
            UnitConfig lightUnitConfig = Registries.getUnitRegistry().getUnitConfigByAlias(MockRegistry.getUnitAlias(UnitType.COLORABLE_LIGHT));
            Assert.assertTrue(lightUnitConfig.getBoundToUnitHost());

            // change unit host position
            UnitConfig.Builder hostUnitConfigBuilder = Registries.getUnitRegistry().getUnitConfigById(lightUnitConfig.getUnitHostId()).toBuilder();
            final PoseType.Pose.Builder pose = PoseType.Pose.newBuilder();
            pose.getRotationBuilder().setQw(1).setQx(0).setQy(0).setQz(0);
            pose.getTranslationBuilder().setX(3).setY(2).setZ(1);
            hostUnitConfigBuilder.getPlacementConfigBuilder().setPose(pose);
            final UnitConfig hostUnitConfig = Registries.getUnitRegistry().updateUnitConfig(hostUnitConfigBuilder.build()).get();

            // verify child unit positions are updated and published
            lightUnitConfig = Registries.getUnitRegistry().getUnitConfigById(lightUnitConfig.getId());
//            System.out.println(lightUnitConfig);
            Registries.getUnitRegistry().waitUntilReady();

            // sleep to wait for rct sync
            // todo: sleep is actually to long!
            Thread.sleep(500);

            Assert.assertEquals("Positions are not synchronized!", lightUnitConfig.getPlacementConfig().getPose(), hostUnitConfig.getPlacementConfig().getPose());
            Assert.assertNotEquals("TransformationFrameId are not unique!", lightUnitConfig.getPlacementConfig().getTransformationFrameId(), hostUnitConfig.getPlacementConfig().getTransformationFrameId());
            Assert.assertEquals("Transformations are not synchronized!", Units.getRootToUnitTransformation(lightUnitConfig).get(5, TimeUnit.SECONDS).getTransform(), Units.getRootToUnitTransformation(hostUnitConfig).get(5, TimeUnit.SECONDS).getTransform());

            // verify that all other unit transformations are available
            verifyTransformations();

            for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs()) {
                if (unitConfig.getUnitType() == UnitType.USER || unitConfig.getUnitType() == UnitType.AUTHORIZATION_GROUP) {
                    // no permissions for change of admin user or group
                    continue;
                }

//                System.out.println("setup transformation of " + LabelProcessor.getBestMatch(unitConfig.getLabel())+ ", " + unitConfig.getUnitType().name());
                final UnitConfig.Builder unitConfigBuilder = unitConfig.toBuilder();
                unitConfigBuilder.getPlacementConfigBuilder().setPose(pose);
                unitConfig = Registries.getUnitRegistry().updateUnitConfig(unitConfigBuilder.build()).get();
//                System.out.println("request modified transformation of " + unitConfig.getLabel());

                // skip disabled units
                if (unitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
//                    System.out.println("filter disabled unit " + unitConfig.getLabel());
                    continue;
                }

                // request new transformation...
                try {
//                    System.out.println("waiting for transformation of " + unitConfig.getLabel());
                    Units.getRootToUnitTransformation(unitConfig).get(5000, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ex) {
                    LOGGER.error("transformation of " + LabelProcessor.getBestMatch(unitConfig.getLabel()) + " NOT AVAILABLE!");
                    Units.getRootToUnitTransformation(unitConfig).get();
                }

//                System.out.println("transformation of" + LabelProcessor.getBestMatch(unitConfig.getLabel())+ " is available.");
            }
            // System.out.println("finished.");
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, System.err);
        }
    }

    private void verifyTransformations() throws InterruptedException, CouldNotPerformException, ExecutionException {
        for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs()) {

            if (unitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                continue;
            }

            if (!unitConfig.hasPlacementConfig()
                    || !unitConfig.getPlacementConfig().hasPose()
                    || !unitConfig.getPlacementConfig().hasTransformationFrameId()
                    || unitConfig.getPlacementConfig().getTransformationFrameId().isEmpty()) {

                // System.out.println("filter unit " + LabelProcessor.getBestMatch(unitConfig.getLabel())+ " because no transformation available.");
                continue;
            }

            // System.out.println("request transformation of " + unitConfig.getLabel());
            // request new transformation...
            try {
                // System.out.println("waiting for transformation of " + unitConfig.getLabel());
                Units.getRootToUnitTransformation(unitConfig).get(5000, TimeUnit.MILLISECONDS);
            } catch (TimeoutException ex) {
                LOGGER.error("transformation of " + LabelProcessor.getBestMatch(unitConfig.getLabel()) + " NOT AVAILABLE!");
                Units.getRootToUnitTransformation(unitConfig).get();
            }
        }
    }
}
