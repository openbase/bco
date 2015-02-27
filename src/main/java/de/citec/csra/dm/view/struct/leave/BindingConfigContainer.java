/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.leave;

import rst.homeautomation.binding.BindingConfigType.BindingConfig;

/**
 *
 * @author thuxohl
 */
public class BindingConfigContainer implements Leave {

    final BindingConfig bindingConfig;

    public BindingConfigContainer(BindingConfig bindingConfig) {
        this.bindingConfig = bindingConfig;
    }

    @Override
    public Object getValue() {
        return bindingConfig;
    }

    @Override
    public String getDescriptor() {
        return "Binding Configuration";
    }
}
