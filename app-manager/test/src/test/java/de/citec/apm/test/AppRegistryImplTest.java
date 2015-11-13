/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.apm.test;

/**
 *
 * @author mpohling
 */
public class AppRegistryImplTest {
//
//    private static final Logger logger = LoggerFactory.getLogger(AppRegistryImplTest.class);
//
//    public static final String LOCATION_LABEL = "paradise";
//    public static LocationConfig LOCATION;
//
//    private static AppRegistryService appRegistry;
//    private static AppClass.Builder appClass;
//    private static AppConfig.Builder appConfig;
//
//    private static LocationRegistryService locationRegistry;
//
//    private static AppClass.Builder appClassRemoteMessage;
//    private static AppClass.Builder returnValue;
//    private static AppConfig.Builder appConfigRemoteMessage;
//    private static AppRegistryRemote remote;
//
//    public AppRegistryImplTest() {
//    }
//
//    @BeforeClass
//    public static void setUpClass() throws InstantiationException, InitializationException, IOException, InvalidStateException, JPServiceException, InterruptedException, CouldNotPerformException {
//        JPService.registerProperty(JPInitializeDB.class, true);
//        JPService.registerProperty(JPAppRegistryScope.class, new Scope("/test/appmanager/registry/"));
//        JPService.registerProperty(JPLocationRegistryScope.class, new Scope("/test/locationmanager/registry/"));
//        JPService.registerProperty(JPAppDatabaseDirectory.class, new File("/tmp/" + System.getProperty("user.name") + "/db/"));
//        JPService.registerProperty(JPAppConfigDatabaseDirectory.class, new File("app-config"));
//        JPService.registerProperty(JPAppClassDatabaseDirectory.class, new File("app-classes"));
//        JPService.setupJUnitTestMode();
//
//        appRegistry = new AppRegistryService();
//        locationRegistry = new LocationRegistryService();
//
//        appRegistry.init();
//        locationRegistry.init();
//
//        Thread appRegistryThread = new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                try {
//                    appRegistry.activate();
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
//        appRegistryThread.start();
//        locationRegistryThread.start();
//
//        appRegistryThread.join();
//        locationRegistryThread.join();
//
//        appClass = AppClass.getDefaultInstance().newBuilderForType();
//        appClass.setLabel("TestAppClassLabel");
//        appClass.setCompany("MyCom");
//        appClass.setProductNumber("TDCL-001");
//        appConfig = AppConfig.getDefaultInstance().newBuilderForType();
//        appConfig.setLabel("TestAppConfigLabel");
//        appConfig.setSerialNumber("0001-0004-2245");
//        appConfig.setAppClass(appClass.clone().setId("TestAppClassLabel"));
//
//        appClassRemoteMessage = AppClass.getDefaultInstance().newBuilderForType();
//        appClassRemoteMessage.setLabel("RemoteTestAppClass").setProductNumber("ABR-132").setCompany("DreamCom");
//        appConfigRemoteMessage = AppConfig.getDefaultInstance().newBuilderForType();
//        appConfigRemoteMessage.setLabel("RemoteTestAppConfig").setSerialNumber("1123-5813-2134");
//        appConfigRemoteMessage.setAppClass(appClassRemoteMessage.clone().setId("RemoteTestAppClass"));
//
//        remote = new AppRegistryRemote();
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
//        if (appRegistry != null) {
//            appRegistry.shutdown();
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
//     * Test of registerAppClass method, of class AppRegistryImpl.
//     */
//    @Test
//    public void testRegisterAppClass() throws Exception {
//        System.out.println("registerAppClass");
//        appRegistry.registerAppClass(appClass.clone().build());
//        assertTrue(appRegistry.containsAppClass(appClass.clone().build()));
////		assertEquals(true, registry.getData().getAppClassesBuilderList().contains(appClass));
//    }
//
//    /**
//     * Test of registerAppConfig method, of class AppRegistryImpl.
//     */
//    @Test
//    public void testRegisterAppConfig() throws Exception {
//        System.out.println("registerAppConfig");
//        appRegistry.registerAppConfig(appConfig.clone().build());
//        assertTrue(appRegistry.containsAppConfig(appConfig.clone().build()));
//    }
//
//    /**
//     * Test of registerAppConfigWithUnits method, of class
//     * AppRegistryImpl.
//     *
//     * Test if the scope and the id of a app configuration and its units is
//     * set when registered.
//     */
//    @Test
//    public void testRegisterAppConfigWithUnits() throws Exception {
//        String productNumber = "ABCD-4321";
//        String serialNumber = "1234-WXYZ";
//        String company = "Fibaro";
//
//        String appId = company + "_" + productNumber + "_" + serialNumber;
//        String appLabel = "TestSensor";
//        String appScope = "/" + LOCATION_LABEL + "/" + appLabel.toLowerCase() + "/";
//
//        String unitLabel = "Battery";
//        String unitScope = "/" + LOCATION_LABEL + "/" + UnitTemplate.UnitType.BATTERY.name().toLowerCase() + "/" + unitLabel.toLowerCase() + "/";
//        String unitID = unitScope;
//
//        ArrayList<UnitConfig> units = new ArrayList<>();
//        AppClass motionSensorClass = appRegistry.registerAppClass(getAppClass("F_MotionSensor", productNumber, company));
//        units.add(getUnitConfig(UnitTemplate.UnitType.BATTERY, unitLabel));
//        AppConfig motionSensorConfig = getAppConfig(appLabel, serialNumber, motionSensorClass, units);
//
//        motionSensorConfig = appRegistry.registerAppConfig(motionSensorConfig);
//
//        assertEquals("App id is not set properly", appId, motionSensorConfig.getId());
//        assertEquals("App scope is not set properly", appScope, ScopeGenerator.generateStringRep(motionSensorConfig.getScope()));
//
//        assertEquals("Unit id is not set properly", unitID, motionSensorConfig.getUnitConfig(0).getId());
//        assertEquals("Unit scope is not set properly", unitScope, ScopeGenerator.generateStringRep(motionSensorConfig.getUnitConfig(0).getScope()));
//
//        assertEquals("App id is not set in unit", motionSensorConfig.getId(), motionSensorConfig.getUnitConfig(0).getAppId());
//    }
//
//    /**
//     * Test of testRegiseredAppConfigWithoutLabel method, of class
//     * AppRegistryImpl.
//     */
//    @Test
//    public void testRegisteredAppConfigWithoutLabel() throws Exception {
//        String productNumber = "KNHD-4321";
//        String serialNumber = "112358";
//        String company = "Company";
//
//        String appId = company + "_" + productNumber + "_" + serialNumber;
//
//        AppClass clazz = appRegistry.registerAppClass(getAppClass("WithoutLabel", productNumber, company));
//        AppConfig appWithoutLabel = getAppConfig("", serialNumber, clazz, new ArrayList<UnitConfig>());
//        appWithoutLabel = appRegistry.registerAppConfig(appWithoutLabel);
//
//        assertEquals("The app label is not set as the id if it is empty!", appId, appWithoutLabel.getLabel());
//    }
//
//    /**
//     * Test of testRegisterTwoAppsWithSameLabel method, of class
//     * AppRegistryImpl.
//     */
//    @Test
//    public void testRegisterTwoAppsWithSameLabel() throws Exception {
//        String serialNumber1 = "FIRST_DEV";
//        String serialNumber2 = "BAD_DEV";
//        String appLabel = "SameLabelSameLocation";
//
//        AppClass clazz = appRegistry.registerAppClass(getAppClass("WithoutLabel", "xyz", "HuxGMBH"));
//        AppConfig appWithLabel1 = getAppConfig(appLabel, serialNumber1, clazz, new ArrayList<UnitConfig>());
//        AppConfig appWithLabel2 = getAppConfig(appLabel, serialNumber2, clazz, new ArrayList<UnitConfig>());
//
//        appRegistry.registerAppConfig(appWithLabel1);
//        try {
//            appRegistry.registerAppConfig(appWithLabel2);
//            fail("There was no exception thrown even though two apps with the same label [" + appLabel + "] where registered in the same location [" + LOCATION_LABEL + "]");
//        } catch (Exception ex) {
//            assertTrue(true);
//        }
//    }
//
//    /**
//     * Test if the unit id of is set in the app service.
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
//        AppClass clazz = appRegistry.registerAppClass(getAppClass("ServiceUnitIdTest", "8383838", "ServiceGMBH"));
//
//        AppConfig appConfig = appRegistry.registerAppConfig(getAppConfig("ServiceTest", "123456", clazz, units));
//
////        assertTrue("Unit id is not set.", !appConfig.getUnitConfig(0).getId().equals(""));
////        assertTrue("Unit id in service config is not set.", !appConfig.getUnitConfig(0).getServiceConfig(0).getUnitId().equals(""));
////        assertTrue("Unit id in service config does not match id in unit config.", appConfig.getUnitConfig(0).getServiceConfig(0).getUnitId().equals(appConfig.getUnitConfig(0).getId()));
//        String itemId = OpenhabServiceConfigItemIdConsistenyHandler.generateItemName(appConfig, unitConfig, serviceConfig, LOCATION);
//
////        assertTrue("OpenHAB item id is not set.", itemId.equals(appConfig.getUnitConfig(0).getServiceConfig(0).getBindingServiceConfig().getOpenhabBindingServiceConfig().getItemId()));
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
//    private AppConfig getAppConfig(String label, String serialNumber, AppClass clazz, ArrayList<UnitConfig> units) {
//        return AppConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setLabel(label).setSerialNumber(serialNumber).setAppClass(clazz).addAllUnitConfig(units).build();
//    }
//
//    private AppClass getAppClass(String label, String productNumber, String company) {
//        return AppClass.newBuilder().setLabel(label).setProductNumber(productNumber).setCompany(company).build();
//    }
//
//    /**
//     * Test of registering a AppClass per remote.
//     */
//    @Test(timeout = 5000)
//    public void testRegisterAppClassPerRemote() throws Exception {
//        System.out.println("registerAppClassPerRemote");
//
//        remote.addObserver(new Observer<AppRegistryType.AppRegistry>() {
//
//            @Override
//            public void update(Observable<AppRegistryType.AppRegistry> source, AppRegistryType.AppRegistry data) throws Exception {
//                if (data != null) {
//                    logger.info("Got empty data!");
//                } else {
//                    logger.info("Got data update: " + data);
//                }
//            }
//        });
//
//        returnValue = remote.registerAppClass(appClassRemoteMessage.clone().build()).toBuilder();
//        logger.info("Returned app class id [" + returnValue.getId() + "]");
//        appClassRemoteMessage.setId(returnValue.getId());
//
//        while (true) {
//            try {
//                if (remote.getData().getAppClassList().contains(appClassRemoteMessage.clone().build())) {
//                    break;
//                }
//            } catch (NotAvailableException ex) {
//                logger.debug("Not ready yet");
//            }
//            Thread.yield();
//        }
//        assertTrue(remote.containsAppClass(appClassRemoteMessage.clone().build()));
//    }
//
//    /**
//     * Test of registering a AppConfig per remote.
//     */
//    @Test(timeout = 3000)
//    public void testRegisterAppConfigPerRemote() throws Exception {
//        System.out.println("registerAppConfigPerRemote");
//        remote.registerAppConfig(appConfigRemoteMessage.clone().build());
//        while (true) {
//            if (remote.containsAppConfig(appConfigRemoteMessage.clone().build())) {
//                break;
//            }
//            Thread.yield();
//        }
//        assertTrue(remote.containsAppConfig(appConfigRemoteMessage.clone().build()));
//    }
}
