/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.scm.test;


/**
 *
 * @author mpohling
 */
public class SceneRegistryImplTest {

//    private static final Logger logger = LoggerFactory.getLogger(SceneRegistryImplTest.class);
//
//    public static final String LOCATION_LABEL = "paradise";
//    public static LocationConfig LOCATION;
//
//    private static SceneRegistryService sceneRegistry;
//    private static SceneClass.Builder sceneClass;
//    private static SceneConfig.Builder sceneConfig;
//
//    private static LocationRegistryService locationRegistry;
//
//    private static SceneClass.Builder sceneClassRemoteMessage;
//    private static SceneClass.Builder returnValue;
//    private static SceneConfig.Builder sceneConfigRemoteMessage;
//    private static SceneRegistryRemote remote;

    public SceneRegistryImplTest() {
    }

//    @BeforeClass
//    public static void setUpClass() throws InstantiationException, InitializationException, IOException, InvalidStateException, JPServiceException, InterruptedException, CouldNotPerformException {
//        JPService.registerProperty(JPInitializeDB.class, true);
//        JPService.registerProperty(JPSceneRegistryScope.class, new Scope("/test/scenemanager/registry/"));
//        JPService.registerProperty(JPLocationRegistryScope.class, new Scope("/test/locationmanager/registry/"));
//        JPService.registerProperty(JPSceneDatabaseDirectory.class, new File("/tmp/" + System.getProperty("user.name") + "/db/"));
//        JPService.registerProperty(JPSceneConfigDatabaseDirectory.class, new File("scene-config"));
//        JPService.registerProperty(JPSceneClassDatabaseDirectory.class, new File("scene-classes"));
//        JPService.setupJUnitTestMode();
//
//        sceneRegistry = new SceneRegistryService();
//        locationRegistry = new LocationRegistryService();
//
//        sceneRegistry.init();
//        locationRegistry.init();
//
//        Thread sceneRegistryThread = new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                try {
//                    sceneRegistry.activate();
//                } catch (CouldNotPerformException | InterruptedException ex) {
//                    ExceptionPrinter.printHistory(ex, logger);
//                }
//            }
//        });
//
//        Thread locationRegistryThread = new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                try {
//                    locationRegistry.activate();
//                } catch (CouldNotPerformException | InterruptedException ex) {
//                    ExceptionPrinter.printHistory(ex, logger);
//                }
//            }
//        });
//
//        sceneRegistryThread.start();
//        locationRegistryThread.start();
//
//        sceneRegistryThread.join();
//        locationRegistryThread.join();
//
//        sceneClass = SceneClass.getDefaultInstance().newBuilderForType();
//        sceneClass.setLabel("TestSceneClassLabel");
//        sceneClass.setCompany("MyCom");
//        sceneClass.setProductNumber("TDCL-001");
//        sceneConfig = SceneConfig.getDefaultInstance().newBuilderForType();
//        sceneConfig.setLabel("TestSceneConfigLabel");
//        sceneConfig.setSerialNumber("0001-0004-2245");
//        sceneConfig.setSceneClass(sceneClass.clone().setId("TestSceneClassLabel"));
//
//        sceneClassRemoteMessage = SceneClass.getDefaultInstance().newBuilderForType();
//        sceneClassRemoteMessage.setLabel("RemoteTestSceneClass").setProductNumber("ABR-132").setCompany("DreamCom");
//        sceneConfigRemoteMessage = SceneConfig.getDefaultInstance().newBuilderForType();
//        sceneConfigRemoteMessage.setLabel("RemoteTestSceneConfig").setSerialNumber("1123-5813-2134");
//        sceneConfigRemoteMessage.setSceneClass(sceneClassRemoteMessage.clone().setId("RemoteTestSceneClass"));
//
//        remote = new SceneRegistryRemote();
//        remote.init();
//        remote.activate();
//
//        LOCATION = locationRegistry.registerLocationConfig(LocationConfig.newBuilder().setLabel(LOCATION_LABEL).build());
//    }
//
//    @AfterClass
//    public static void tearDownClass() {
//        remote.shutdown();
//
//        if (locationRegistry != null) {
//            locationRegistry.shutdown();
//        }
//        if (sceneRegistry != null) {
//            sceneRegistry.shutdown();
//        }
//    }
//
//    @Before
//    public void setUp() {
//    }
//
//    @After
//    public void tearDown() {
//    }
//
//    /**
//     * Test of registerSceneClass method, of class SceneRegistryImpl.
//     */
//    @Test
//    public void testRegisterSceneClass() throws Exception {
//        System.out.println("registerSceneClass");
//        sceneRegistry.registerSceneClass(sceneClass.clone().build());
//        assertTrue(sceneRegistry.containsSceneClass(sceneClass.clone().build()));
////		assertEquals(true, registry.getData().getSceneClassesBuilderList().contains(sceneClass));
//    }
//
//    /**
//     * Test of registerSceneConfig method, of class SceneRegistryImpl.
//     */
//    @Test
//    public void testRegisterSceneConfig() throws Exception {
//        System.out.println("registerSceneConfig");
//        sceneRegistry.registerSceneConfig(sceneConfig.clone().build());
//        assertTrue(sceneRegistry.containsSceneConfig(sceneConfig.clone().build()));
//    }
//
//    /**
//     * Test of registerSceneConfigWithUnits method, of class
//     * SceneRegistryImpl.
//     *
//     * Test if the scope and the id of a scene configuration and its units is
//     * set when registered.
//     */
//    @Test
//    public void testRegisterSceneConfigWithUnits() throws Exception {
//        String productNumber = "ABCD-4321";
//        String serialNumber = "1234-WXYZ";
//        String company = "Fibaro";
//
//        String sceneId = company + "_" + productNumber + "_" + serialNumber;
//        String sceneLabel = "TestSensor";
//        String sceneScope = "/" + LOCATION_LABEL + "/" + sceneLabel.toLowerCase() + "/";
//
//        String unitLabel = "Battery";
//        String unitScope = "/" + LOCATION_LABEL + "/" + UnitTemplate.UnitType.BATTERY.name().toLowerCase() + "/" + unitLabel.toLowerCase() + "/";
//        String unitID = unitScope;
//
//        ArrayList<UnitConfig> units = new ArrayList<>();
//        SceneClass motionSensorClass = sceneRegistry.registerSceneClass(getSceneClass("F_MotionSensor", productNumber, company));
//        units.add(getUnitConfig(UnitTemplate.UnitType.BATTERY, unitLabel));
//        SceneConfig motionSensorConfig = getSceneConfig(sceneLabel, serialNumber, motionSensorClass, units);
//
//        motionSensorConfig = sceneRegistry.registerSceneConfig(motionSensorConfig);
//
//        assertEquals("Scene id is not set properly", sceneId, motionSensorConfig.getId());
//        assertEquals("Scene scope is not set properly", sceneScope, ScopeGenerator.generateStringRep(motionSensorConfig.getScope()));
//
//        assertEquals("Unit id is not set properly", unitID, motionSensorConfig.getUnitConfig(0).getId());
//        assertEquals("Unit scope is not set properly", unitScope, ScopeGenerator.generateStringRep(motionSensorConfig.getUnitConfig(0).getScope()));
//
//        assertEquals("Scene id is not set in unit", motionSensorConfig.getId(), motionSensorConfig.getUnitConfig(0).getSceneId());
//    }
//
//    /**
//     * Test of testRegiseredSceneConfigWithoutLabel method, of class
//     * SceneRegistryImpl.
//     */
//    @Test
//    public void testRegisteredSceneConfigWithoutLabel() throws Exception {
//        String productNumber = "KNHD-4321";
//        String serialNumber = "112358";
//        String company = "Company";
//
//        String sceneId = company + "_" + productNumber + "_" + serialNumber;
//
//        SceneClass clazz = sceneRegistry.registerSceneClass(getSceneClass("WithoutLabel", productNumber, company));
//        SceneConfig sceneWithoutLabel = getSceneConfig("", serialNumber, clazz, new ArrayList<UnitConfig>());
//        sceneWithoutLabel = sceneRegistry.registerSceneConfig(sceneWithoutLabel);
//
//        assertEquals("The scene label is not set as the id if it is empty!", sceneId, sceneWithoutLabel.getLabel());
//    }
//
//    /**
//     * Test of testRegisterTwoScenesWithSameLabel method, of class
//     * SceneRegistryImpl.
//     */
//    @Test
//    public void testRegisterTwoScenesWithSameLabel() throws Exception {
//        String serialNumber1 = "FIRST_DEV";
//        String serialNumber2 = "BAD_DEV";
//        String sceneLabel = "SameLabelSameLocation";
//
//        SceneClass clazz = sceneRegistry.registerSceneClass(getSceneClass("WithoutLabel", "xyz", "HuxGMBH"));
//        SceneConfig sceneWithLabel1 = getSceneConfig(sceneLabel, serialNumber1, clazz, new ArrayList<UnitConfig>());
//        SceneConfig sceneWithLabel2 = getSceneConfig(sceneLabel, serialNumber2, clazz, new ArrayList<UnitConfig>());
//
//        sceneRegistry.registerSceneConfig(sceneWithLabel1);
//        try {
//            sceneRegistry.registerSceneConfig(sceneWithLabel2);
//            fail("There was no exception thrown even though two scenes with the same label [" + sceneLabel + "] where registered in the same location [" + LOCATION_LABEL + "]");
//        } catch (Exception ex) {
//            assertTrue(true);
//        }
//    }
//
//    /**
//     * Test if the unit id of is set in the scene service.
//     */
//    @Test
//    public void testServiceConsistencyHandling() throws Exception {
//        UnitConfig unitConfig = getUnitConfig(UnitTemplate.UnitType.LIGHT, "ServiceTest");
//        BindingServiceConfig bindingConfig = BindingServiceConfig.newBuilder().setType(BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB).build();
//        ServiceConfig serviceConfig = ServiceConfig.newBuilder().setType(ServiceType.POWER_PROVIDER).setBindingServiceConfig(bindingConfig).build();
//        unitConfig = unitConfig.toBuilder().addServiceConfig(serviceConfig).build();
//        ArrayList<UnitConfig> units = new ArrayList<>();
//        units.add(unitConfig);
//
//        SceneClass clazz = sceneRegistry.registerSceneClass(getSceneClass("ServiceUnitIdTest", "8383838", "ServiceGMBH"));
//
//        SceneConfig sceneConfig = sceneRegistry.registerSceneConfig(getSceneConfig("ServiceTest", "123456", clazz, units));
//
////        assertTrue("Unit id is not set.", !sceneConfig.getUnitConfig(0).getId().equals(""));
////        assertTrue("Unit id in service config is not set.", !sceneConfig.getUnitConfig(0).getServiceConfig(0).getUnitId().equals(""));
////        assertTrue("Unit id in service config does not match id in unit config.", sceneConfig.getUnitConfig(0).getServiceConfig(0).getUnitId().equals(sceneConfig.getUnitConfig(0).getId()));
//        String itemId = OpenhabServiceConfigItemIdConsistenyHandler.generateItemName(sceneConfig, unitConfig, serviceConfig, LOCATION);
//
////        assertTrue("OpenHAB item id is not set.", itemId.equals(sceneConfig.getUnitConfig(0).getServiceConfig(0).getBindingServiceConfig().getOpenhabBindingServiceConfig().getItemId()));
//    }
//
//    private PlacementConfigType.PlacementConfig getDefaultPlacement() {
//        RotationType.Rotation rotation = RotationType.Rotation.newBuilder().setQw(1).setQx(0).setQy(0).setQz(0).build();
//        TranslationType.Translation translation = TranslationType.Translation.newBuilder().setX(0).setY(0).setZ(0).build();
//        PoseType.Pose pose = PoseType.Pose.newBuilder().setRotation(rotation).setTranslation(translation).build();
//        ScopeType.Scope.Builder locationScope = ScopeType.Scope.newBuilder().addComponent(LOCATION_LABEL);
//        return PlacementConfigType.PlacementConfig.newBuilder().setPosition(pose).setLocationId(LOCATION_LABEL).build();
//    }
//
//    private UnitConfig getUnitConfig(UnitTemplate.UnitType type, String label) {
//        return UnitConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setTemplate(UnitTemplate.newBuilder().setType(type).build()).setLabel(label).build();
//    }
//
//    private SceneConfig getSceneConfig(String label, String serialNumber, SceneClass clazz, ArrayList<UnitConfig> units) {
//        return SceneConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setLabel(label).setSerialNumber(serialNumber).setSceneClass(clazz).addAllUnitConfig(units).build();
//    }
//
//    private SceneClass getSceneClass(String label, String productNumber, String company) {
//        return SceneClass.newBuilder().setLabel(label).setProductNumber(productNumber).setCompany(company).build();
//    }
//
//    /**
//     * Test of registering a SceneClass per remote.
//     */
//    @Test(timeout = 5000)
//    public void testRegisterSceneClassPerRemote() throws Exception {
//        System.out.println("registerSceneClassPerRemote");
//
//        remote.addObserver(new Observer<SceneRegistryType.SceneRegistry>() {
//
//            @Override
//            public void update(Observable<SceneRegistryType.SceneRegistry> source, SceneRegistryType.SceneRegistry data) throws Exception {
//                if (data != null) {
//                    logger.info("Got empty data!");
//                } else {
//                    logger.info("Got data update: " + data);
//                }
//            }
//        });
//
//        returnValue = remote.registerSceneClass(sceneClassRemoteMessage.clone().build()).toBuilder();
//        logger.info("Returned scene class id [" + returnValue.getId() + "]");
//        sceneClassRemoteMessage.setId(returnValue.getId());
//
//        while (true) {
//            try {
//                if (remote.getData().getSceneClassList().contains(sceneClassRemoteMessage.clone().build())) {
//                    break;
//                }
//            } catch (NotAvailableException ex) {
//                logger.debug("Not ready yet");
//            }
//            Thread.yield();
//        }
//        assertTrue(remote.containsSceneClass(sceneClassRemoteMessage.clone().build()));
//    }
//
//    /**
//     * Test of registering a SceneConfig per remote.
//     */
//    @Test(timeout = 3000)
//    public void testRegisterSceneConfigPerRemote() throws Exception {
//        System.out.println("registerSceneConfigPerRemote");
//        remote.registerSceneConfig(sceneConfigRemoteMessage.clone().build());
//        while (true) {
//            if (remote.containsSceneConfig(sceneConfigRemoteMessage.clone().build())) {
//                break;
//            }
//            Thread.yield();
//        }
//        assertTrue(remote.containsSceneConfig(sceneConfigRemoteMessage.clone().build()));
//    }
}
