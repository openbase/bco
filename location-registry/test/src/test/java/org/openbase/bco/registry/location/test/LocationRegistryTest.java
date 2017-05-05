package org.openbase.bco.registry.location.test;

/*
 * #%L
 * BCO Registry Location Test
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
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.registry.location.core.LocationRegistryController;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import rst.math.Vec3DDoubleType.Vec3DDouble;
import static junit.framework.TestCase.assertEquals;

/**
 *
 * @author <a href="mailto:jdaberkow@techfak.uni-bielefeld.de">Julian Daberkow</a>
 */
public class LocationRegistryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationRegistryTest.class);

    private static LocationRegistryController locationRegistry;

    @BeforeClass
    public static void setUpClass() throws IOException, JPServiceException, InterruptedException, CouldNotPerformException, ExecutionException {
        JPService.setupJUnitTestMode();
    }

    @AfterClass
    public static void tearDownClass() {
        try {
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() throws CouldNotPerformException {
        MockRegistryHolder.newMockRegistry();
        locationRegistry = (LocationRegistryController) MockRegistry.getLocationRegistry();
    }

    @After
    public void tearDown() throws CouldNotPerformException {
        MockRegistryHolder.shutdownMockRegistry();
    }

    /**
     * Test of get getLocationConfigsByCoordinate method, of class
     * LocationRegistry.
     */
    @Test(timeout = 5000)
    public void testGetLocationConfigsByCoordinate() throws Exception {
        LOGGER.info("testGetLocationConfigsByCoordinate");
        List<UnitConfig> pointEden = locationRegistry.getLocationConfigsByCoordinate(Vec3DDouble.newBuilder().setX(1.5).setY(4).setZ(0).build());
        assertEquals(3, pointEden.size());

        List<UnitConfig> pointEdenFilter = locationRegistry.getLocationConfigsByCoordinate(Vec3DDouble.newBuilder().setX(1.5).setY(4).setZ(0).build(), LocationType.TILE);
        assertEquals(1, pointEdenFilter.size());

        List<UnitConfig> pointParadise = locationRegistry.getLocationConfigsByCoordinate(Vec3DDouble.newBuilder().setX(0.5).setY(3).setZ(0).build());
        assertEquals(1, pointParadise.size());

        List<UnitConfig> pointHell = locationRegistry.getLocationConfigsByCoordinate(Vec3DDouble.newBuilder().setX(4).setY(3).setZ(0).build());
        assertEquals(2, pointHell.size());
    }
}
