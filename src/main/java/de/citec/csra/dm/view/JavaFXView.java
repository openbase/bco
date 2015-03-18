/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view;

import de.citec.csra.dm.view.struct.node.DeviceClassList;
import static de.citec.csra.dm.DeviceManager.DEFAULT_SCOPE;
import de.citec.csra.dm.remote.DeviceRegistryRemote;
import de.citec.csra.dm.view.struct.leaf.Leaf;
import de.citec.csra.dm.view.struct.node.DeviceConfigContainer;
import de.citec.csra.dm.view.struct.node.Node;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.pattern.Observable;
import java.util.ArrayList;
import java.util.Collection;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.binding.BindingConfigType;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.registry.DeviceRegistryType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitTypeHolderType;
import rst.math.Vec3DFloatType;
import rst.spatial.PlacementConfigType;

/**
 *
 * @author thuxohl
 */
public class JavaFXView extends Application {

    private static final Logger logger = LoggerFactory.getLogger(JavaFXView.class);

    public static final String APP_NAME = "RegistryView";

    private final DeviceRegistryRemote remote;
    private TabPane registryTabPane, tabDeviceRegistryPane;
    private Tab tabDeviceRegistry, tabLocationRegistry, tabDeviceClass, tabDeviceConfig;
    private ProgressIndicator progressDeviceRegistryIndicator;
    private ProgressIndicator progressLocationRegistryIndicator;
    private TreeTableView<Node> deviceClassTreeTableView;
    private TreeTableView<Node> deviceConfigTreeTableView;
    private TreeTableColumn<Node, Node> descriptorColumn;
    private TreeTableColumn<Node, Node> valueColumn;
    private TreeTableColumn<Node, Node> descriptorColumn2;
    private TreeTableColumn<Node, Node> valueColumn2;
    private ValueCell valueColumnCell;

    public JavaFXView() {
        this.remote = new DeviceRegistryRemote();
    }

    @Override
    public void init() throws Exception {
        super.init();
        remote.init(JPService.getProperty(JPDeviceRegistryScope.class).getValue());

        registryTabPane = new TabPane();
        registryTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabDeviceRegistry = new Tab("DeviceRegistry");
        tabLocationRegistry = new Tab("LocationRegistry");
        registryTabPane.getTabs().addAll(tabDeviceRegistry, tabLocationRegistry);

        progressDeviceRegistryIndicator = new ProgressIndicator();
        progressLocationRegistryIndicator = new ProgressIndicator();
        tabLocationRegistry.setContent(progressLocationRegistryIndicator);

        deviceClassTreeTableView = new TreeTableView<>();
        deviceConfigTreeTableView = new TreeTableView<>();

        descriptorColumn = new TreeTableColumn<>("Description");
        descriptorColumn.setPrefWidth(400);
        descriptorColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("descriptor"));

        valueColumn = new TreeTableColumn<>("Value");
        valueColumn.setPrefWidth(1024 - 400);
        valueColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("this"));
//        valueColumn.setCellValueFactory(new Callback<CellDataFeatures<Node, Node>, ObservableValue<Node>>() {
//
//            @Override
//            public ObservableValue<Node> call(CellDataFeatures<Node, Node> param) {
//                if (param.getValue().getValue() instanceof Leaf) {
//                    return (ObservableValue<Node>) new Label(((Leaf) param.getValue().getValue()).getValue().toString());
//                } else {
//                    return (ObservableValue<Node>) new Label("");
//                }
//            }
//        });
//        valueColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        valueColumnCell= new ValueCell();
        valueColumn.setCellFactory(new Callback<TreeTableColumn<Node, Node>, TreeTableCell<Node, Node>>() {

            @Override
            public TreeTableCell<Node, Node> call(TreeTableColumn<Node, Node> param) {

                return new ValueCell();
            }
        });
        valueColumn.setOnEditCommit(new EventHandler<TreeTableColumn.CellEditEvent<Node, Node>>() {

            @Override
            public void handle(TreeTableColumn.CellEditEvent<Node, Node> event) {
                if (event.getRowValue().getValue() instanceof Leaf) {
                    if (!event.getRowValue().getValue().getDescriptor().equals("ID")) {
                    }
                    ((Leaf) event.getRowValue().getValue()).setValue(((Leaf) event.getNewValue()).getValue());
                }
            }
        });

        deviceClassTreeTableView.getColumns().addAll(descriptorColumn, valueColumn);
//        deviceClassTreeTableView.setRoot(new DeviceClassList(testDeviceClass()));
        deviceClassTreeTableView.setEditable(true);
        deviceClassTreeTableView.setShowRoot(false);

        descriptorColumn2 = new TreeTableColumn<>("Description");
        descriptorColumn2.setPrefWidth(400);
        descriptorColumn2.setCellValueFactory(new TreeItemPropertyValueFactory<>("descriptor"));

        valueColumn2 = new TreeTableColumn<>("Value");
        valueColumn2.setPrefWidth(1024 - 400);
        valueColumn2.setCellValueFactory(new TreeItemPropertyValueFactory<>("this"));
        valueColumn2.setCellFactory(valueColumn.getCellFactory());
        valueColumn2.setOnEditCommit(valueColumn.getOnEditCommit());
        deviceConfigTreeTableView.getColumns().addAll(descriptorColumn2, valueColumn2);
        deviceConfigTreeTableView.setEditable(true);
        deviceConfigTreeTableView.setRoot(new DeviceConfigContainer(testDeviceConfig().toBuilder()));

        tabDeviceRegistryPane = new TabPane();
        tabDeviceRegistryPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabDeviceClass = new Tab("DeviceClass");
        tabDeviceConfig = new Tab("DeviceConfig");
        tabDeviceClass.setContent(deviceClassTreeTableView);
        tabDeviceConfig.setContent(deviceConfigTreeTableView);
        tabDeviceRegistryPane.getTabs().addAll(tabDeviceClass, tabDeviceConfig);
        tabDeviceRegistry.setContent(tabDeviceRegistryPane);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        remote.activate();
        remote.addObserver((Observable<DeviceRegistryType.DeviceRegistry> source, DeviceRegistryType.DeviceRegistry data) -> {
            updateDynamicNodes();
        });
        remote.requestStatus();
        Scene scene = new Scene(registryTabPane, 1024, 576);
        primaryStage.setTitle("Registry Editor");
//        primaryStage.setFullScreen(true);
//        primaryStage.setFullScreenExitKeyCombination(KeyCombination.ALT_ANY);
        primaryStage.setScene(scene);
        primaryStage.show();

        updateDynamicNodes();
//        remote.registerDeviceClass(getTestData());
//        for(DeviceClass test : testDeviceClass()) {
//            remote.registerDeviceClass(test);
//        }
    }

    public DeviceClass getTestData() {
        return DeviceClassType.DeviceClass.newBuilder().setLabel("MyTestData4").build();
    }

    @Override
    public void stop() throws Exception {
        remote.shutdown();
        super.stop();
    }

    private void updateDynamicNodes() {
        if (!remote.isConnected()) {
            tabDeviceRegistry.setContent(progressDeviceRegistryIndicator);
            return;
        }
        try {
            DeviceRegistryType.DeviceRegistry data = remote.getData();
            deviceClassTreeTableView.setRoot(new DeviceClassList(data.toBuilder()));
            tabDeviceRegistry.setContent(tabDeviceRegistryPane);

        } catch (NotAvailableException ex) {
            logger.error("Device classes not available!", ex);
            tabDeviceRegistry.setContent(new Label("Error: " + ex.getMessage()));
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);
        JPService.registerProperty(JPDeviceRegistryScope.class, DEFAULT_SCOPE);
        launch(args);
    }

    public DeviceConfigType.DeviceConfig testDeviceConfig() {
        Vec3DFloatType.Vec3DFloat testPosition = Vec3DFloatType.Vec3DFloat.newBuilder().setX(12.5f).setY(55f).setZ(0.33f).build();
        PlacementConfigType.PlacementConfig testPlacement = PlacementConfigType.PlacementConfig.newBuilder().setPosition(testPosition).build();
        UnitConfigType.UnitConfig testUnitConfig = UnitConfigType.UnitConfig.newBuilder().setLabel("TestUnitConfig").setPlacement(testPlacement).build();

        DeviceConfigType.DeviceConfig testDeviceConfig = DeviceConfigType.DeviceConfig.newBuilder().setDescription("This is a TestConfig").setLabel("TestConfig").setSerialNumber("123-456-789").addUnitConfigs(testUnitConfig).build();
        return testDeviceConfig;
    }

    private Collection<DeviceClassType.DeviceClass> testDeviceClass() {
        Collection<DeviceClassType.DeviceClass> testCollection = new ArrayList();

        UnitTypeHolderType.UnitTypeHolder testUnitType1 = UnitTypeHolderType.UnitTypeHolder.newBuilder().setUnitType(UnitTypeHolderType.UnitTypeHolder.UnitType.LIGHT).build();
        UnitTypeHolderType.UnitTypeHolder testUnitType2 = UnitTypeHolderType.UnitTypeHolder.newBuilder().setUnitType(UnitTypeHolderType.UnitTypeHolder.UnitType.MOTION_SENSOR).build();
        BindingConfigType.BindingConfig testBindingConfig = BindingConfigType.BindingConfig.newBuilder().setBindingType(BindingConfigType.BindingConfig.BindingType.OPENHAB).build();
        DeviceClassType.DeviceClass testDeviceClass = DeviceClassType.DeviceClass.newBuilder().setLabel("Test DeviceClass").setProductNumber("1234-5678-9101").setDescription("This is a test DeviceClass").setBindingConfig(testBindingConfig).addUnits(testUnitType2).addUnits(testUnitType1).build();

        UnitTypeHolderType.UnitTypeHolder testUnitType3 = UnitTypeHolderType.UnitTypeHolder.newBuilder().setUnitType(UnitTypeHolderType.UnitTypeHolder.UnitType.BATTERY).build();
        UnitTypeHolderType.UnitTypeHolder testUnitType4 = UnitTypeHolderType.UnitTypeHolder.newBuilder().setUnitType(UnitTypeHolderType.UnitTypeHolder.UnitType.TEMPERATURE_SENSOR).build();
        DeviceClassType.DeviceClass testDeviceClass2 = DeviceClassType.DeviceClass.newBuilder().setLabel("Test DeviceClass2").setProductNumber("1234-5878-9101").setDescription("This is a test DeviceClass").setBindingConfig(testBindingConfig).addUnits(testUnitType3).addUnits(testUnitType4).build();

        testCollection.add(testDeviceClass);
        testCollection.add(testDeviceClass2);
        return testCollection;
    }
}
