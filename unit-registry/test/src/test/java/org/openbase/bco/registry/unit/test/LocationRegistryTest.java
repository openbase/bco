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
import org.junit.Test;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.printer.ExceptionPrinter;
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
public class LocationRegistryTest extends AbstractBCORegistryTest {

    private static UnitConfig.Builder getLocationUnitBuilder(final String label) {
        UnitConfig.Builder unitConfig = UnitConfig.newBuilder().setUnitType(UnitTemplate.UnitType.LOCATION).setLocationConfig(LocationConfig.getDefaultInstance());
        LabelProcessor.addLabel(unitConfig.getLabelBuilder(), Locale.ENGLISH, label);
        return unitConfig;
    }

    private static UnitConfig.Builder getLocationUnitBuilder(final LocationConfig.LocationType locationType, final String label) {
        final UnitConfig.Builder location = getLocationUnitBuilder(label);
        location.getLocationConfigBuilder().setLocationType(locationType);
        return location;
    }

    private static UnitConfig.Builder getLocationUnitBuilder(final LocationConfig.LocationType locationType, final String label, final String locationId) {
        final UnitConfig.Builder location = getLocationUnitBuilder(locationType, label);
        location.getPlacementConfigBuilder().setLocationId(locationId);
        return location;
    }

    private static UnitConfig.Builder getConnectionUnitBuilder(final String label) {
        UnitConfig.Builder unitConfig = UnitConfig.newBuilder().setUnitType(UnitTemplate.UnitType.CONNECTION).setConnectionConfig(ConnectionConfig.getDefaultInstance());
        LabelProcessor.addLabel(unitConfig.getLabelBuilder(), Locale.ENGLISH, label);
        return unitConfig;
    }

    /**
     * Test if a new root location can be introduced by setting the old one as its child.
     *
     * @throws Exception if any error occurs
     */
    @Test(timeout = 5000)
    public void testRootLocationConsistency() throws Exception {
        System.out.println("testChildConsistency");

        // get current root location
        UnitConfig.Builder previousRootLocation = Registries.getUnitRegistry().getRootLocationConfig().toBuilder();

        // register a new location which will become root
        String rootLocationLabel = "NewRootLocation";
        UnitConfig.Builder newRootLocation = Registries.getUnitRegistry().registerUnitConfig(getLocationUnitBuilder(LocationType.ZONE, rootLocationLabel).build()).get().toBuilder();
        // validate that newRootLocation is not yet root and put under the old root location
        assertFalse("New location should not have been registered as root directly", newRootLocation.getLocationConfig().getRoot());
        assertEquals("New location should be put under the old root location", previousRootLocation.getId(), newRootLocation.getPlacementConfig().getLocationId());

        // put previous root location under the new root location
        previousRootLocation.getPlacementConfigBuilder().setLocationId(newRootLocation.getId());
        previousRootLocation = Registries.getUnitRegistry().updateUnitConfig(previousRootLocation.build()).get().toBuilder();
        // retrieve updated new root location
        newRootLocation = Registries.getUnitRegistry().getUnitConfigById(newRootLocation.getId()).toBuilder();
        // validate that previous root location is not root anymore and that the new one is
        assertFalse("Previous root location is still root", previousRootLocation.getLocationConfig().getRoot());
        assertEquals("Previous root location should be put under the new one", newRootLocation.getId(), previousRootLocation.getPlacementConfig().getLocationId());
        assertTrue("New root location did not become root", newRootLocation.getLocationConfig().getRoot());
        assertEquals("New root location should have itself as placement", newRootLocation.getId(), newRootLocation.getPlacementConfig().getLocationId());

        // try to do the change back again
        newRootLocation.getPlacementConfigBuilder().setLocationId(previousRootLocation.getId());
        newRootLocation = Registries.getUnitRegistry().updateUnitConfig(newRootLocation.build()).get().toBuilder();
        // retrieve updated previous root
        previousRootLocation = Registries.getUnitRegistry().getUnitConfigById(previousRootLocation.getId()).toBuilder();
        // test if all changes have been reverted
        assertFalse("New root location is still root", newRootLocation.getLocationConfig().getRoot());
        assertEquals("New root location should be put under the previous one", previousRootLocation.getId(), newRootLocation.getPlacementConfig().getLocationId());
        assertTrue("Previous root location did not become root again", previousRootLocation.getLocationConfig().getRoot());
        assertEquals("Previous root location should have itself as placement", previousRootLocation.getId(), previousRootLocation.getPlacementConfig().getLocationId());
    }

    /**
     * Test if a root location becomes a child after it is set as a child of
     * root locations.
     *
     * @throws Exception if any error occurs
     */
    @Test(timeout = 5000)
    public void testParentChildConsistency() throws Exception {
        System.out.println("testParentChildConsistency");

        // register two locations under the root location on the same level
        UnitConfig.Builder zone1 = Registries.getUnitRegistry().registerUnitConfig(getLocationUnitBuilder(LocationType.ZONE, "zone one").build()).get().toBuilder();
        UnitConfig.Builder zone2 = Registries.getUnitRegistry().registerUnitConfig(getLocationUnitBuilder(LocationType.ZONE, "zone two").build()).get().toBuilder();

        // register a tile as a child of zone1
        UnitConfig.Builder childLocation = getLocationUnitBuilder(LocationType.TILE, "Child Location");
        childLocation.getPlacementConfigBuilder().setLocationId(zone1.getId());
        childLocation = Registries.getUnitRegistry().registerUnitConfig(childLocation.build()).get().toBuilder();

        // retrieve updated zones
        zone1 = Registries.getUnitRegistry().getUnitConfigById(zone1.getId()).toBuilder();
        zone2 = Registries.getUnitRegistry().getUnitConfigById(zone2.getId()).toBuilder();

        // validate that child location is placed under zone1, that zone1 contains it as a child and that zone2 does not
        assertEquals("Child has not been placed under the correct location", zone1.getId(), childLocation.getPlacementConfig().getLocationId());
        assertTrue("Zone1 does not contain child location id", zone1.getLocationConfig().getChildIdList().contains(childLocation.getId()));
        assertFalse("Zone2 contains child location id event though it should not", zone2.getLocationConfig().getChildIdList().contains(childLocation.getId()));

        // move child under zone2
        childLocation.getPlacementConfigBuilder().setLocationId(zone2.getId());
        childLocation = Registries.getUnitRegistry().updateUnitConfig(childLocation.build()).get().toBuilder();

        // retrieve updated zones
        zone1 = Registries.getUnitRegistry().getUnitConfigById(zone1.getId()).toBuilder();
        zone2 = Registries.getUnitRegistry().getUnitConfigById(zone2.getId()).toBuilder();

        // validate that child location is placed under zone2, that zone2 contains it as a child and that zone1 does not
        assertEquals("Child has not been placed under the correct location", zone2.getId(), childLocation.getPlacementConfig().getLocationId());
        assertTrue("Zone2 does not contain child location id", zone2.getLocationConfig().getChildIdList().contains(childLocation.getId()));
        assertFalse("Zone1 contains child location id event though it should not", zone1.getLocationConfig().getChildIdList().contains(childLocation.getId()));
    }

    /**
     * Test if a a loop in the location configuration is detected.
     *
     * @throws Exception if any error occurs
     */
    @Test(timeout = 5000)
    public void testLoopConsistency() throws Exception {
        System.out.println("testLoopConsistency");

        // register a first location
        UnitConfig.Builder zone1 = Registries.getUnitRegistry().registerUnitConfig(getLocationUnitBuilder(LocationType.ZONE, "Looping Zone 1").build()).get().toBuilder();
        // register second location under first location
        UnitConfig.Builder zone2 = getLocationUnitBuilder(LocationType.ZONE, "Looping Zone 2");
        zone2.getPlacementConfigBuilder().setLocationId(zone1.getId());
        zone2 = Registries.getUnitRegistry().registerUnitConfig(zone2.build()).get().toBuilder();

        // put first location also under second location to construct the loop
        zone1.getPlacementConfigBuilder().setLocationId(zone2.getId());

        try {
            // set exception printer to quit because an exception is expected
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            // push loop to registry
            zone1 = Registries.getUnitRegistry().updateUnitConfig(zone1.build()).get().toBuilder();
            // fail if no exception has been thrown
            Assert.fail("No exception when registering a loop");
        } catch (ExecutionException ex) {
            // if an execution exception is thrown the loop could not be registered
        } finally {
            // reset quit flag from exception printer
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
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

    /**
     * Test the registration process of connections.
     * If connections with less than two tile ids can be registered and if the tile id list is cleared up.
     *
     * @throws Exception if any error occurs
     */
    @Test(timeout = 5000)
    public void testConnectionTilesConsistency() throws Exception {
        System.out.println("testConnectionTilesConsistency");

        // register a test location structure with two tiles and a region
        UnitConfig root = Registries.getUnitRegistry().getRootLocationConfig();
        UnitConfig tile1 = Registries.getUnitRegistry().registerUnitConfig(getLocationUnitBuilder(LocationConfig.LocationType.TILE, "Tile 1", root.getId()).build()).get();
        UnitConfig tile2 = Registries.getUnitRegistry().registerUnitConfig(getLocationUnitBuilder(LocationConfig.LocationType.TILE, "Tile 2", root.getId()).build()).get();
        UnitConfig region = Registries.getUnitRegistry().registerUnitConfig(getLocationUnitBuilder(LocationType.REGION, "Region", tile1.getId()).build()).get();

        // create a connection with only one tile id
        UnitConfig.Builder failingConnectionConfig = getConnectionUnitBuilder("Failing connection");
        failingConnectionConfig.getConnectionConfigBuilder().setConnectionType(ConnectionType.DOOR).addTileId(tile1.getId());
        try {
            // set exception printer to quit because an exception is expected
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            // try to register the connection which should fail
            Registries.getUnitRegistry().registerUnitConfig(failingConnectionConfig.build()).get();
            // fail of no exception has been thrown
            Assert.fail("Registered connection with less than one tile");
        } catch (ExecutionException ex) {
            // if an execution exception is thrown the connection could not be registered
        } finally {
            // reset quit flag from exception printer
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }

        // create a new connection with duplicated and fake tile ids
        UnitConfig.Builder connection = getConnectionUnitBuilder("Test Connection");
        connection.getConnectionConfigBuilder().setConnectionType(ConnectionType.WINDOW).addAllTileId(Arrays.asList(
                // add ids of location that are not tiles
                region.getId(),
                root.getId(),
                // add a fake id
                "fakeId",
                // add tile ids with a duplicate
                tile1.getId(),
                tile1.getId(),
                tile2.getId()
        ));
        // register connection
        connection = Registries.getUnitRegistry().registerUnitConfig(connection.build()).get().toBuilder();

        // verify that only the two tile ids remain in the tile id list
        assertEquals("Tile id list has not been reduced by removing fakes and duplicates", 2, connection.getConnectionConfig().getTileIdCount());
        assertTrue("The tile list does not contain the expected tile", connection.getConnectionConfig().getTileIdList().contains(tile1.getId()));
        assertTrue("The tile list does not contain the expected tile", connection.getConnectionConfig().getTileIdList().contains(tile2.getId()));
    }

    /**
     * Test the LocationTypeConsistencyHandler.
     * Test if location types can be recovered and inferred correctly.
     *
     * @throws Exception if any error occurs
     */
    @Test(timeout = 10000)
    public void testLocationTypeConsistency() throws Exception {
        System.out.println("testLocationTypeConsistency");

        // try if the location type for the root location is recovered
        UnitConfig.Builder root = Registries.getUnitRegistry().getRootLocationConfig().toBuilder();
        root.getLocationConfigBuilder().clearLocationType();
        root = Registries.getUnitRegistry().updateUnitConfig(root.build()).get().toBuilder();
        assertEquals("Location type zone has not been recovered for root location", LocationType.ZONE, root.getLocationConfig().getLocationType());

        // register a tile
        UnitConfig.Builder tile = Registries.getUnitRegistry().registerUnitConfig(getLocationUnitBuilder(LocationType.TILE, "Tile", root.getId()).build()).get().toBuilder();

        // register a location under a tile, therefore it should be inferred to be a region
        UnitConfig.Builder region = getLocationUnitBuilder("Region");
        region.getPlacementConfigBuilder().setLocationId(tile.getId());
        region = Registries.getUnitRegistry().registerUnitConfig(region.build()).get().toBuilder();
        assertEquals("Type has not been detected for region", LocationType.REGION, region.getLocationConfig().getLocationType());

        // now the tile has a zone as its parent and a region as its child, therefore the consistency handler should be
        // able to recover its type
        tile.getLocationConfigBuilder().setLocationType(LocationType.ZONE);
        tile = Registries.getUnitRegistry().updateUnitConfig(tile.build()).get().toBuilder();
        assertEquals("Type of tile has not been recovered", LocationType.TILE, tile.getLocationConfig().getLocationType());
    }

    /**
     * Test if a location can be accessed by its scope.
     *
     * @throws Exception if any error occurs
     */
    @Test(timeout = 5000)
    public void testGetLocationUnitConfigByScope() throws Exception {
        System.out.println("testGetLocationUnitConfigByScope");

        final UnitConfig rootLocation = Registries.getUnitRegistry().getRootLocationConfig();
        assertEquals("Could not resolve locationUnitConfig by its scope", rootLocation, Registries.getUnitRegistry().getUnitConfigByScope(rootLocation.getScope()));
    }
}
