/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

/**
 *
 * @author thuxohl
 */
public class JavaFXView extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
		TabPane registryTabPane = new TabPane();

        Tab tabDeviceRegistry = new Tab("DeviceRegistry");
        Tab tabLocationRegistry = new Tab("LocationRegistry");
		registryTabPane.getTabs().addAll(tabDeviceRegistry, tabLocationRegistry);

        TabPane tabPane = new TabPane();
        Tab tabDeviceClass = new Tab("DeviceClass");
        Tab tabDeviceConfig = new Tab("DeviceConfig");
        tabPane.getTabs().addAll(tabDeviceClass,tabDeviceConfig);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabDeviceRegistry.getTabPane().a
        Scene scene = new Scene(registryTabPane,700,700);
		setTitle("");
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}
}
