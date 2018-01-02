/*-
 * #%L
 * BCO Manager Utility
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.EnablingStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.geometry.PoseType;

/**
 *
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

    @Test
    public void testUnitTransformation() throws Exception {
        try {
            Registries.waitUntilReady();
            Registries.waitForData();

            // load bound to unithost unit
            UnitConfig lightUnitConfig = Registries.getUnitRegistry().getUnitConfigsByLabel(MockRegistry.COLORABLE_LIGHT_LABEL).get(0);
            Assert.assertTrue(lightUnitConfig.getBoundToUnitHost());

            // change unit host position
            UnitConfig.Builder hostUnitConfigBuilder = Registries.getUnitRegistry().getUnitConfigById(lightUnitConfig.getUnitHostId()).toBuilder();
            final PoseType.Pose.Builder pose = PoseType.Pose.newBuilder();
            pose.getRotationBuilder().setQw(1).setQx(0).setQy(0).setQz(0);
            pose.getTranslationBuilder().setX(3).setY(2).setZ(1);
            hostUnitConfigBuilder.getPlacementConfigBuilder().setPosition(pose);
            final UnitConfig hostUnitConfig = Registries.getUnitRegistry().updateUnitConfig(hostUnitConfigBuilder.build()).get();

            // verify child unit positions are updated and published
            lightUnitConfig = Registries.getUnitRegistry().getUnitConfigById(lightUnitConfig.getId());
            System.out.println(lightUnitConfig);
            Registries.getUnitRegistry().waitUntilReady();

            // sleep to wait for rct sync
            // todo: sleep is actiually to long!
            Thread.sleep(500);

            Assert.assertEquals("Positions are not synchronized!", lightUnitConfig.getPlacementConfig().getPosition(), hostUnitConfig.getPlacementConfig().getPosition());
            Assert.assertNotEquals("TransformationFrameId are not unique!", lightUnitConfig.getPlacementConfig().getTransformationFrameId(), hostUnitConfig.getPlacementConfig().getTransformationFrameId());
            Assert.assertEquals("Transformations are not synchronized!", Units.getRootToUnitTransformationFuture(lightUnitConfig).get(5, TimeUnit.SECONDS).getTransform(), Units.getRootToUnitTransformationFuture(hostUnitConfig).get(5, TimeUnit.SECONDS).getTransform());

            // verify that all other unit transformations are available
            verifyTransformations();

            for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs()) {

                // System.out.println("setup transformation of " + unitConfig.getLabel());
                final UnitConfig.Builder unitConfigBuilder = unitConfig.toBuilder();
                unitConfigBuilder.getPlacementConfigBuilder().setPosition(pose);
                unitConfig = Registries.getUnitRegistry().updateUnitConfig(unitConfigBuilder.build()).get();
                // System.out.println("request modified transformation of " + unitConfig.getLabel());

                if (!unitConfig.getEnablingState().getValue().equals(EnablingStateType.EnablingState.State.ENABLED)) {
                    // System.out.println("filter disabled unit " + unitConfig.getLabel());
                    continue;
                }

                // request new transformation...
                try {
                    // System.out.println("waiting for transformation of " + unitConfig.getLabel());
                    Units.getRootToUnitTransformationFuture(unitConfig).get(5000, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ex) {
                    LOGGER.error("transformation of " + unitConfig.getLabel() + " NOT AVAILABLE!");
                    Units.getRootToUnitTransformationFuture(unitConfig).get();
                }

                // System.out.println("transformation of" + unitConfig.getLabel() + " is available.");
            }
            // System.out.println("finished.");
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, System.err);
        }
    }

    private void verifyTransformations() throws NotAvailableException, InterruptedException, CouldNotPerformException, ExecutionException {
        for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs()) {

            if (!unitConfig.getEnablingState().getValue().equals(EnablingStateType.EnablingState.State.ENABLED)) {
                // System.out.println("filter disabled unit " + unitConfig.getLabel());
                continue;
            }

            if (!unitConfig.hasPlacementConfig()
                    || !unitConfig.getPlacementConfig().hasPosition()
                    || !unitConfig.getPlacementConfig().hasTransformationFrameId()
                    || unitConfig.getPlacementConfig().getTransformationFrameId().isEmpty()) {

                // System.out.println("filter unit " + unitConfig.getLabel() + " because no transformation available.");
                continue;
            }

            // System.out.println("request transformation of " + unitConfig.getLabel());
            // request new transformation...
            try {
                // System.out.println("waiting for transformation of " + unitConfig.getLabel());
                Units.getRootToUnitTransformationFuture(unitConfig).get(5000, TimeUnit.MILLISECONDS);
            } catch (TimeoutException ex) {
                LOGGER.error("transformation of " + unitConfig.getLabel() + " NOT AVAILABLE!");
                Units.getRootToUnitTransformationFuture(unitConfig).get();
            }
        }
    }
}
