package org.openbase.bco.registry.unit.test;

/*
 * #%L
 * BCO Registry Unit Test
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.jp.JPRSBLegacyMode;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.connection.ConnectionConfigType.ConnectionConfig;
import org.openbase.type.domotic.unit.connection.ConnectionConfigType.ConnectionConfig.ConnectionType;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class LocationRegistryLegacyTest extends AbstractBCORegistryTest {

    @BeforeClass
    public static void setupProperties() throws Exception {
        JPService.registerProperty(JPRSBLegacyMode.class, true);
    }

    private static UnitConfig.Builder getLocationUnitBuilder(final String label) {
        UnitConfig.Builder unitConfig = UnitConfig.newBuilder().setUnitType(UnitTemplate.UnitType.LOCATION).setLocationConfig(LocationConfig.getDefaultInstance());
        LabelProcessor.addLabel(unitConfig.getLabelBuilder(), Locale.ENGLISH, label);
        return unitConfig;
    }

    private static UnitConfig.Builder getLocationUnitBuilder(final LocationType locationType, final String label) {
        final UnitConfig.Builder location = getLocationUnitBuilder(label);
        location.getLocationConfigBuilder().setLocationType(locationType);
        return location;
    }

    /**
     * Test if a location with two children with the same label can be
     * registered.
     *
     * @throws Exception if any error occurs
     */
    @Test(timeout = 5000)
    public void testChildWithSameLabelConsistency() throws Exception {
        System.out.println("testChildWithSameLabelConsistency");

        // retrieve root location
        final UnitConfig root = Registries.getUnitRegistry().getRootLocationConfig();
        // create label
        final String label = "childish";
        // generate 2 identical location unit configs
        UnitConfig.Builder child1 = getLocationUnitBuilder(LocationType.ZONE, label);
        UnitConfig.Builder child2 = getLocationUnitBuilder(LocationType.ZONE, label);
        child1.getPlacementConfigBuilder().setLocationId(root.getId());
        child2.getPlacementConfigBuilder().setLocationId(root.getId());

        // register the first one
        Registries.getUnitRegistry().registerUnitConfig(child1.build()).get();
        try {
            // set exception printer to quit because an exception is expected
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            // register second child
            Registries.getUnitRegistry().registerUnitConfig(child2.build()).get();
            // fail if the no exception has been thrown
            Assert.fail("No exception thrown when registering a second child with the same label");
        } catch (ExecutionException ex) {
            // if an execution exception is thrown the second child could not be registered
        } finally {
            // reset quit flag from exception printer
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }
}
