package org.openbase.bco.registry.unit.test;

/*
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
import org.openbase.bco.registry.agent.core.AgentRegistryController;
import org.openbase.bco.registry.app.core.AppRegistryController;
import org.openbase.bco.registry.device.core.DeviceRegistryController;
import org.openbase.bco.registry.unit.core.UnitRegistryController;
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
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.connection.ConnectionConfigType.ConnectionConfig;
import rst.domotic.unit.location.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class LocationRegistryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationRegistryTest.class);

    private static UnitRegistryController unitRegistry;
    private static DeviceRegistryController deviceRegistry;
    private static AppRegistryController appRegistry;
    private static AgentRegistryController agentRegistry;

    public LocationRegistryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();
            JPService.registerProperty(JPDebugMode.class, true);

            try {
                unitRegistry = new UnitRegistryController();
                deviceRegistry = new DeviceRegistryController();
                appRegistry = new AppRegistryController();
                agentRegistry = new AgentRegistryController();

                unitRegistry.init();
                deviceRegistry.init();
                appRegistry.init();
                agentRegistry.init();
            } catch (Throwable ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
            }

            Thread unitRegistryThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        unitRegistry.activate();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, LOGGER);
                    }
                }
            });
            Thread deviceRegistryThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        deviceRegistry.activate();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, LOGGER);
                    }
                }
            });
            Thread appRegistryThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        appRegistry.activate();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, LOGGER);
                    }
                }
            });
            Thread agentRegistryThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        agentRegistry.activate();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, LOGGER);
                    }
                }
            });

            unitRegistryThread.start();
            deviceRegistryThread.start();
            appRegistryThread.start();
            agentRegistryThread.start();

            unitRegistryThread.join();
            deviceRegistryThread.join();
            appRegistryThread.join();
            agentRegistryThread.join();
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws InterruptedException, CouldNotPerformException {
        try {
            if (unitRegistry != null) {
                unitRegistry.shutdown();
            }
            if (deviceRegistry != null) {
                deviceRegistry.shutdown();
            }
            if (appRegistry != null) {
                appRegistry.shutdown();
            }
            if (agentRegistry != null) {
                agentRegistry.shutdown();
            }
        } catch (Throwable ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() throws CouldNotPerformException {
        unitRegistry.getConnectionUnitConfigRegistry().clear();
        unitRegistry.getLocationUnitConfigRegistry().clear();
    }

    @After
    public void tearDown() {
    }

    private static UnitConfig.Builder getLocationUnitBuilder() {
        return UnitConfig.newBuilder().setType(UnitTemplate.UnitType.LOCATION).setLocationConfig(LocationConfig.getDefaultInstance());
    }

    private static UnitConfig.Builder getLocationUnitBuilder(LocationConfig.LocationType locationType) {
        LocationConfig locationConfig = LocationConfig.getDefaultInstance().toBuilder().setType(locationType).build();
        return UnitConfig.newBuilder().setType(UnitTemplate.UnitType.LOCATION).setLocationConfig(locationConfig);
    }

    private static UnitConfig.Builder getConnectionUnitBuilder() {
        return UnitConfig.newBuilder().setType(UnitTemplate.UnitType.CONNECTION).setConnectionConfig(ConnectionConfig.getDefaultInstance());
    }

    private static UnitConfig.Builder getConnectionUnitBuilder(ConnectionConfig.ConnectionType connectionType) {
        ConnectionConfig connectionConfig = ConnectionConfig.getDefaultInstance().toBuilder().setType(connectionType).build();
        return UnitConfig.newBuilder().setType(UnitTemplate.UnitType.LOCATION).setConnectionConfig(connectionConfig);
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
        UnitConfig root = getLocationUnitBuilder().setLabel("TestRootLocation").build();
        UnitConfig registeredRoot = unitRegistry.registerUnitConfig(root).get();

        assertTrue("The new location isn't registered as a root location.", registeredRoot.getLocationConfig().getRoot());
        assertEquals("Wrong location scope", "/testrootlocation/", ScopeGenerator.generateStringRep(registeredRoot.getScope()));

        UnitConfig child = getLocationUnitBuilder().setLabel("TestChildLocation").setPlacementConfig(PlacementConfig.newBuilder().setLocationId(registeredRoot.getId()).build()).build();
        UnitConfig registeredChild = unitRegistry.registerUnitConfig(child).get();
        assertTrue("The new location isn't registered as a child location.", !registeredChild.getLocationConfig().getRoot());
        assertTrue("The child location isn't represented in its parent.", unitRegistry.getUnitConfigById(registeredRoot.getId()).getLocationConfig().getChildIdList().contains(registeredChild.getId()));
        assertTrue("The root node contains more than one child.", unitRegistry.getUnitConfigById(registeredRoot.getId()).getLocationConfig().getChildIdCount() == 1);

        UnitConfig removedLocation = unitRegistry.removeUnitConfig(registeredRoot).get();
        assertFalse("The deleted root location is still available.", unitRegistry.containsUnitConfig(removedLocation));
        assertTrue("Child hasn't become a root location after the removal of its parent.", unitRegistry.getUnitConfigById(registeredChild.getId()).getLocationConfig().getRoot());
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
        UnitConfig living = getLocationUnitBuilder().setLabel(label).build();
        UnitConfig registeredLiving = unitRegistry.registerUnitConfig(living).get();
        assertTrue("The new location isn't registered as a root location.", registeredLiving.getLocationConfig().getRoot());
        assertEquals("Label has not been set", label, registeredLiving.getLabel());

        String rootLocationConfigLabel = "Test3RootLocation";
        UnitConfig rootLocationConfig = getLocationUnitBuilder().setLabel(rootLocationConfigLabel).build();
        UnitConfig registeredRootLocationConfig = unitRegistry.registerUnitConfig(rootLocationConfig).get();
        UnitConfig.Builder registeredLivingBuilder = unitRegistry.getUnitConfigById(registeredLiving.getId()).toBuilder();
        registeredLivingBuilder.getPlacementConfigBuilder().setLocationId(registeredRootLocationConfig.getId());
        unitRegistry.updateUnitConfig(registeredLivingBuilder.build()).get();
        assertEquals("Parent was not updated!", registeredRootLocationConfig.getId(), registeredLivingBuilder.getPlacementConfig().getLocationId());

        UnitConfig home = getLocationUnitBuilder().setLabel("Test2Home").build();
        UnitConfig registeredHome = unitRegistry.registerUnitConfig(home).get();
        registeredLivingBuilder = unitRegistry.getUnitConfigById(registeredRootLocationConfig.getId()).toBuilder();
        registeredLivingBuilder.getPlacementConfigBuilder().setLocationId(registeredHome.getId());
        assertEquals("Parent was not updated!", registeredHome.getId(), unitRegistry.updateUnitConfig(registeredLivingBuilder.build()).get().getPlacementConfig().getLocationId());
    }

    /**
     * Test if a root location becomes a child after it is set as a child of
     * root locations.
     *
     * @throws Exception
     */
    @Test(timeout = 5000)
    public void testParentIdUpdateConsistency() throws Exception {
        System.out.println("testParentIdUpdateConsistency");

        String rootLocationConfigLabel = "Test3RootLocation";
        UnitConfig rootLocationConfig = getLocationUnitBuilder().setLabel(rootLocationConfigLabel).build();
        UnitConfig registeredRootLocationConfig = unitRegistry.registerUnitConfig(rootLocationConfig).get();

        String childLocationConfigLabel = "Test3ChildLocation";
        UnitConfig.Builder childLocationConfigBuilder = getLocationUnitBuilder();
        childLocationConfigBuilder.setLabel(childLocationConfigLabel);
        childLocationConfigBuilder.getPlacementConfigBuilder().setLocationId(registeredRootLocationConfig.getId());
        UnitConfig registeredChildLocationConfig = unitRegistry.registerUnitConfig(childLocationConfigBuilder.build()).get();

        String parentLabel = "Test3ParentLocation";
        UnitConfig.Builder parentLocationConfigBuilder = getLocationUnitBuilder().setLabel(parentLabel);
        UnitConfig registeredParentLocationConfig = unitRegistry.registerUnitConfig(parentLocationConfigBuilder.build()).get();

        assertEquals("The new location isn't registered as child of Location[" + rootLocationConfigLabel + "]!", registeredRootLocationConfig.getId(), registeredChildLocationConfig.getPlacementConfig().getLocationId());

        UnitConfig.Builder registeredChildLocationConfigBuilder = registeredChildLocationConfig.toBuilder();
        registeredChildLocationConfigBuilder.getPlacementConfigBuilder().setLocationId(registeredParentLocationConfig.getId());
        registeredChildLocationConfig = unitRegistry.updateUnitConfig(registeredChildLocationConfigBuilder.build()).get();

        assertEquals("The parent location of child was not updated as new placement location id after update.", registeredParentLocationConfig.getId(), registeredChildLocationConfig.getPlacementConfig().getLocationId());
        assertEquals("The parent location of child was not updated as new placement location id in global registry.", registeredParentLocationConfig.getId(), unitRegistry.getUnitConfigsByLabel(childLocationConfigLabel).get(0).getPlacementConfig().getLocationId());
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
        UnitConfig root = getLocationUnitBuilder().setLabel(rootLabel).build();
        root = unitRegistry.registerUnitConfig(root).get();

        UnitConfig firstChild = getLocationUnitBuilder().setLabel(firstChildLabel).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).build();
        unitRegistry.registerUnitConfig(firstChild);

        UnitConfig secondChild = getLocationUnitBuilder().setLabel(SecondChildLabel).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).build();
        secondChild = unitRegistry.registerUnitConfig(secondChild).get();

        try {
            // register loop
            root = unitRegistry.getUnitConfigById(root.getId());
            UnitConfig.Builder rootBuilder = root.toBuilder();
            rootBuilder.getPlacementConfigBuilder().setLocationId(secondChild.getId());
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            unitRegistry.registerUnitConfig(rootBuilder.build()).get();
            Assert.fail("No exception when registering location with a loop [" + secondChild + "]");
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
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
        UnitConfig root = getLocationUnitBuilder().setLabel(rootLabel).build();
        root = unitRegistry.registerUnitConfig(root).get();

        UnitConfig firstChild = getLocationUnitBuilder().setLabel(childLabel).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).build();
        unitRegistry.registerUnitConfig(firstChild).get();

        try {
            UnitConfig secondChild = getLocationUnitBuilder().setLabel(childLabel).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).build();
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            unitRegistry.registerUnitConfig(secondChild).get();
            Assert.fail("No exception thrown when registering a second child with the same label");
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
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
        UnitConfig root = unitRegistry.registerUnitConfig(getLocationUnitBuilder(LocationConfig.LocationType.ZONE).setLabel(rootLabel).build()).get();
        UnitConfig zone = unitRegistry.registerUnitConfig(getLocationUnitBuilder(LocationConfig.LocationType.ZONE).setLabel(zoneLabel).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).build()).get();
        UnitConfig tile1 = unitRegistry.registerUnitConfig(getLocationUnitBuilder(LocationConfig.LocationType.TILE).setLabel(tile1Label).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).build()).get();
        UnitConfig tile2 = unitRegistry.registerUnitConfig(getLocationUnitBuilder(LocationConfig.LocationType.TILE).setLabel(tile2Label).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(zone.getId())).build()).get();
        UnitConfig tile3 = unitRegistry.registerUnitConfig(getLocationUnitBuilder(LocationConfig.LocationType.TILE).setLabel(tile3Label).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(zone.getId())).build()).get();

        String connection1Label = "Connection1";
        String connection2Label = "Connection2";
        ConnectionConfig connectionConfig1 = ConnectionConfig.newBuilder().setType(ConnectionConfig.ConnectionType.DOOR).addTileId(tile1.getId()).addTileId(tile2.getId()).build();
        ConnectionConfig connectionConfig2 = ConnectionConfig.newBuilder().setType(ConnectionConfig.ConnectionType.WINDOW).addTileId(tile2.getId()).addTileId(tile3.getId()).build();
        UnitConfig connection1 = unitRegistry.registerUnitConfig(getConnectionUnitBuilder().setLabel(connection1Label).setConnectionConfig(connectionConfig1).build()).get();
        UnitConfig connection2 = unitRegistry.registerUnitConfig(getConnectionUnitBuilder().setLabel(connection2Label).setConnectionConfig(connectionConfig2).build()).get();

        assertEquals(root.getId(), connection1.getPlacementConfig().getLocationId());
        assertEquals(zone.getId(), connection2.getPlacementConfig().getLocationId());

        assertEquals("/rootzoneforconnectiontest/connection/connection1/", ScopeGenerator.generateStringRep(connection1.getScope()));
        assertEquals(ScopeGenerator.generateConnectionScope(connection2, zone), connection2.getScope());

        ConnectionConfig connectionConfig3 = ConnectionConfig.newBuilder().setType(ConnectionConfig.ConnectionType.PASSAGE).addTileId(tile2.getId()).addTileId(tile3.getId()).build();
        UnitConfig connection3 = getConnectionUnitBuilder().setConnectionConfig(connectionConfig3).build();
        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            unitRegistry.registerUnitConfig(connection3).get();
            Assert.fail("No exception thrown when registering a second connection at the same location with the same label");
        } catch (Throwable ex) {
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
        UnitConfig root = unitRegistry.registerUnitConfig(getLocationUnitBuilder(LocationConfig.LocationType.ZONE).setLabel(rootLabel).build()).get();
        UnitConfig noTile = unitRegistry.registerUnitConfig(getLocationUnitBuilder(LocationConfig.LocationType.REGION).setLabel(noTileLabel).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).build()).get();
        UnitConfig tile1 = unitRegistry.registerUnitConfig(getLocationUnitBuilder(LocationConfig.LocationType.TILE).setLabel(tile1Label).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).build()).get();
        UnitConfig tile2 = unitRegistry.registerUnitConfig(getLocationUnitBuilder(LocationConfig.LocationType.TILE).setLabel(tile2Label).setPlacementConfig(PlacementConfig.newBuilder().setLocationId(root.getId())).build()).get();

        String connectionFailLabel = "ConnectionFail";
        String connectionLabel = "TilesTestConnection";
        ConnectionConfig connectionFail = ConnectionConfig.newBuilder().setType(ConnectionConfig.ConnectionType.DOOR).addTileId(tile2.getId()).build();
        UnitConfig connectionUnitFail = getConnectionUnitBuilder().setLabel(connectionFailLabel).setConnectionConfig(connectionFail).build();
        try {
            ExceptionPrinter.setBeQuit(Boolean.TRUE);
            unitRegistry.registerUnitConfig(connectionUnitFail).get();
            Assert.fail("Registered connection with less than one tile");
        } catch (ExecutionException ex) {
        } finally {
            ExceptionPrinter.setBeQuit(Boolean.FALSE);
        }

        ConnectionConfig.Builder connectionBuilder = ConnectionConfig.newBuilder().setType(ConnectionConfig.ConnectionType.WINDOW);
        connectionBuilder.addTileId(noTile.getId());
        connectionBuilder.addTileId(tile1.getId());
        connectionBuilder.addTileId(tile1.getId());
        connectionBuilder.addTileId(tile2.getId());
        connectionBuilder.addTileId(root.getId());
        connectionBuilder.addTileId("fakeLocationId");
        UnitConfig connectionUnit = unitRegistry.registerUnitConfig(getConnectionUnitBuilder().setLabel(connectionLabel).setConnectionConfig(connectionBuilder.build()).build()).get();
        ConnectionConfig connection = connectionUnit.getConnectionConfig();

        assertEquals("Doubled tiles or locations that aren't tiles or that do not exists do not have been removed", 2, connection.getTileIdCount());
        assertTrue("The tile list does not contain the expected tile", connection.getTileIdList().contains(tile1.getId()));
        assertTrue("The tile list does not contain the expected tile", connection.getTileIdList().contains(tile2.getId()));
    }
}
