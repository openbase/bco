/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.unit.UnitTypeHolderType.UnitTypeHolder;

/**
 *
 * @author thuxohl
 */
public class UnitTypeContainer extends VariableNode<UnitTypeHolder.Builder>{

    public UnitTypeContainer(UnitTypeHolder.Builder value) {
        super("Unit Type", value);
        super.add(value.getUnitType(), "unit_type");
    }
    
}
