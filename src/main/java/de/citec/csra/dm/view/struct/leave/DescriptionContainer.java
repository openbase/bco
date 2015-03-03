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
public class DescriptionContainer implements Leave {

    final String description;

    public DescriptionContainer(String description) {
        this.description = description;
    }

    @Override
    public Object getValue() {
        return description;
    }

    @Override
    public String getDescriptor() {
        return "Description";
    }
}
