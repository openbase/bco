/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import java.util.Collection;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class UnitConfigListContainer extends NodeContainer<Collection<UnitConfigType.UnitConfig>> {

    public UnitConfigListContainer(final Collection<UnitConfigType.UnitConfig> unitConfigs) {
        super("Unit Configurations", unitConfigs);
        unitConfigs.stream().forEach((unitConfig) -> {
            super.add(new UnitConfigContainer(unitConfig));
        });
    }
}
