/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.leave;

import rst.homeautomation.unit.UnitTypeHolderType.UnitTypeHolder;

/**
 *
 * @author thuxohl
 */
public class UnitTypeContainer implements Leave {

    private final UnitTypeHolder unitType;

    public UnitTypeContainer(UnitTypeHolder unitType) {
        this.unitType = unitType;
    }

    @Override
    public String getDescriptor() {
        return "Unit Type";
    }

    @Override
    public UnitTypeHolder getValue() {
        return unitType;
    }
}
