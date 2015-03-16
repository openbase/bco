/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view;

import de.citec.csra.dm.view.struct.leaf.Leaf;
import de.citec.csra.dm.view.struct.node.Node;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import rst.homeautomation.binding.BindingConfigType;

/**
 *
 * @author thuxohl
 */
public class ValueCell extends TreeTableCell<Node, Node> {

    private final TextField textField;
    private final ComboBox comboBox;
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
                leaf.setValue(comboBox.getSelectionModel().getSelectedItem());
                commitEdit(leaf);
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
            } else if (leaf.getValue() instanceof BindingConfigType.BindingConfig.BindingType) {
                comboBox.setItems(FXCollections.observableArrayList(BindingConfigType.BindingConfig.BindingType.values()));
                comboBox.setValue(leaf.getValue());
                graphicProperty().setValue(comboBox);
            }
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        graphicProperty().setValue(null);
    }

    @Override
    public void commitEdit(Node newValue) {
        super.commitEdit(newValue);
        graphicProperty().setValue(null);
//        textProperty().setValue(((Leaf) newValue).getValue().toString());
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
        } else {
            graphicProperty().setValue(null);
            textProperty().setValue("");
        }
    }
}
