/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.leaf;

import de.citec.csra.dm.view.struct.node.Node;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;

/**
 *
 * @author thuxohl
 * @param <T>
 */
public class LeafContainer<T> implements Leaf<T> {

    private final Property<T> value;
    private final Property<String> descriptor;

    public LeafContainer(T value, String descriptor) {
        this.value = new SimpleObjectProperty<>(value);
        this.descriptor = new ReadOnlyObjectWrapper<>(descriptor);
    }

    @Override
    public T getValue() {
        return value.getValue();
    }

    @Override
    public String getDescriptor() {
        return descriptor.getValue();
    }

    @Override
    public void setValue(T value) {
       this.value.setValue(value);
    }
    
    @Override
    public Node getThis() {
        return this;
    }
}
