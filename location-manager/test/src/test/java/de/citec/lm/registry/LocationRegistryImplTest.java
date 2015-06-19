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
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.storage.jp.JPInitializeDB;
import de.citec.lm.core.registry.LocationRegistryService;
import de.citec.lm.remote.LocationRegistryRemote;
import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rst.geometry.PoseType.Pose;
import rst.geometry.RotationType.Rotation;
import rst.geometry.TranslationType.Translation;
import rst.homeautomation.unit.UnitTemplateType;
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
        JPService.registerProperty(JPLocationDatabaseDirectory.class, new File("/tmp/db/"));
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
                    ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
                }
            }
        });

        Thread locationRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    locationRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
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
        assertTrue("The new location isn't registered as a root location.", registeredRoot.getRoot());

        LocationConfig child = LocationConfig.newBuilder().setLabel("TestChildLocation").setParentId(registeredRoot.getId()).build();
        LocationConfig registeredChild = remote.registerLocationConfig(child);
        assertTrue("The new location isn't registered as a child location.", !registeredChild.getRoot());
        remote.requestStatus();
        assertTrue("The child location isn't represented in its parent.", remote.getLocationConfigById(registeredRoot.getId()).getChildList().contains(registeredChild));
        assertTrue("The root node contains more than one child.", remote.getLocationConfigById(registeredRoot.getId()).getChildCount() == 1);

        LocationConfig removedLocation = remote.removeLocationConfig(registeredRoot);
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
        LocationConfig living = LocationConfig.newBuilder().setLabel("Test2Living").build();
        LocationConfig registeredLiving = remote.registerLocationConfig(living);
        assertTrue("The new location isn't registered as a root location.", registeredLiving.getRoot());

        LocationConfig home = LocationConfig.newBuilder().setLabel("Test2Home").addChild(living).build();
        LocationConfig registeredHome = remote.registerLocationConfig(home);
        assertTrue("The new location isn't registered as a root location.", registeredHome.getRoot());
        remote.requestStatus();
        assertFalse("Root hasn't become a child location after setting its parent.", remote.getLocationConfigById(registeredLiving.getId()).getRoot());
    }
    
    @Test
    public void testPositionChanges() throws Exception {
        LocationConfig root = LocationConfig.newBuilder().setLabel("RootPosition").build();
        root = remote.registerLocationConfig(root);
        assertTrue("The new location isn't registered as a root location.", root.getRoot());

        LocationConfig child = LocationConfig.newBuilder().setLabel("ChildPosition").setParentId(root.getId()).build();
        child = remote.registerLocationConfig(child);
        assertTrue("The new location isn't registered as a child location.", !child.getRoot());
        remote.requestStatus(); 
        
        Translation translation = Translation.newBuilder().setX(1).setY(2).setZ(3).build();
        Rotation rotation = Rotation.newBuilder().setQw(1).setQx(2).setQy(3).setQz(4).build();
        Pose position = Pose.newBuilder().setRotation(rotation).setTranslation(translation).build();
        root = root.toBuilder().setPosition(position).build();
        root = remote.updateLocationConfig(root);
        assertEquals("The position for the root location has not been updated", position, root.getPosition());
        
        child = child.toBuilder().setPosition(position).build();
        root = root.toBuilder().setChild(0, child).build();
        root = remote.updateLocationConfig(root);
        assertEquals("The position for the child location has not been updated", position, root.getChild(0).getPosition());
    }

    @Test
    public void testGetUnitConfigs() throws Exception {
        try {
            remote.getUnitConfigs(UnitTemplateType.UnitTemplate.UnitType.UNKNOWN, locationConfig.getId());
            assertTrue("Exception handling failed!", false);
        } catch (CouldNotPerformException ex) {
            // this should happen id unit type is unknown!
        }
        
        try {
            remote.getUnitConfigs(UnitTemplateType.UnitTemplate.UnitType.AMBIENT_LIGHT, "Quark");
            assertTrue("Exception handling failed!", false);
        } catch (CouldNotPerformException ex) {
            // this should happen id unit type is unknown!
        }
    }
}
