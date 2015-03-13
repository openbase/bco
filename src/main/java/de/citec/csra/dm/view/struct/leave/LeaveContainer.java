/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.leave;

/**
 *
 * @author thuxohl
 * @param <T>
 */
public class LeaveContainer<T> implements Leave<T> {

    private T value;
    private final String descriptor;

    public LeaveContainer(T value, String descriptor) {
        this.value = value;
        this.descriptor = descriptor;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public void setValue(T value) {
        this.value = value;
    }
}
