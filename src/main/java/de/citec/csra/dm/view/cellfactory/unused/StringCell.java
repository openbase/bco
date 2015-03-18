/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.cellfactory.unused;

import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 *
 * @author thuxohl
 */
public class StringCell extends NodeCell {
    
    private final TextField textField;

    public StringCell(TextField textField) {
        this.textField = textField;
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
    }
    
    @Override
    public void startEdit() {
        super.startEdit();
        textField.setText(leaf.getValue().toString());
        setGraphic(textField);
    }
}
