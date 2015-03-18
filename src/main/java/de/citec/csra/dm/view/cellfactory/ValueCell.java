/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.cellfactory;

import de.citec.csra.dm.remote.DeviceRegistryRemote;
import de.citec.csra.dm.view.struct.leaf.Leaf;
import de.citec.csra.dm.view.struct.node.DeviceClassContainer;
import de.citec.csra.dm.view.struct.node.DeviceConfigContainer;
import de.citec.csra.dm.view.struct.node.Node;
import de.citec.csra.dm.view.struct.node.SendableNode;
import de.citec.jul.exception.CouldNotPerformException;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceConfigType;

/**
 *
 * @author thuxohl
 */
public class ValueCell extends RowCell {

    private final TextField textField;
    private final ComboBox comboBox;
    private final TextField decimalTextField;
    private final TextField integerTextField;
    private final Button apply;
    private Leaf leaf;

    public ValueCell(DeviceRegistryRemote remote) {
        super(remote);
        apply = new Button("Apply Changes");
        apply.setVisible(true);
        apply.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {

                SendableNode sendNode = (SendableNode) getItem();
//                            sendNode.getApplyButton().setVisible(false);
                if (sendNode instanceof DeviceClassContainer) {
                    DeviceClassType.DeviceClass type = ((DeviceClassType.DeviceClass.Builder) sendNode.getBuilder()).build();
                    try {
                        if (remote.containsDeviceClass(type)) {
                            remote.registerDeviceClass(type);
                        } else {
                            remote.updateDeviceClass(type);
                        }
                    } catch (CouldNotPerformException ex) {
                        System.out.println("Could not register or update deviceClass [" + type + "]");
                    }
                } else if (sendNode instanceof DeviceConfigContainer) {
                    DeviceConfigType.DeviceConfig type = ((DeviceConfigType.DeviceConfig.Builder) sendNode.getBuilder()).build();
                    try {
                        if (remote.containsDeviceConfig(type)) {
                            remote.registerDeviceConfig(type);
                        } else {
                            remote.updateDeviceConfig(type);
                        }
                    } catch (CouldNotPerformException ex) {
                        System.out.println("Could not register or update deviceConfig [" + type + "]");
                    }
                }
            }
        });

        textField = new TextField();
        textField.setOnKeyReleased(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ESCAPE)) {
                    cancelEdit();
                } else if (event.getCode().equals(KeyCode.ENTER)) {
                    leaf.setValue(textField.getText());
                    commitEdit(leaf);
                }
            }
        });

        comboBox = new ComboBox();
        comboBox.setOnAction(new EventHandler() {

            @Override
            public void handle(Event event) {
                if (comboBox.getSelectionModel().getSelectedItem() != null && !leaf.getValue().equals(comboBox.getSelectionModel().getSelectedItem())) {
                    leaf.setValue(comboBox.getSelectionModel().getSelectedItem());
                    commitEdit(leaf);
                }
            }
        });

        decimalTextField = new TextField() {
            @Override
            public void replaceText(int start, int end, String text) {
                if (text.matches("[0-9.]") || text.equals("")) {
                    super.replaceText(start, end, text);
                }
            }

            @Override
            public void replaceSelection(String text) {
                if (text.matches("[0-9.]") || text.equals("")) {
                    super.replaceSelection(text);
                }
            }
        };
        decimalTextField.setOnKeyReleased(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ESCAPE)) {
                    cancelEdit();
                } else if (event.getCode().equals(KeyCode.ENTER)) {
                    float parseFloat = Float.parseFloat(decimalTextField.getText());
                    leaf.setValue(parseFloat);
                    commitEdit(leaf);
                }
            }
        });

        integerTextField = new TextField() {
            @Override
            public void replaceText(int start, int end, String text) {
                if (text.matches("[0-9]") || text.equals("")) {
                    super.replaceText(start, end, text);
                }
            }

            @Override
            public void replaceSelection(String text) {
                if (text.matches("[0-9]") || text.equals("")) {
                    super.replaceSelection(text);
                }
            }
        };
        integerTextField.setOnKeyReleased(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ESCAPE)) {
                    cancelEdit();
                } else if (event.getCode().equals(KeyCode.ENTER)) {
                    long parseLong = Long.parseLong(integerTextField.getText());
                    leaf.setValue(parseLong);
                    commitEdit(leaf);
                }
            }
        });
    }

    @Override
    public void startEdit() {
        super.startEdit();

        if (getItem() instanceof Leaf) {
            leaf = ((Leaf) getItem());

            if (leaf.getValue() instanceof String) {
                textField.setText((String) leaf.getValue());
                graphicProperty().setValue(textField);
            } else if (leaf.getValue() instanceof Enum) {
                comboBox.setItems(FXCollections.observableArrayList(leaf.getValue().getClass().getEnumConstants()));
                comboBox.setValue(leaf.getValue());
                graphicProperty().setValue(comboBox);
            } else if (leaf.getValue() instanceof Float) {
                decimalTextField.setText(((Float) leaf.getValue()).toString());
                graphicProperty().setValue(decimalTextField);
            } else if (leaf.getValue() instanceof Long) {
                integerTextField.setText(((Long) leaf.getValue()).toString());
                setGraphic(integerTextField);
            }
        }
    }

    @Override
    public void commitEdit(Node newValue) {
        super.commitEdit(newValue);
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        graphicProperty().setValue(null);
    }

    @Override
    public void updateItem(Node item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            graphicProperty().setValue(null);
            textProperty().setValue("");
            setContextMenu(null);
        } else if (item instanceof Leaf) {
            graphicProperty().setValue(null);
            textProperty().setValue(((Leaf) item).getValue().toString());
            setContextMenu(null);
        } else if (item instanceof DeviceClassContainer) {
            setGraphic(apply);
        }
    }

}
