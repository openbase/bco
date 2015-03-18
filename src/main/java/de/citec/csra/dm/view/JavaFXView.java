/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view;

import de.citec.csra.dm.view.cellfactory.DescriptionCell;
import de.citec.csra.dm.view.cellfactory.ValueCell;
import de.citec.csra.dm.view.struct.node.DeviceClassList;
import static de.citec.csra.dm.DeviceManager.DEFAULT_SCOPE;
import de.citec.csra.dm.remote.DeviceRegistryRemote;
import de.citec.csra.dm.view.struct.leaf.Leaf;
import de.citec.csra.dm.view.struct.node.DeviceClassContainer;
import de.citec.csra.dm.view.struct.node.DeviceConfigContainer;
import de.citec.csra.dm.view.struct.node.DeviceConfigList;
import de.citec.csra.dm.view.struct.node.Node;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.rsb.jp.JPScope;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.registry.DeviceRegistryType;

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
        descriptorColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("this"));
        descriptorColumn.setCellFactory(new Callback<TreeTableColumn<Node, Node>, TreeTableCell<Node, Node>>() {

            @Override
            public TreeTableCell<Node, Node> call(TreeTableColumn<Node, Node> param) {
                return new DescriptionCell(remote);
            }
        });

        MenuItem addDeviceClass = new MenuItem("Add");
        addDeviceClass.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                if( event.getSource().equals(addDeviceClass)) {
                    deviceClassTreeTableView.getRoot().getChildren().add(new DeviceClassContainer(DeviceClassType.DeviceClass.getDefaultInstance().toBuilder()));
                }
            }
        });

        ContextMenu deviceClassTreeTableViewContextMenu = new ContextMenu(addDeviceClass);
                deviceClassTreeTableView.setContextMenu(deviceClassTreeTableViewContextMenu);

        valueColumn = new TreeTableColumn<>("Value");
        valueColumn.setPrefWidth(1024 - 400);
        valueColumn.setSortable(false);
        valueColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("this"));
        valueColumn.setCellFactory(new Callback<TreeTableColumn<Node, Node>, TreeTableCell<Node, Node>>() {

            @Override
            public TreeTableCell<Node, Node> call(TreeTableColumn<Node, Node> param) {

                return new ValueCell(remote);
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

        MenuItem addDeviceConfig = new MenuItem("Add");
        addDeviceConfig.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                if( event.getSource().equals(addDeviceConfig)) {
                    deviceConfigTreeTableView.getRoot().getChildren().add(new DeviceConfigContainer(DeviceConfigType.DeviceConfig.getDefaultInstance().toBuilder()));
                }
            }
        });

        ContextMenu deviceConfiglassTreeTableViewContextMenu = new ContextMenu(addDeviceConfig);
        deviceConfigTreeTableView.setContextMenu(deviceConfiglassTreeTableViewContextMenu);
                
        deviceClassTreeTableView.getColumns().addAll(descriptorColumn, valueColumn);
//        deviceClassTreeTableView.getColumns().addAll(new DescriptorColumn(remote), new ValueColumn(remote));
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
        deviceConfigTreeTableView.setShowRoot(false);

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
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                if (!remote.isConnected()) {
                    tabDeviceRegistry.setContent(progressDeviceRegistryIndicator);
                    return;
                }
                try {
                    DeviceRegistryType.DeviceRegistry data = remote.getData();
                    deviceClassTreeTableView.setRoot(new DeviceClassList(data.toBuilder()));
                    deviceConfigTreeTableView.setRoot(new DeviceConfigList(data.toBuilder()));
                    tabDeviceRegistry.setContent(tabDeviceRegistryPane);

                } catch (NotAvailableException ex) {
                    logger.error("Device classes not available!", ex);
                    tabDeviceRegistry.setContent(new Label("Error: " + ex.getMessage()));
                }
            }
        });

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
}
