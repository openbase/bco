/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view;

import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;

/**
 *
 * @author thuxohl
 * @param <E> The enumeration type.
 */
public class EnumCell<E extends Enum> extends NodeCell {

    private final ComboBox comboBox;

    public EnumCell() {
        comboBox = new ComboBox(FXCollections.observableArrayList());
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
        comboBox.setValue(leaf.getValue());
        setGraphic(comboBox);
    }
}
