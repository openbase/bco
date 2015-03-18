/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import com.google.protobuf.GeneratedMessage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.unit.UnitTypeHolderType;

/**
 *
 * @author thuxohl
 * @param <MB>
 */
public class VariableNode<MB extends GeneratedMessage.Builder> extends NodeContainer<MB> {

    private final ContextMenu contextMenu;
    private final MenuItem addMenuItem, removeMenuItem;

    public VariableNode(String descriptor, MB value) {
        super(descriptor, value);

        EventHandlerImpl eventHandler = new EventHandlerImpl<>();
        addMenuItem = new MenuItem("Add");
        removeMenuItem = new MenuItem("Remove");
        addMenuItem.setOnAction(eventHandler);
        removeMenuItem.setOnAction(eventHandler);
        contextMenu = new ContextMenu(addMenuItem, removeMenuItem);
    }

    private class EventHandlerImpl<T> implements EventHandler<ActionEvent> {

        @Override
        public void handle(ActionEvent event) {
            if (event.getSource().equals(addMenuItem)) {
                VariableNode addedNode = null;
                if (VariableNode.this instanceof DeviceClassContainer) {
                    addedNode = new DeviceClassContainer(DeviceClassType.DeviceClass.getDefaultInstance().toBuilder());
                } else if (VariableNode.this instanceof UnitTypeContainer) {
                    addedNode = new UnitTypeContainer(UnitTypeHolderType.UnitTypeHolder.getDefaultInstance().toBuilder());
                }

                if (addedNode != null) {
                    VariableNode.this.getParent().getChildren().add(addedNode);
                }

                if (addedNode instanceof SendableNode) {
                    ((SendableNode) addedNode).getApplyButton().setVisible(true);
                } else {
                    TreeItem<Node> parent = addedNode.getParent();
                    while (!(parent instanceof SendableNode)) {
                        parent = parent.getParent();
                    }
                    ((SendableNode) parent).getApplyButton().setVisible(true);
                }
            } else if (event.getSource().equals(removeMenuItem)) {
                VariableNode.this.getParent().getChildren().remove(VariableNode.this);
            }
        }

    }

    public ContextMenu getContextMenu() {
        return contextMenu;
    }
}
