/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.cellfactory.unused;

import java.lang.reflect.ParameterizedType;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;

/**
 *
 * @author thuxohl
 * @param <E> The enumeration type.
 */
public class EnumCell<E extends Enum<E>> extends NodeCell {

    private final ComboBox comboBox;

    public EnumCell() {
        comboBox = new ComboBox(FXCollections.observableArrayList(detectTypeClass().getEnumConstants()));
        comboBox.setOnAction((Event event) -> {
            leaf.setValue(comboBox.getSelectionModel().getSelectedItem());
            commitEdit(leaf);
        });
    }

    @Override
    public void startEdit() {
        super.startEdit();
        comboBox.setValue(leaf.getValue());
        setGraphic(comboBox);
    }


    public final Class<E> detectTypeClass() {
        return (Class<E>) ((ParameterizedType) getClass()
                .getGenericSuperclass())
                .getActualTypeArguments()[0];
    }
}
