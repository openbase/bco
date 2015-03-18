/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import com.google.protobuf.Descriptors;
import rst.homeautomation.binding.BindingConfigType.BindingConfig;

/**
 *
 * @author thuxohl
 */
public class BindingConfigContainer extends NodeContainer<BindingConfig.Builder> {

    public BindingConfigContainer(BindingConfig.Builder bindingConfig) {
        super("BindingConfig", bindingConfig);
//        for(Descriptors.FieldDescriptor field : bindingConfig.getDescriptorForType().getFields()) {
//            System.out.println("Type ["++"], field name ["+field.getName()+"]");
//            super.add(bindingConfig.getField(field), field.getName());
//        }
        super.add(bindingConfig.getBindingType(), "binding_type");
    }
    
    
}
