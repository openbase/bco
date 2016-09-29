package org.openbase.bco.registry.location.test;

/*
 * #%L
 * REM LocationRegistry Test
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.registry.device.core.DeviceRegistryController;
import org.openbase.bco.registry.location.core.LocationRegistryController;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.bco.registry.user.core.UserRegistryController;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class LocationRegistryTest {

    private static final Logger logger = LoggerFactory.getLogger(LocationRegistryTest.class);

    private static DeviceRegistryController deviceRegistry;
    private static UserRegistryController userRegistry;

    private static LocationRegistryController locationRegistry;
    private static LocationConfigType.LocationConfig.Builder locationConfig;

    private static LocationRegistryRemote remote;

    public LocationRegistryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InstantiationException, InitializationException, IOException, JPServiceException, InterruptedException, CouldNotPerformException {
        JPService.registerProperty(JPDebugMode.class, true);
//        JPService.registerProperty(JPLocationRegistryScope.class, new Scope("/test/registry/location"));
//        JPService.registerProperty(JPDeviceRegistryScope.class, new Scope("/test/registry/device"));
//        JPService.registerProperty(JPDatabaseDirectory.class, new File("/tmp/" + System.getProperty("user.name") + "/db/"));
        JPService.setupJUnitTestMode();

        deviceRegistry = new DeviceRegistryController();
        userRegistry = new UserRegistryController();
        locationRegistry = new LocationRegistryController();

        deviceRegistry.init();
        userRegistry.init();
        locationRegistry.init();

        Thread deviceRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    deviceRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });

        Thread userRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    userRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });

        Thread locationRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    locationRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });

        deviceRegistryThread.start();
        locationRegistryThread.start();
        userRegistryThread.start();

        deviceRegistryThread.join();
        locationRegistryThread.join();
        userRegistryThread.join();

        locationConfig = LocationConfig.getDefaultInstance().newBuilderForType();
        locationConfig.setId("TestLocationConfigLabel");
        locationConfig.setLabel("TestLocationConfigLabel");

        remote = new LocationRegistryRemote();
        remote.init();
        remote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws InterruptedException, CouldNotPerformException {
        remote.shutdown();
        if (locationRegistry != null) {
            locationRegistry.shutdown();
        }
        if (userRegistry != null) {
            userRegistry.shutdown();
        }
        if (deviceRegistry != null) {
            deviceRegistry.shutdown();
        }
    }

    @Before
    public void setUp() throws CouldNotPerformException {
        deviceRegistry.getUnitGroupRegistry().clear();
        deviceRegistry.getDeviceConfigRegistry().clear();
        locationRegistry.getConnectionConfigRegistry().clear();
        locationRegistry.getLocationConfigRegistry().clear();
        userRegistry.getUserRegistry().clear();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test if a root location is removed that the children become root
     * locations.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testRootConsistency() throws Exception {
        System.out.println("TestRootConsisntency");
        LocationConfig root = LocationConfig.newBuilder().setLabel("TestRootLocation").build();
        LocationConfig registeredRoot = remote.registerLocationConfig(root).get();
        remote.requestData().get();
        assertTrue("The new location isn't registered as a root location.", registeredRoot.getRoot());
        assertEquals("Wrong location scope", "/testrootlocation/", ScopeGenerator.generateStringRep(registeredRoot.getScope()));

        LocationConfig child = LocationConfig.newBuilder().setLabel("TestChildLocation").setPlacementConfig(PlacementConfig.newBuilder().setLocationId(registeredRoot.getId()).build()).build();
        LocationConfig registeredChild = remote.registerLocationConfig(child).get();
        assertTrue("The new location isn't registered as a child location.", !registeredChild.getRoot());
        remote.requestData().get();
        assertTrue("The child location isn't represented in its parent.", remote.getLocationConfigById(registeredRoot.getId()).getChildIdList().contains(registeredChild.getId()));
        assertTrue("The root node contains more than one child.", remote.getLocationConfigById(registeredRoot.getId()).getChildIdCount() == 1);

        LocationConfig removedLocation = remote.removeLocationConfig(registeredRoot).get();
        remote.requestData().get();
        assertFalse("The deleted root location is still available.", remote.containsLocationConfig(removedLocation));
        assertTrue("Child hasn't become a root location after the removal of its parent.", remote.getLocationConfigById(registeredChild.getId()).getRoot());
    }

    /**
     * Test if a root location becomes a child after it is set as a child of
     * root locations.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testChildConsistency() throws Exception {
        System.out.println("TestChildConsistency");
        String label = "Test2Living";
        LocationConfig living = LocationConfig.newBuilder().setLabel(label).build();
        LocationConfig registeredLiving = remote.registerLocationConfig(living).get();
        assertTrue("The new location isn't registered as a root location.", registeredLiving.getRoot());
        assertEquals("Label has not been set", label, registeredLiving.getLabel());

        String rootLocationConfigLabel = "Test3RootLocation";
        LocationConfig.Builder rootLocationConfigBuilder = LocationConfig.newBuilder();
        rootLocationConfigBuilder.setLabel(rootLocationConfigLabel);
        LocationConfig registeredRootLocationConfig = remote.registerLocationConfig(rootLocationConfigBuilder.build()).get();
        LocationConfig.Builder registeredLivingBuilder = remote.getLocationConfigById(registeredLiving.getId()).toBuilder();
        registeredLivingBuilder.getPlacementConfigBuilder().setLocationId(registeredRootLocationConfig.getId());
        remote.updateLocationConfig(registeredLivingBuilder.build()).get();
        remote.requestData().get();
        assertEquals("Parent was not updated!", registeredRootLocationConfig.getId(), registeredLivingBuilder.getPlacementConfig().getLocationId());

        LocationConfig home = LocationConfig.newBuilder().setLabel("Test2Home").build();
        LocationConfig registeredHome = remote.registerLocationConfig(home).get();
        registeredLivingBuilder = remote.getLocationConfigById(registeredRootLocationConfig.getId()).toBuilder();
        registeredLivingBuilder.getPlacementConfigBuilder().setLocationId(registeredHome.getId());
        assertEquals("Parent was not updated!", registeredHome.getId(), remote.updateLocationConfig(registeredLivingBuilder.build()).get().getPlacementConfig().getLocationId());
    }

    /**
     * Test if a root location becomes a child after it is set as a child of
     * root locations.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testParentIDUpdateConsistency() throws Exception {
        System.out.println("TestParentIDUpdateConsistency");

        String rootLocationConfigLabel = "Test3RootLocation";
        LocationConfig.Builder rootLocationConfigBuilder = LocationConfig.newBuilder();
        rootLocationConfigBuilder.setLabel(rootLocationConfigLabel);
        LocationConfig registeredRootLocationConfig = remote.registerLocationConfig(rootLocationConfigBuilder.build()).get();

        String childLocationConfigLabel = "Test3ChildLocation";
        LocationConfig.Builder childLocationConfigBuilder = LocationConfig.newBuilder();
        childLocationConfigBuilder.setLabel(childLocationConfigLabel);
        childLocationConfigBuilder.getPlacementConfigBuilder().setLocationId(registeredRootLocationConfig.getId());
        LocationConfig registeredChildLocationConfig = remote.registerLocationConfig(childLocationConfigBuilder.build()).get();

        String parentLabel = "Test3ParentLocation";
        LocationConfig.Builder parentLocationConfigBuilder = LocationConfig.newBuilder().setLabel(parentLabel);
        LocationConfig registeredParentLocationConfig = remote.registerLocationConfig(parentLocationConfigBuilder.build()).get();

        assertEquals("The new location isn't registered as child of Location[" + rootLocationConfigLabel + "]!", registeredRootLocationConfig.getId(), registeredChildLocationConfig.getPlacementConfig().getLocationId());

        LocationConfig.Builder registeredChildLocationConfigBuilder = registeredChildLocationConfig.toBuilder();
        registeredChildLocationConfigBuilder.getPlacementConfigBuilder().setLocationId(registeredParentLocationConfig.getId());
        registeredChildLocationConfig = remote.updateLocationConfig(registeredChildLocationConfigBuilder.build()).get();

        assertEquals("The parent location of child was not updated as new placement location id after update.", registeredParentLocationConfig.getId(), registeredChildLocationConfig.getPlacementConfig().getLocationId());
        remote.requestData().get();
        assertEquals("The parent location of child was not updated as new placement location id in global registry.", registeredParentLocationConfig.getId(), remote.getLocationConfigsByLabel(childLocationConfigLabel).get(0).getPlacementConfig().getLocationId());
    }

    @Test(timeout = 5000)
    public void testGetUnitConfigs() throws Exception {
        System.out.println("TestGetUnitConfigs");

        try {
            remote.getUnitConfigsByLocation(UnitTemplate.UnitType.UNKNOWN, locationConfig.getId());
            assertTrue("Exception handling failed!", false);
        } catch (CouldNotPerformException ex) {
            // this should not happen id unit type is unknown!
        }

        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            remote.getUnitConfigsByLocation(UnitTemplate.UnitType.COLORABLE_LIGHT, "Quark");
            assertTrue("Exception handling failed!", false);
        } catch (CouldNotPerformException ex) {
            // this should happen id unit type is unknown!
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }

    /**
     * Test if a a loop in the location configuration is detected by the
     * consistency handler.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testLoopConsistency() throws Exception {
        System.out.println("TestLoopConsistency");

        String rootLabel = "Root";
        String firstChildLabel = "FirstChild";
        String SecondChildLabel = "SecondChild";
        LocationConfig root = LocationConfig.newBuilder().setLabel(rootLabel).build();
        root = remote.registerLocationConfig(root).get();

        LocationConfig firstChild = LocationConfig.newBuilder().setLabel(firstChildLabel).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).build();
        remote.registerLocationConfig(firstChild);

        LocationConfig secondChild = LocationConfig.newBuilder().setLabel(SecondChildLabel).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).build();
        secondChild = remote.registerLocationConfig(secondChild).get();

        try {
            // register loop
            root = remote.getLocationConfigById(root.getId());
            LocationConfig.Builder rootBuilder = root.toBuilder();
            rootBuilder.getPlacementConfigBuilder().setLocationId(secondChild.getId());
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            remote.registerLocationConfig(rootBuilder.build()).get();
            Assert.fail("No exception when registering location with a loop [" + secondChild + "]");
        } catch (Exception ex) {
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }

    /**
     * Test if a location with two children with the same label can be
     * registered.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testChildWithSameLabelConsistency() throws Exception {
        System.out.println("TestChildWithSameLabelConsistency");

        String rootLabel = "RootWithChildrenWithSameLabel";
        String childLabel = "childWithSameLabel";
        LocationConfig root = LocationConfig.newBuilder().setLabel(rootLabel).build();
        root = remote.registerLocationConfig(root).get();

        LocationConfig firstChild = LocationConfig.newBuilder().setLabel(childLabel).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).build();
        firstChild = remote.registerLocationConfig(firstChild).get();

        try {
            LocationConfig secondChild = LocationConfig.newBuilder().setLabel(childLabel).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).build();
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            secondChild = remote.registerLocationConfig(secondChild).get();
            Assert.fail("No exception thrown when registering a second child with the same label");
        } catch (Exception ex) {
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }

    /**
     * Test connection scope, location and label consistency handler.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testConnectionLocationAndScopeAndLabelConsistency() throws Exception {
        System.out.println("TestConnectionLocationAndScopeAndLabelConsistency");

        String rootLabel = "RootZoneForConnectionTest";
        String zoneLabel = "SubZone";
        String tile1Label = "Tile1";
        String tile2Label = "Tile3";
        String tile3Label = "Tile2";
        LocationConfig root = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(rootLabel).setType(LocationConfig.LocationType.ZONE).build()).get();
        LocationConfig zone = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(zoneLabel).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).setType(LocationConfig.LocationType.ZONE).build()).get();
        LocationConfig tile1 = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(tile1Label).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).setType(LocationConfig.LocationType.TILE).build()).get();
        LocationConfig tile2 = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(tile2Label).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(zone.getId())).setType(LocationConfig.LocationType.TILE).build()).get();
        LocationConfig tile3 = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(tile3Label).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(zone.getId())).setType(LocationConfig.LocationType.TILE).build()).get();

        String connection1Label = "Connection1";
        String connection2Label = "Connection2";
        ConnectionConfig connection1 = remote.registerConnectionConfig(ConnectionConfig.newBuilder().setLabel(connection1Label).setType(ConnectionConfig.ConnectionType.DOOR).addTileId(tile1.getId()).addTileId(tile2.getId()).build()).get();
        ConnectionConfig connection2 = remote.registerConnectionConfig(ConnectionConfig.newBuilder().setLabel(connection2Label).setType(ConnectionConfig.ConnectionType.WINDOW).addTileId(tile2.getId()).addTileId(tile3.getId()).build()).get();

        assertEquals(root.getId(), connection1.getPlacementConfig().getLocationId());
        assertEquals(zone.getId(), connection2.getPlacementConfig().getLocationId());

        assertEquals("/rootzoneforconnectiontest/door/connection1/", ScopeGenerator.generateStringRep(connection1.getScope()));
        assertEquals(ScopeGenerator.generateConnectionScope(connection2, zone), connection2.getScope());

        ConnectionConfig connection3 = ConnectionConfig.newBuilder().setLabel(connection2Label).setType(ConnectionConfig.ConnectionType.PASSAGE).addTileId(tile2.getId()).addTileId(tile3.getId()).build();
        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            remote.registerConnectionConfig(connection3).get();
            Assert.fail("No exception thrown when registering a second connection at the same location with the same label");
        } catch (Exception ex) {
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }
    }

    /**
     * Test connection tiles consistency handler.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testConnectionTilesConsistency() throws Exception {
        System.out.println("TestConnectionTilesConsistency");

        String rootLabel = "ConnectionTestRootZone";
        String noTileLabel = "NoTile";
        String tile1Label = "RealTile1";
        String tile2Label = "RealTile2";
        LocationConfig root = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(rootLabel).setType(LocationConfig.LocationType.ZONE).build()).get();
        LocationConfig noTile = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(noTileLabel).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).setType(LocationConfig.LocationType.REGION).build()).get();
        LocationConfig tile1 = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(tile1Label).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).setType(LocationConfig.LocationType.TILE).build()).get();
        LocationConfig tile2 = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(tile2Label).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).setType(LocationConfig.LocationType.TILE).build()).get();

        String connectionFailLabel = "ConnectionFail";
        String connectionLabel = "TilesTestConnection";
        ConnectionConfig connectionFail = ConnectionConfig.newBuilder().setLabel(connectionFailLabel).setType(ConnectionConfig.ConnectionType.DOOR).addTileId(tile2.getId()).build();
        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            remote.registerConnectionConfig(connectionFail).get();
            Assert.fail("Registered connection with less than one tile");
        } catch (ExecutionException ex) {
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }

        ConnectionConfig.Builder connectionBuilder = ConnectionConfig.newBuilder().setLabel(connectionLabel).setType(ConnectionConfig.ConnectionType.WINDOW);
        connectionBuilder.addTileId(noTile.getId());
        connectionBuilder.addTileId(tile1.getId());
        connectionBuilder.addTileId(tile1.getId());
        connectionBuilder.addTileId(tile2.getId());
        connectionBuilder.addTileId(root.getId());
        connectionBuilder.addTileId("fakeLocationId");
        ConnectionConfig connection = remote.registerConnectionConfig(connectionBuilder.build()).get();

        assertEquals("Doubled tiles or locations that aren't tiles or that do not exists do not have been removed", 2, connection.getTileIdCount());
        assertTrue("The tile list does not contain the expected tile", connection.getTileIdList().contains(tile1.getId()));
        assertTrue("The tile list does not contain the expected tile", connection.getTileIdList().contains(tile2.getId()));
    }
}
