/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.leaf;

import com.google.protobuf.Descriptors;
import com.google.protobuf.ProtocolMessageEnum;
import de.citec.csra.dm.view.struct.node.Node;
import de.citec.csra.dm.view.struct.node.NodeContainer;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import rst.homeautomation.binding.BindingConfigType;

/**
 *
 * @author thuxohl
 * @param <T>
 */
public class LeafContainer<T> implements Leaf<T> {

    private final Property<T> value;
    private final Property<String> descriptor;
    private final NodeContainer parent;

    public LeafContainer(T value, String descriptor, NodeContainer parent) {
        this.value = new SimpleObjectProperty<>(value);
        this.descriptor = new ReadOnlyObjectWrapper<>(descriptor);
        this.parent = parent;
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

        Descriptors.FieldDescriptor field = parent.getBuilder().getDescriptorForType().findFieldByName(descriptor.getValue());
        System.out.println("field:fullname" + field.getFullName());
        System.out.println("field:name" + field.getName());
        System.out.println("field:type" + field.getType());
        System.out.println("field:Jvatype" + field.getJavaType());
        System.out.println("getValue():" + getValue());
        System.out.println("getValue().Class:" + getValue().getClass());

        if (value instanceof ProtocolMessageEnum) {
            parent.getBuilder().setField(field, ((ProtocolMessageEnum) getValue()).getValueDescriptor());
        } else if (value instanceof String) {
            parent.getBuilder().setField(field, value);
        } else if (value instanceof Float) {
            parent.getBuilder().setField(field, value);
        }
    }

    @Override
    public Node getThis() {
        return this;
    }
}
