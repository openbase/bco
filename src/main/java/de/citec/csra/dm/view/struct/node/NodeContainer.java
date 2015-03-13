/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import de.citec.csra.dm.view.struct.leave.LeaveContainer;
import javafx.scene.control.TreeItem;

/**
 *
 * @author thuxohl
 * @param <T>
 */
public class NodeContainer<T> extends TreeItem<Node> implements Node {

    private final String descriptor;
    private final T value;

    public NodeContainer(String descriptor, T value) {
        this.descriptor = descriptor;
        this.value = value;
        this.setValue(this);
    }

    protected void add(LeaveContainer leave) {
        this.getChildren().add(new TreeItem<>(leave));
    }

    protected void add(TreeItem<Node> node) {
        this.getChildren().add(node);
    }
    
    protected<S> void add(S value, String descriptor) {
        this.add(new LeaveContainer(value, descriptor));
    }

    @Override
    public String getDescriptor() {
        return descriptor;
    }

}
