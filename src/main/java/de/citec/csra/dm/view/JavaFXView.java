/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view;

import de.citec.csra.dm.view.struct.node.DeviceClassList;
import static de.citec.csra.dm.DeviceManager.DEFAULT_SCOPE;
import de.citec.csra.dm.remote.DeviceRegistryRemote;
import de.citec.csra.dm.view.struct.leave.Leave;
import de.citec.csra.dm.view.struct.node.Node;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.rsb.jp.JPScope;
import java.util.ArrayList;
import java.util.Collection;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.binding.BindingConfigType;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.registry.DeviceRegistryType;
import rst.homeautomation.unit.UnitTypeHolderType;

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
    private TreeTableColumn<Node, String> descriptorColumn;
    private TreeTableColumn<Node, String> descriptorColumn2;
    private TreeTableColumn<Node, String> valueColumn;
    private TreeTableColumn<Node, String> valueColumn2;
    private BorderPane borderPane;
    private HBox hBox;
    private Button add, edit, remove;

    public JavaFXView() {
        this.remote = new DeviceRegistryRemote();
    }

    @Override
    public void init() throws Exception {
        super.init();
        remote.init(JPService.getProperty(JPScope.class).getValue());

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
        descriptorColumn.setCellValueFactory(new Callback<CellDataFeatures<Node, String>, ObservableValue<String>>() {

            @Override
            public ObservableValue<String> call(CellDataFeatures<Node, String> param) {
                return new ReadOnlyStringWrapper(param.getValue().getValue().getDescriptor());
            }
        });
        valueColumn = new TreeTableColumn<>("Value");
        valueColumn.setPrefWidth(1024 - 400);
        valueColumn.setCellValueFactory(new Callback<CellDataFeatures<Node, String>, ObservableValue<String>>() {

            @Override
            public ObservableValue<String> call(CellDataFeatures<Node, String> param) {
                if (param.getValue().getValue() instanceof Leave) {
                    return new ReadOnlyStringWrapper(((Leave) param.getValue().getValue()).getValue().toString());
                } else {
                    return new ReadOnlyStringWrapper("");
                }
            }
        });

//        valueColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        valueColumn.setOnEditCommit(new EventHandler<TreeTableColumn.CellEditEvent<Node, String>>() {

            @Override
            public void handle(TreeTableColumn.CellEditEvent<Node, String> event) {
                if (event.getRowValue().getValue() instanceof Leave) {
                    if (!event.getRowValue().getValue().getDescriptor().equals("ID")) {
                    }
                    ((Leave) event.getRowValue().getValue()).setValue(event.getNewValue());
                }
            }
        });
        valueColumn2 = new TreeTableColumn<>("Value");
        valueColumn2.setPrefWidth(1024 - 400);
        valueColumn2.setCellValueFactory(valueColumn.getCellValueFactory());
        descriptorColumn2 = new TreeTableColumn<>("Description");
        descriptorColumn2.setPrefWidth(400);
        descriptorColumn2.setCellValueFactory(descriptorColumn.getCellValueFactory());

        deviceClassTreeTableView.getColumns().addAll(descriptorColumn, valueColumn);
        deviceClassTreeTableView.setRoot(new DeviceClassList(testDeviceClass()));
        deviceClassTreeTableView.setEditable(true);
        deviceConfigTreeTableView.getColumns().addAll(descriptorColumn2, valueColumn2);

        tabDeviceRegistryPane = new TabPane();
        tabDeviceRegistryPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabDeviceClass = new Tab("DeviceClass");
        tabDeviceConfig = new Tab("DeviceConfig");
        tabDeviceClass.setContent(deviceClassTreeTableView);
        tabDeviceConfig.setContent(deviceConfigTreeTableView);
        tabDeviceRegistryPane.getTabs().addAll(tabDeviceClass, tabDeviceConfig);
        tabDeviceRegistry.setContent(tabDeviceRegistryPane);

        hBox = new HBox();
        edit = new Button("Edit");
        edit.setPrefWidth(100);
        add = new Button("Add");
        add.setPrefWidth(100);
        remove = new Button("Remove");
        remove.setPrefWidth(100);
        hBox.setAlignment(Pos.BASELINE_RIGHT);
        hBox.getChildren().addAll(add, edit, remove);
        borderPane = new BorderPane();
        borderPane.setBottom(hBox);
        borderPane.setCenter(registryTabPane);
    }

    public void start(Stage primaryStage) throws Exception {

//        remote.activate();
//        remote.addObserver((Observable<DeviceRegistryType.DeviceRegistry> source, DeviceRegistryType.DeviceRegistry data) -> {
//            updateDynamicNodes();
//        });
        Scene scene = new Scene(borderPane, 1024, 576);
        primaryStage.setTitle("Registry Editor");
//        primaryStage.setFullScreen(true);
//        primaryStage.setFullScreenExitKeyCombination(KeyCombination.ALT_ANY);
        primaryStage.setScene(scene);
        primaryStage.show();

//        updateDynamicNodes();
//        remote.registerDeviceClass(getTestData());
    }

    public DeviceClass getTestData() {
        return DeviceClassType.DeviceClass.newBuilder().setLabel("MyTestData5").build();
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
            deviceClassTreeTableView.setRoot(new DeviceClassList(data.getDeviceClassesList()));
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
        JPService.registerProperty(JPScope.class, DEFAULT_SCOPE);
        launch(args);
    }

    private Collection<DeviceClassType.DeviceClass> testDeviceClass() {
        Collection<DeviceClassType.DeviceClass> testCollection = new ArrayList();

        UnitTypeHolderType.UnitTypeHolder testUnitType1 = UnitTypeHolderType.UnitTypeHolder.newBuilder().setUnitType(UnitTypeHolderType.UnitTypeHolder.UnitType.LIGHT).build();
        UnitTypeHolderType.UnitTypeHolder testUnitType2 = UnitTypeHolderType.UnitTypeHolder.newBuilder().setUnitType(UnitTypeHolderType.UnitTypeHolder.UnitType.MOTION_SENSOR).build();
        BindingConfigType.BindingConfig testBindingConfig = BindingConfigType.BindingConfig.newBuilder().setBindingType(BindingConfigType.BindingConfig.BindingType.OPENHAB).build();
        DeviceClassType.DeviceClass testDeviceClass = DeviceClassType.DeviceClass.newBuilder().setLabel("Test DeviceClass").setId("Test DeviceClassID").setProductNumber("1234-5678-9101").setDescription("This is a test DeviceClass").setBindingConfig(testBindingConfig).addUnits(testUnitType2).addUnits(testUnitType1).build();

        UnitTypeHolderType.UnitTypeHolder testUnitType3 = UnitTypeHolderType.UnitTypeHolder.newBuilder().setUnitType(UnitTypeHolderType.UnitTypeHolder.UnitType.BATTERY).build();
        UnitTypeHolderType.UnitTypeHolder testUnitType4 = UnitTypeHolderType.UnitTypeHolder.newBuilder().setUnitType(UnitTypeHolderType.UnitTypeHolder.UnitType.TEMPERATURE_SENSOR).build();
        DeviceClassType.DeviceClass testDeviceClass2 = DeviceClassType.DeviceClass.newBuilder().setLabel("Test DeviceClass2").setId("Test DeviceClassID2").setProductNumber("1234-5878-9101").setDescription("This is a test DeviceClass").setBindingConfig(testBindingConfig).addUnits(testUnitType3).addUnits(testUnitType4).build();

        testCollection.add(testDeviceClass);
        testCollection.add(testDeviceClass2);
        return testCollection;
    }
}
