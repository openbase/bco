/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.state.InventoryStateType.InventoryState;

/**
 *
 * @author thuxohl
 */
public class InventoryStateContainer extends NodeContainer<InventoryState> {

    public InventoryStateContainer(InventoryState inventoryState) {
        super("Inventory State", inventoryState);
        super.add(inventoryState.getInventoryStatus(), "Inventory Status");
        super.add(new LocationConfigContainer(inventoryState.getLocation()));
        super.add(new TimestampContainer(inventoryState.getTimestamp()));
        super.add(new PersonContainer(inventoryState.getOwner()));
    }
}
