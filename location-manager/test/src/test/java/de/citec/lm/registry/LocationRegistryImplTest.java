/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.registry;

import de.citec.dm.core.registry.DeviceRegistryService;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jp.JPLocationConfigDatabaseDirectory;
import de.citec.jp.JPLocationDatabaseDirectory;
import de.citec.jp.JPLocationRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jps.exception.JPServiceException;
import de.citec.jps.preset.JPVerbose;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.extension.rsb.scope.ScopeGenerator;
import de.citec.jul.storage.registry.jp.JPInitializeDB;
import de.citec.lm.core.registry.LocationRegistryService;
import de.citec.lm.remote.LocationRegistryRemote;
import java.io.File;
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
import rsb.Scope;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class LocationRegistryImplTest {

    private static final Logger logger = LoggerFactory.getLogger(LocationRegistryImplTest.class);

    private static DeviceRegistryService deviceRegistry;

    private static LocationRegistryService locationRegistry;
    private static LocationConfigType.LocationConfig.Builder locationConfig;

    private static LocationConfigType.LocationConfig.Builder locationConfigRemote;
    private static LocationRegistryRemote remote;

    public LocationRegistryImplTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InstantiationException, InitializationException, IOException, JPServiceException, InterruptedException, CouldNotPerformException {

        JPService.registerProperty(JPInitializeDB.class, true);
        JPService.registerProperty(JPLocationRegistryScope.class, new Scope("/test/locationmanager/registry"));
        JPService.registerProperty(JPDeviceRegistryScope.class, new Scope("/test/devicemanager/registry/"));
        JPService.registerProperty(JPLocationDatabaseDirectory.class, new File("/tmp/" + System.getProperty("user.name") + "/db/"));
        JPService.registerProperty(JPLocationConfigDatabaseDirectory.class, new File("location-config"));
        JPService.registerProperty(JPVerbose.class, true);
        JPService.setupJUnitTestMode();

        deviceRegistry = new DeviceRegistryService();
        locationRegistry = new LocationRegistryService();

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
}
