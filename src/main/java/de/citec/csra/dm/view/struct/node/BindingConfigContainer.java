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
public class BindingConfigContainer extends NodeContainer<BindingConfig.Builder> {

    public BindingConfigContainer(BindingConfig.Builder bindingConfig) {
        super("BindingConfig", bindingConfig);
        super.add(bindingConfig.getBindingType(), "binding_type");
    }
}
