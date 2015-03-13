/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import java.util.Collection;
import rst.homeautomation.unit.UnitTypeHolderType.UnitTypeHolder;

/**
 *
 * @author thuxohl
 */
public class UnitTypeListContainer extends NodeContainer<Collection<UnitTypeHolder>> {

    public UnitTypeListContainer(final Collection<UnitTypeHolder> unitTypes) {
        super("Unit Types", unitTypes);
        unitTypes.stream().forEach((unitType) -> {
            super.add(unitType.getUnitType(), "Unit Type");
        });
    }
}
