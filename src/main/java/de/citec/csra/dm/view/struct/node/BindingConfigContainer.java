/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.binding.BindingConfigType.BindingConfig;

/**
 *
 * @author thuxohl
 */
public class BindingConfigContainer extends NodeContainer<BindingConfig> {

    public BindingConfigContainer(BindingConfig bindingConfig) {
        super("Binding Configuration", bindingConfig);
        super.add(bindingConfig.getBindingType(), "Binding Type");
    }
}
