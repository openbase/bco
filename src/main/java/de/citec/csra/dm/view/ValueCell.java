/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view;

import de.citec.csra.dm.view.struct.leaf.Leaf;
import de.citec.csra.dm.view.struct.node.Node;
import de.citec.csra.dm.view.struct.node.SendableNode;
import de.citec.csra.dm.view.struct.node.VariableNode;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 *
 * @author thuxohl
 */
public class ValueCell extends TreeTableCell<Node, Node> {

    private final TextField textField;
    private final ComboBox comboBox;
    private final TextField numberTextField;
    private final Button apply;
    private Leaf leaf;

    public ValueCell() {        
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

        numberTextField = new TextField() {
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
        numberTextField.setOnKeyReleased(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ESCAPE)) {
                    cancelEdit();
                } else if (event.getCode().equals(KeyCode.ENTER)) {
                    float parseFloat = Float.parseFloat(numberTextField.getText());
                    leaf.setValue(parseFloat);
                    commitEdit(leaf);
                }
            }
        });
        
        apply = new Button("Apply Changes");
        apply.setVisible(false);
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
                numberTextField.setText(((Float) leaf.getValue()).toString());
                graphicProperty().setValue(numberTextField);
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
        } else if (item instanceof Leaf) {
            graphicProperty().setValue(null);
            textProperty().setValue(((Leaf) item).getValue().toString());
        } else if (item instanceof VariableNode && !(item instanceof Leaf)) {
            setContextMenu(((VariableNode) item).getContextMenu());
            if( item instanceof SendableNode) {
                SendableNode sendable = (SendableNode) item;
                setGraphic(sendable.getApplyButton());
            }
        }else {
            graphicProperty().setValue(null);
            textProperty().setValue("");
        }
    }
}
