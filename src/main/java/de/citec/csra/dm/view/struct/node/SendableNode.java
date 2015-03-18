/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import com.google.protobuf.GeneratedMessage;
import javafx.scene.control.Button;

/**
 *
 * @author thuxohl
 * @param <MB>
 */
public class SendableNode<MB extends GeneratedMessage.Builder> extends VariableNode<MB> {

    private final Button applyButton;

    public SendableNode(String descriptor, MB value) {
        super(descriptor, value);
        applyButton = new Button("Apply Changes");
        applyButton.setVisible(false);
    }

    public Button getApplyButton() {
        return applyButton;
    }
}
