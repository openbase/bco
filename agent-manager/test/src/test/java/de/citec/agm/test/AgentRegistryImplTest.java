/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.agm.test;

/**
 *
 * @author mpohling
 */
public class AgentRegistryImplTest {

//    private static final Logger logger = LoggerFactory.getLogger(AgentRegistryImplTest.class);
//
//    public static final String LOCATION_LABEL = "paradise";
//    public static LocationConfig LOCATION;
//
//    private static AgentRegistryService agentRegistry;
//    private static AgentClass.Builder agentClass;
//    private static AgentConfig.Builder agentConfig;
//
//    private static LocationRegistryService locationRegistry;
//
//    private static AgentClass.Builder agentClassRemoteMessage;
//    private static AgentClass.Builder returnValue;
//    private static AgentConfig.Builder agentConfigRemoteMessage;
//    private static AgentRegistryRemote remote;
//
//    public AgentRegistryImplTest() {
//    }
//
//    @BeforeClass
//    public static void setUpClass() throws InstantiationException, InitializationException, IOException, InvalidStateException, JPServiceException, InterruptedException, CouldNotPerformException {
//        JPService.registerProperty(JPInitializeDB.class, true);
//        JPService.registerProperty(JPAgentRegistryScope.class, new Scope("/test/agentmanager/registry/"));
//        JPService.registerProperty(JPLocationRegistryScope.class, new Scope("/test/locationmanager/registry/"));
//        JPService.registerProperty(JPAgentDatabaseDirectory.class, new File("/tmp/" + System.getProperty("user.name") + "/db/"));
//        JPService.registerProperty(JPAgentConfigDatabaseDirectory.class, new File("agent-config"));
//        JPService.registerProperty(JPAgentClassDatabaseDirectory.class, new File("agent-classes"));
//        JPService.setupJUnitTestMode();
//
//        agentRegistry = new AgentRegistryService();
//        locationRegistry = new LocationRegistryService();
//
//        agentRegistry.init();
//        locationRegistry.init();
//
//        Thread agentRegistryThread = new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                try {
//                    agentRegistry.activate();
//                } catch (CouldNotPerformException | InterruptedException ex) {
//                    ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
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
//                    ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
//                }
//            }
//        });
//
//        agentRegistryThread.start();
//        locationRegistryThread.start();
//
//        agentRegistryThread.join();
//        locationRegistryThread.join();
//
//        agentClass = AgentClass.getDefaultInstance().newBuilderForType();
//        agentClass.setLabel("TestAgentClassLabel");
//        agentClass.setCompany("MyCom");
//        agentClass.setProductNumber("TDCL-001");
//        agentConfig = AgentConfig.getDefaultInstance().newBuilderForType();
//        agentConfig.setLabel("TestAgentConfigLabel");
//        agentConfig.setSerialNumber("0001-0004-2245");
//        agentConfig.setAgentClass(agentClass.clone().setId("TestAgentClassLabel"));
//
//        agentClassRemoteMessage = AgentClass.getDefaultInstance().newBuilderForType();
//        agentClassRemoteMessage.setLabel("RemoteTestAgentClass").setProductNumber("ABR-132").setCompany("DreamCom");
//        agentConfigRemoteMessage = AgentConfig.getDefaultInstance().newBuilderForType();
//        agentConfigRemoteMessage.setLabel("RemoteTestAgentConfig").setSerialNumber("1123-5813-2134");
//        agentConfigRemoteMessage.setAgentClass(agentClassRemoteMessage.clone().setId("RemoteTestAgentClass"));
//
//        remote = new AgentRegistryRemote();
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
//        if (agentRegistry != null) {
//            agentRegistry.shutdown();
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
//     * Test of registerAgentClass method, of class AgentRegistryImpl.
//     */
//    @Test
//    public void testRegisterAgentClass() throws Exception {
//        System.out.println("registerAgentClass");
//        agentRegistry.registerAgentClass(agentClass.clone().build());
//        assertTrue(agentRegistry.containsAgentClass(agentClass.clone().build()));
////		assertEquals(true, registry.getData().getAgentClassesBuilderList().contains(agentClass));
//    }
//
//    /**
//     * Test of registerAgentConfig method, of class AgentRegistryImpl.
//     */
//    @Test
//    public void testRegisterAgentConfig() throws Exception {
//        System.out.println("registerAgentConfig");
//        agentRegistry.registerAgentConfig(agentConfig.clone().build());
//        assertTrue(agentRegistry.containsAgentConfig(agentConfig.clone().build()));
//    }
//
//    /**
//     * Test of registerAgentConfigWithUnits method, of class
//     * AgentRegistryImpl.
//     *
//     * Test if the scope and the id of a agent configuration and its units is
//     * set when registered.
//     */
//    @Test
//    public void testRegisterAgentConfigWithUnits() throws Exception {
//        String productNumber = "ABCD-4321";
//        String serialNumber = "1234-WXYZ";
//        String company = "Fibaro";
//
//        String agentId = company + "_" + productNumber + "_" + serialNumber;
//        String agentLabel = "TestSensor";
//        String agentScope = "/" + LOCATION_LABEL + "/" + agentLabel.toLowerCase() + "/";
//
//        String unitLabel = "Battery";
//        String unitScope = "/" + LOCATION_LABEL + "/" + UnitTemplate.UnitType.BATTERY.name().toLowerCase() + "/" + unitLabel.toLowerCase() + "/";
//        String unitID = unitScope;
//
//        ArrayList<UnitConfig> units = new ArrayList<>();
//        AgentClass motionSensorClass = agentRegistry.registerAgentClass(getAgentClass("F_MotionSensor", productNumber, company));
//        units.add(getUnitConfig(UnitTemplate.UnitType.BATTERY, unitLabel));
//        AgentConfig motionSensorConfig = getAgentConfig(agentLabel, serialNumber, motionSensorClass, units);
//
//        motionSensorConfig = agentRegistry.registerAgentConfig(motionSensorConfig);
//
//        assertEquals("Agent id is not set properly", agentId, motionSensorConfig.getId());
//        assertEquals("Agent scope is not set properly", agentScope, ScopeGenerator.generateStringRep(motionSensorConfig.getScope()));
//
//        assertEquals("Unit id is not set properly", unitID, motionSensorConfig.getUnitConfig(0).getId());
//        assertEquals("Unit scope is not set properly", unitScope, ScopeGenerator.generateStringRep(motionSensorConfig.getUnitConfig(0).getScope()));
//
//        assertEquals("Agent id is not set in unit", motionSensorConfig.getId(), motionSensorConfig.getUnitConfig(0).getAgentId());
//    }
//
//    /**
//     * Test of testRegiseredAgentConfigWithoutLabel method, of class
//     * AgentRegistryImpl.
//     */
//    @Test
//    public void testRegisteredAgentConfigWithoutLabel() throws Exception {
//        String productNumber = "KNHD-4321";
//        String serialNumber = "112358";
//        String company = "Company";
//
//        String agentId = company + "_" + productNumber + "_" + serialNumber;
//
//        AgentClass clazz = agentRegistry.registerAgentClass(getAgentClass("WithoutLabel", productNumber, company));
//        AgentConfig agentWithoutLabel = getAgentConfig("", serialNumber, clazz, new ArrayList<UnitConfig>());
//        agentWithoutLabel = agentRegistry.registerAgentConfig(agentWithoutLabel);
//
//        assertEquals("The agent label is not set as the id if it is empty!", agentId, agentWithoutLabel.getLabel());
//    }
//
//    /**
//     * Test of testRegisterTwoAgentsWithSameLabel method, of class
//     * AgentRegistryImpl.
//     */
//    @Test
//    public void testRegisterTwoAgentsWithSameLabel() throws Exception {
//        String serialNumber1 = "FIRST_DEV";
//        String serialNumber2 = "BAD_DEV";
//        String agentLabel = "SameLabelSameLocation";
//
//        AgentClass clazz = agentRegistry.registerAgentClass(getAgentClass("WithoutLabel", "xyz", "HuxGMBH"));
//        AgentConfig agentWithLabel1 = getAgentConfig(agentLabel, serialNumber1, clazz, new ArrayList<UnitConfig>());
//        AgentConfig agentWithLabel2 = getAgentConfig(agentLabel, serialNumber2, clazz, new ArrayList<UnitConfig>());
//
//        agentRegistry.registerAgentConfig(agentWithLabel1);
//        try {
//            agentRegistry.registerAgentConfig(agentWithLabel2);
//            fail("There was no exception thrown even though two agents with the same label [" + agentLabel + "] where registered in the same location [" + LOCATION_LABEL + "]");
//        } catch (Exception ex) {
//            assertTrue(true);
//        }
//    }
//
//    /**
//     * Test if the unit id of is set in the agent service.
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
//        AgentClass clazz = agentRegistry.registerAgentClass(getAgentClass("ServiceUnitIdTest", "8383838", "ServiceGMBH"));
//
//        AgentConfig agentConfig = agentRegistry.registerAgentConfig(getAgentConfig("ServiceTest", "123456", clazz, units));
//
////        assertTrue("Unit id is not set.", !agentConfig.getUnitConfig(0).getId().equals(""));
////        assertTrue("Unit id in service config is not set.", !agentConfig.getUnitConfig(0).getServiceConfig(0).getUnitId().equals(""));
////        assertTrue("Unit id in service config does not match id in unit config.", agentConfig.getUnitConfig(0).getServiceConfig(0).getUnitId().equals(agentConfig.getUnitConfig(0).getId()));
//        String itemId = OpenhabServiceConfigItemIdConsistenyHandler.generateItemName(agentConfig, unitConfig, serviceConfig, LOCATION);
//
////        assertTrue("OpenHAB item id is not set.", itemId.equals(agentConfig.getUnitConfig(0).getServiceConfig(0).getBindingServiceConfig().getOpenhabBindingServiceConfig().getItemId()));
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
//    private AgentConfig getAgentConfig(String label, String serialNumber, AgentClass clazz, ArrayList<UnitConfig> units) {
//        return AgentConfig.newBuilder().setPlacementConfig(getDefaultPlacement()).setLabel(label).setSerialNumber(serialNumber).setAgentClass(clazz).addAllUnitConfig(units).build();
//    }
//
//    private AgentClass getAgentClass(String label, String productNumber, String company) {
//        return AgentClass.newBuilder().setLabel(label).setProductNumber(productNumber).setCompany(company).build();
//    }
//
//    /**
//     * Test of registering a AgentClass per remote.
//     */
//    @Test(timeout = 5000)
//    public void testRegisterAgentClassPerRemote() throws Exception {
//        System.out.println("registerAgentClassPerRemote");
//
//        remote.addObserver(new Observer<AgentRegistryType.AgentRegistry>() {
//
//            @Override
//            public void update(Observable<AgentRegistryType.AgentRegistry> source, AgentRegistryType.AgentRegistry data) throws Exception {
//                if (data != null) {
//                    logger.info("Got empty data!");
//                } else {
//                    logger.info("Got data update: " + data);
//                }
//            }
//        });
//
//        returnValue = remote.registerAgentClass(agentClassRemoteMessage.clone().build()).toBuilder();
//        logger.info("Returned agent class id [" + returnValue.getId() + "]");
//        agentClassRemoteMessage.setId(returnValue.getId());
//
//        while (true) {
//            try {
//                if (remote.getData().getAgentClassList().contains(agentClassRemoteMessage.clone().build())) {
//                    break;
//                }
//            } catch (NotAvailableException ex) {
//                logger.debug("Not ready yet");
//            }
//            Thread.yield();
//        }
//        assertTrue(remote.containsAgentClass(agentClassRemoteMessage.clone().build()));
//    }
//
//    /**
//     * Test of registering a AgentConfig per remote.
//     */
//    @Test(timeout = 3000)
//    public void testRegisterAgentConfigPerRemote() throws Exception {
//        System.out.println("registerAgentConfigPerRemote");
//        remote.registerAgentConfig(agentConfigRemoteMessage.clone().build());
//        while (true) {
//            if (remote.containsAgentConfig(agentConfigRemoteMessage.clone().build())) {
//                break;
//            }
//            Thread.yield();
//        }
//        assertTrue(remote.containsAgentConfig(agentConfigRemoteMessage.clone().build()));
//    }
}
