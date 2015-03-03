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
public class LabelContainer implements Leave {

    String label;

    public LabelContainer(String label) {
        this.label = label;
    }

    @Override
    public Object getValue() {
        return label;
    }

    @Override
    public String getDescriptor() {
        return "Label";
    }
}
