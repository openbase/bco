/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.leave;

/**
 *
 * @author thuxohl
 */
public class NameContainer implements Leave {
    
    private String name;

    public NameContainer(String name) {
        this.name = name;
    }

    @Override
    public Object getValue() {
        return name;
    }

    @Override
    public String getDescriptor() {
        return "Name";
    }
}
