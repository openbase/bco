/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import de.citec.csra.dm.view.struct.leaf.LeafContainer;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TreeItem;

/**
 *
 * @author thuxohl
 * @param <T>
 */
public class NodeContainer<T> extends TreeItem<Node> implements Node {

    private final Property<String> descriptor;
    private final T value;

    public NodeContainer(String descriptor, T value) {
        this.value = value;
        this.setValue(this);
        this.descriptor = new ReadOnlyObjectWrapper<>(descriptor);
    }

    protected void add(LeafContainer leave) {
        this.getChildren().add(new TreeItem<>(leave));
    }

    protected void add(TreeItem<Node> node) {
        this.getChildren().add(node);
    }

    protected <S> void add(S value, String descriptor) {
        this.add(new LeafContainer(value, descriptor));
    }

    @Override
    public String getDescriptor() {
        return descriptor.getValue();
    }

    @Override
    public Node getThis() {
        return this;
    }
}
