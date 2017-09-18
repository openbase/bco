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

import java.util.Random;
import java.util.concurrent.ExecutionException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.geometry.PoseType.Pose;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class TestBoundToDeviceFlag extends AbstractBCORegistryTest {

    private final Random random = new Random();
    private final UnitType[] unitTypes = {UnitType.LIGHT, UnitType.LIGHT, UnitType.LIGHT};
    private DeviceClass deviceClass;

    private UnitConfig deviceUnitConfig;
    private UnitConfig lightOne;
    private UnitConfig lightTwo;
    private UnitConfig lightThree;

    private Pose poseDevice;
    private Pose poseLightOne;
    private Pose poseLightTwo;
    private Pose poseLightThree;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        try {
            deviceClass = super.registerDeviceClass(getDeviceClass("Label", "Product Number", "Company", unitTypes));
            PlacementConfig placement = generatePlacementConfig();
            Pose position = placement.getPosition();
            deviceUnitConfig = unitRegistry.registerUnitConfig(getDeviceUnitConfig("Label", "Serial Number", deviceClass).toBuilder().setPlacementConfig(placement).build()).get();
            getUpdatedConfigs();

            unitRegistry.updateUnitConfig(lightOne.toBuilder().setPlacementConfig(generatePlacementConfig()).build()).get();
            unitRegistry.updateUnitConfig(lightTwo.toBuilder().setBoundToUnitHost(true).setPlacementConfig(placement).build()).get();
            unitRegistry.updateUnitConfig(lightThree.toBuilder().setPlacementConfig(generatePlacementConfig()).build()).get();
            getUpdatedConfigs();

            // verify expected start:
            assertTrue(!deviceUnitConfig.getBoundToUnitHost());
            assertTrue(!lightOne.getBoundToUnitHost());
            assertTrue(lightTwo.getBoundToUnitHost());
            assertTrue(!lightThree.getBoundToUnitHost());
            assertEquals(position, poseDevice);
            assertTrue(!position.equals(poseLightOne));
            assertEquals(position, poseLightTwo);
            assertTrue(!position.equals(poseLightThree));
        } catch (CouldNotPerformException | ExecutionException | InterruptedException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    private PlacementConfig generatePlacementConfig() throws CouldNotPerformException {
        PlacementConfig.Builder placement = PlacementConfig.newBuilder();
        placement.setLocationId(locationRegistry.getRootLocationConfig().getId());
        placement.getPositionBuilder().getRotationBuilder().setQw(random.nextDouble()).setQx(random.nextDouble()).setQy(random.nextDouble()).setQz(random.nextDouble()).build();
        placement.getPositionBuilder().getTranslationBuilder().setX(random.nextInt(10)).setY(random.nextInt(10)).setZ(random.nextInt(10));
        return placement.build();
    }

    private void getUpdatedConfigs() throws CouldNotPerformException {
        deviceUnitConfig = unitRegistry.getUnitConfigById(deviceUnitConfig.getId());
        lightOne = unitRegistry.getUnitConfigById(deviceUnitConfig.getDeviceConfig().getUnitId(0));
        lightTwo = unitRegistry.getUnitConfigById(deviceUnitConfig.getDeviceConfig().getUnitId(1));
        lightThree = unitRegistry.getUnitConfigById(deviceUnitConfig.getDeviceConfig().getUnitId(2));

        poseDevice = deviceUnitConfig.getPlacementConfig().getPosition();
        poseLightOne = lightOne.getPlacementConfig().getPosition();
        poseLightTwo = lightTwo.getPlacementConfig().getPosition();
        poseLightThree = lightThree.getPlacementConfig().getPosition();
    }

    /**
     * Test what happens when the flag is not set in the device and
     * not in its unit.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testDeviceNotBoundAndUnitsNotBound() throws Exception {
        logger.info("testDeviceNotBoundAndUnitsNotBound");

        Pose oldPoseDevice = deviceUnitConfig.getPlacementConfig().getPosition();
        Pose oldPoseLightTwo = lightTwo.getPlacementConfig().getPosition();
        Pose oldPoseLightThree = lightThree.getPlacementConfig().getPosition();

        // change placement of light one and check what happens in the device
        // and all other lights
        PlacementConfig placementConfig = generatePlacementConfig();
        unitRegistry.updateUnitConfig(lightOne.toBuilder().setPlacementConfig(placementConfig).build()).get();
        getUpdatedConfigs();

        // everything but light one should stay the same
        assertEquals(placementConfig.getPosition(), poseLightOne);
        assertEquals(oldPoseDevice, poseDevice);
        assertEquals(oldPoseLightTwo, poseLightTwo);
        assertEquals(oldPoseLightThree, poseLightThree);
    }

    /**
     * Test what happens when the flag is set in the device and
     * in not its unit.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testDeviceBoundAndUnitsNotBound() throws Exception {
        logger.info("testDeviceBoundAndUnitsNotBound");

        // change bound to host flag in device
        unitRegistry.updateUnitConfig(deviceUnitConfig.toBuilder().setBoundToUnitHost(true).build()).get();
        getUpdatedConfigs();
        assertTrue(deviceUnitConfig.getBoundToUnitHost());

        // all lights should now be bound to unit host and they should all have the device position
        assertTrue(lightOne.getBoundToUnitHost());
        assertTrue(lightTwo.getBoundToUnitHost());
        assertTrue(lightThree.getBoundToUnitHost());
        assertEquals(poseDevice, poseLightOne);
        assertEquals(poseDevice, poseLightTwo);
        assertEquals(poseDevice, poseLightThree);

        // test if changing light one is still possible
        unitRegistry.updateUnitConfig(lightOne.toBuilder().setBoundToUnitHost(false).setPlacementConfig(generatePlacementConfig()).build()).get();
        getUpdatedConfigs();

        assertTrue(lightOne.getBoundToUnitHost());
        assertTrue(lightTwo.getBoundToUnitHost());
        assertTrue(lightThree.getBoundToUnitHost());
        assertEquals(poseDevice, poseLightOne);
        assertEquals(poseDevice, poseLightTwo);
        assertEquals(poseDevice, poseLightThree);
    }

    /**
     * Test what happens when the flag is not set in the device but
     * in its unit.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testDeviceNotBoundAndUnitsBound() throws Exception {
        logger.info("testDeviceBoundAndUnitsNotBound");

        Pose oldPoseLightThree = poseLightThree;

        // set light one to bound to unit host which should make it adapt the position of the device
        unitRegistry.updateUnitConfig(lightOne.toBuilder().setBoundToUnitHost(true).build()).get();
        getUpdatedConfigs();

        assertTrue(lightOne.getBoundToUnitHost());
        assertEquals(poseDevice, poseLightOne);

        // change position of light one and check if the position is updated in the device and all other bound units
        PlacementConfig tmp = generatePlacementConfig();
        Pose newPoseLightOne = tmp.getPosition();
        unitRegistry.updateUnitConfig(lightOne.toBuilder().setPlacementConfig(tmp).build()).get();
        getUpdatedConfigs();

        assertEquals(newPoseLightOne, poseLightOne);
        assertEquals(newPoseLightOne, poseDevice);
        assertEquals(newPoseLightOne, poseLightTwo);
        assertEquals(oldPoseLightThree, poseLightThree);
        assertTrue(!poseLightThree.equals(poseLightOne));
    }

    /**
     * Test what happens when the flag is set in the device and
     * its unit.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testDeviceBoundAndUnitsBound() throws Exception {
        logger.info("testDeviceBoundAndUnitsBound");

        // change bound to host flag in device
        unitRegistry.updateUnitConfig(deviceUnitConfig.toBuilder().setBoundToUnitHost(true).setPlacementConfig(generatePlacementConfig()).build()).get();
        getUpdatedConfigs();
        assertTrue(deviceUnitConfig.getBoundToUnitHost());

        // all lights should now be bound to unit host and they should all have the device position
        assertTrue(lightOne.getBoundToUnitHost());
        assertTrue(lightTwo.getBoundToUnitHost());
        assertTrue(lightThree.getBoundToUnitHost());
        assertEquals(poseDevice, poseLightOne);
        assertEquals(poseDevice, poseLightTwo);
        assertEquals(poseDevice, poseLightThree);
        
        // update light one and check if everything else adapts
        PlacementConfig tmp = generatePlacementConfig();
        Pose newPoseLightOne = tmp.getPosition();
        unitRegistry.updateUnitConfig(lightOne.toBuilder().setPlacementConfig(tmp).build()).get();
        getUpdatedConfigs();
        
        assertEquals(newPoseLightOne, poseLightOne);
        assertEquals(newPoseLightOne, poseDevice);
        assertEquals(newPoseLightOne, poseLightTwo);
        assertEquals(newPoseLightOne, poseLightThree);
    }
}
