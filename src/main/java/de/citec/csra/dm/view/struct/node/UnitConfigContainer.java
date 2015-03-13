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
public class UnitConfigContainer extends NodeContainer<UnitConfig> {

    public UnitConfigContainer(UnitConfig unitConfig) {
        super("Unit Configuration", unitConfig);
        super.add(unitConfig.getLabel(), "Label");
        super.add(unitConfig.getName(), "Name");
        super.add(new PlacementConfigContainer(unitConfig.getPlacement()));
        super.add(new ServiceConfigListContainer(unitConfig.getServiceConfigsList()));
        super.add(new ScopeContainer(unitConfig.getScope()));
        super.add(unitConfig.getDescription(), "Description");
    }
}
