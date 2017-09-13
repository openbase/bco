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
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.registry.device.core.DeviceRegistryController;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.unit.core.UnitRegistryController;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class UnitGroupRegistryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceRegistryTest.class);

    private static MockRegistry mockRegistry;

    private static UnitRegistryController unitRegistry;
    private static DeviceRegistryController deviceRegistry;

    private static UnitConfigType.UnitConfig LOCATION;

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() throws CouldNotPerformException {
        mockRegistry = MockRegistryHolder.newMockRegistry();

        unitRegistry = (UnitRegistryController) MockRegistry.getUnitRegistry();

        LOCATION = MockRegistry.getLocationRegistry().getRootLocationConfig();
    }

    /**
     * Test if changing the placement of a unit group works.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testPlacementChange() throws Exception {
        LOGGER.info("testPlacementChange");

        UnitConfig.Builder unitConfig = UnitConfig.newBuilder();
        unitConfig.setLabel("PlacementChangeGroup");
        unitConfig.setType(UnitType.UNIT_GROUP);

        PlacementConfig.Builder placement = unitConfig.getPlacementConfigBuilder();
        placement.setLocationId(LOCATION.getId());

        UnitGroupConfig.Builder unitGroupConfig = unitConfig.getUnitGroupConfigBuilder();
        unitGroupConfig.setUnitType(UnitType.COLORABLE_LIGHT);
        unitGroupConfig.addMemberId(unitRegistry.getUnitConfigs(UnitType.COLORABLE_LIGHT).get(0).getId());

        UnitConfig registeredGroup = unitRegistry.registerUnitConfig(unitConfig.build()).get();

        assertEquals("BoundingBox was not generated!", registeredGroup.getPlacementConfig().getShape().getBoundingBox(), MockRegistry.DEFAULT_BOUNDING_BOX);
    }
}
