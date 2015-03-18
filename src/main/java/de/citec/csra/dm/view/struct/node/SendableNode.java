/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import com.google.protobuf.GeneratedMessage;
import de.citec.csra.dm.remote.DeviceRegistryRemote;
import de.citec.jul.exception.CouldNotPerformException;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import rst.homeautomation.device.DeviceClassType;

/**
 *
 * @author thuxohl
 * @param <MB>
 */
public abstract class SendableNode<MB extends GeneratedMessage.Builder> extends VariableNode<MB> {

    private boolean newNode;

    public SendableNode(String descriptor, MB value) {
        super(descriptor, value);
        newNode = false;
    }

    public void setNewNode() {
        newNode = true;
    }
    
    public boolean isNewNode() {
        return newNode;
    }
}
