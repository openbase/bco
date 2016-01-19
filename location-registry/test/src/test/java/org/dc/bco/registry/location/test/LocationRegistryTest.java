/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.test;

import org.dc.bco.registry.device.core.DeviceRegistryController;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.bco.registry.location.core.LocationRegistryController;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class LocationRegistryTest {

    private static final Logger logger = LoggerFactory.getLogger(LocationRegistryTest.class);

    private static DeviceRegistryController deviceRegistry;

    private static LocationRegistryController locationRegistry;
    private static LocationConfigType.LocationConfig.Builder locationConfig;

    private static LocationConfigType.LocationConfig.Builder locationConfigRemote;
    private static LocationRegistryRemote remote;

    public LocationRegistryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InstantiationException, InitializationException, IOException, JPServiceException, InterruptedException, CouldNotPerformException {
//        JPService.registerProperty(JPInitializeDB.class, true);
//        JPService.registerProperty(JPLocationRegistryScope.class, new Scope("/test/registry/location"));
//        JPService.registerProperty(JPDeviceRegistryScope.class, new Scope("/test/registry/device"));
//        JPService.registerProperty(JPDatabaseDirectory.class, new File("/tmp/" + System.getProperty("user.name") + "/db/"));
        JPService.setupJUnitTestMode();

        deviceRegistry = new DeviceRegistryController();
        locationRegistry = new LocationRegistryController();

        deviceRegistry.init();
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

        deviceRegistryThread.join();
        locationRegistryThread.join();

        locationConfig = LocationConfig.getDefaultInstance().newBuilderForType();
        locationConfig.setId("TestLocationConfigLabel");
        locationConfig.setLabel("TestLocationConfigLabel");

        locationConfigRemote = LocationConfig.getDefaultInstance().newBuilderForType();

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
        if (deviceRegistry != null) {
            deviceRegistry.shutdown();
        }
    }

    @Before
    public void setUp() {
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
    @Test
    public void testRootConsistency() throws Exception {
        LocationConfig root = LocationConfig.newBuilder().setLabel("TestRootLocation").build();
        LocationConfig registeredRoot = remote.registerLocationConfig(root);
        remote.requestStatus();
        assertTrue("The new location isn't registered as a root location.", registeredRoot.getRoot());
        assertEquals("Wrong location scope", "/testrootlocation/", ScopeGenerator.generateStringRep(registeredRoot.getScope()));

        LocationConfig child = LocationConfig.newBuilder().setLabel("TestChildLocation").setParentId(registeredRoot.getId()).build();
        LocationConfig registeredChild = remote.registerLocationConfig(child);
        assertTrue("The new location isn't registered as a child location.", !registeredChild.getRoot());
        remote.requestStatus();
        assertTrue("The child location isn't represented in its parent.", remote.getLocationConfigById(registeredRoot.getId()).getChildIdList().contains(registeredChild.getId()));
        assertTrue("The root node contains more than one child.", remote.getLocationConfigById(registeredRoot.getId()).getChildIdCount() == 1);

        LocationConfig removedLocation = remote.removeLocationConfig(registeredRoot);
        remote.requestStatus();
        assertFalse("The deleted root location is still available.", remote.containsLocationConfig(removedLocation));
        assertTrue("Child hasn't become a root location after the removal of its parent.", remote.getLocationConfigById(registeredChild.getId()).getRoot());
    }

    /**
     * Test if a root location becomes a child after it is set as a child of
     * root locations.
     *
     * @throws Exception
     */
    @Test
    public void testChildConsistency() throws Exception {
        String label = "Test2Living";
        LocationConfig living = LocationConfig.newBuilder().setLabel(label).build();
        LocationConfig registeredLiving = remote.registerLocationConfig(living);
        remote.requestStatus();
        assertTrue("The new location isn't registered as a root location.", registeredLiving.getRoot());
        assertEquals("Label has not been set", label, registeredLiving.getLabel());

        LocationConfig home = LocationConfig.newBuilder().setLabel("Test2Home").addChildId(registeredLiving.getId()).build();
        LocationConfig registeredHome = remote.registerLocationConfig(home);
        remote.requestStatus();
        assertTrue("The new location isn't registered as a root location.", registeredHome.getRoot());
        assertFalse("Root hasn't become a child location after setting its parent.", remote.getLocationConfigById(registeredLiving.getId()).getRoot());
    }

    @Test
    public void testGetUnitConfigs() throws Exception {
        try {
            remote.getUnitConfigsByLocation(UnitTemplate.UnitType.UNKNOWN, locationConfig.getId());
            assertTrue("Exception handling failed!", false);
        } catch (CouldNotPerformException ex) {
            // this should happen id unit type is unknown!
        }

        try {
            remote.getUnitConfigsByLocation(UnitTemplate.UnitType.AMBIENT_LIGHT, "Quark");
            assertTrue("Exception handling failed!", false);
        } catch (CouldNotPerformException ex) {
            // this should happen id unit type is unknown!
        }
    }

    /**
     * Test if a a loop in the location configuration is detected by the
     * consistency handler.
     *
     * @throws Exception
     */
    @Test
    public void testLoopConsistency() throws Exception {
        String rootLabel = "Root";
        String firstChildLabel = "FirstChild";
        String SecondChildLabel = "SecondChild";
        LocationConfig root = LocationConfig.newBuilder().setLabel(rootLabel).build();
        root = remote.registerLocationConfig(root);

        LocationConfig firstChild = LocationConfig.newBuilder().setLabel(firstChildLabel).setParentId(root.getId()).build();
        remote.registerLocationConfig(firstChild);

        try {
            LocationConfig secondChild = LocationConfig.newBuilder().setLabel(SecondChildLabel).setParentId(root.getId()).addChildId(root.getId()).build();
            secondChild = remote.registerLocationConfig(secondChild);
            Assert.fail("No exception when registering location with a loop [" + secondChild + "]");
        } catch (Exception ex) {
        }
    }

    /**
     * Test if a location with two children with the same label can be
     * registered.
     *
     * @throws Exception
     */
    @Test
    public void testChildWithSameLabelConsistency() throws Exception {
        String rootLabel = "RootWithChildrenWithSameLabel";
        String childLabel = "childWithSameLabel";
        LocationConfig root = LocationConfig.newBuilder().setLabel(rootLabel).build();
        root = remote.registerLocationConfig(root);

        LocationConfig firstChild = LocationConfig.newBuilder().setLabel(childLabel).setParentId(root.getId()).build();
        firstChild = remote.registerLocationConfig(firstChild);

        try {
            LocationConfig secondChild = LocationConfig.newBuilder().setLabel(childLabel).setParentId(root.getId()).build();
            secondChild = remote.registerLocationConfig(secondChild);
            Assert.fail("No exception thrown when registering a second child with the same label");
        } catch (Exception ex) {
        }
    }

    /**
     * Test connection scope, location and label consistency handler.
     *
     * @throws Exception
     */
    @Test
    public void testConnectionLocationAndScopeAndLabelConsistency() throws Exception {
        String rootLabel = "RootZoneForConnectionTest";
        String zoneLabel = "SubZone";
        String tile1Label = "Tile1";
        String tile2Label = "Tile3";
        String tile3Label = "Tile2";
        LocationConfig root = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(rootLabel).setType(LocationConfig.LocationType.ZONE).build());
        LocationConfig zone = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(zoneLabel).setParentId(root.getId()).setType(LocationConfig.LocationType.ZONE).build());
        LocationConfig tile1 = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(tile1Label).setParentId(root.getId()).setType(LocationConfig.LocationType.TILE).build());
        LocationConfig tile2 = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(tile2Label).setParentId(zone.getId()).setType(LocationConfig.LocationType.TILE).build());
        LocationConfig tile3 = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(tile3Label).setParentId(zone.getId()).setType(LocationConfig.LocationType.TILE).build());

        String connection1Label = "Connection1";
        String connection2Label = "Connection2";
        ConnectionConfig connection1 = remote.registerConnectionConfig(ConnectionConfig.newBuilder().setLabel(connection1Label).setType(ConnectionConfig.ConnectionType.DOOR).addTileId(tile1.getId()).addTileId(tile2.getId()).build());
        ConnectionConfig connection2 = remote.registerConnectionConfig(ConnectionConfig.newBuilder().setLabel(connection2Label).setType(ConnectionConfig.ConnectionType.WINDOW).addTileId(tile2.getId()).addTileId(tile3.getId()).build());

        assertEquals(root.getId(), connection1.getPlacementConfig().getLocationId());
        assertEquals(zone.getId(), connection2.getPlacementConfig().getLocationId());

        assertEquals("/rootzoneforconnectiontest/door/connection1/", ScopeGenerator.generateStringRep(connection1.getScope()));
        assertEquals(ScopeGenerator.generateConnectionScope(connection2, zone), connection2.getScope());

        ConnectionConfig connection3 = ConnectionConfig.newBuilder().setLabel(connection2Label).setType(ConnectionConfig.ConnectionType.PASSAGE).addTileId(tile2.getId()).addTileId(tile3.getId()).build();
        try {
            remote.registerConnectionConfig(connection3);
            Assert.fail("No exception thrown when registering a second connection at the same location with the same label");
        } catch (CouldNotPerformException ex) {
        }
    }

    /**
     * Test connection tiles consistency handler.
     *
     * @throws Exception
     */
    @Test
    public void testConnectionTilesConsistency() throws Exception {
        String rootLabel = "ConnectionTestRootZone";
        String noTileLabel = "NoTile";
        String tile1Label = "RealTile1";
        String tile2Label = "RealTile2";
        LocationConfig root = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(rootLabel).setType(LocationConfig.LocationType.ZONE).build());
        LocationConfig noTile = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(noTileLabel).setParentId(root.getId()).setType(LocationConfig.LocationType.REGION).build());
        LocationConfig tile1 = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(tile1Label).setParentId(root.getId()).setType(LocationConfig.LocationType.TILE).build());
        LocationConfig tile2 = remote.registerLocationConfig(LocationConfig.newBuilder().setLabel(tile2Label).setParentId(root.getId()).setType(LocationConfig.LocationType.TILE).build());

        String connectionFailLabel = "ConnectionFail";
        String connectionLabel = "TilesTestConnection";
        ConnectionConfig connectionFail = ConnectionConfig.newBuilder().setLabel(connectionFailLabel).setType(ConnectionConfig.ConnectionType.DOOR).addTileId(tile2.getId()).build();
        try {
            remote.registerConnectionConfig(connectionFail);
            Assert.fail("Registered connection with less than one tile");
        } catch (CouldNotPerformException ex) {
        }

        ConnectionConfig.Builder connectionBuilder = ConnectionConfig.newBuilder().setLabel(connectionLabel).setType(ConnectionConfig.ConnectionType.WINDOW);
        connectionBuilder.addTileId(noTile.getId());
        connectionBuilder.addTileId(tile1.getId());
        connectionBuilder.addTileId(tile1.getId());
        connectionBuilder.addTileId(tile2.getId());
        connectionBuilder.addTileId(root.getId());
        connectionBuilder.addTileId("fakeLocationId");
        ConnectionConfig connection = remote.registerConnectionConfig(connectionBuilder.build());

        assertEquals("Doubled tiles or locations that aren't tiles or that do not exists do not have been removed", 2, connection.getTileIdCount());
        assertTrue("The tile list does not contain the expected tile", connection.getTileIdList().contains(tile1.getId()));
        assertTrue("The tile list does not contain the expected tile", connection.getTileIdList().contains(tile2.getId()));
    }
}
