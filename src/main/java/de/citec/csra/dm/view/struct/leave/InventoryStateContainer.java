/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.leave;

import rst.homeautomation.state.InventoryStateType;

/**
 *
 * @author thuxohl
 */
public class InventoryStateContainer implements Leave {
    
    private final InventoryStateType.InventoryState inventoryState;

    public InventoryStateContainer(InventoryStateType.InventoryState inventoryState) {
        this.inventoryState = inventoryState;
    }

    @Override
    public Object getValue() {
        return this.inventoryState;
    }

    @Override
    public String getDescriptor() {
        return "Inventory State";
    }  
}
