/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author thuxohl
 */
public class UnitConfigContainer extends NodeContainer<UnitConfig.Builder> {

    public UnitConfigContainer(UnitConfig.Builder unitConfig) {
        super("Unit Configuration", unitConfig);
        super.add(unitConfig.getLabel(), "label");
        super.add(unitConfig.getName(), "name");
        super.add(new PlacementConfigContainer(unitConfig.getPlacement().toBuilder()));
        super.add(new ServiceConfigListContainer(unitConfig));
        super.add(new ScopeContainer(unitConfig.getScope().toBuilder()));
        super.add(unitConfig.getDescription(), "description");
    }
}
