/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.data.Location;
import de.citec.dal.hal.service.MultiService;
import de.citec.jul.iface.Identifiable;
import de.citec.jul.extension.rsb.scope.ScopeProvider;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author mpohling
 */
public interface Unit extends MultiService, ScopeProvider, Identifiable<String> {

    public String getLabel();

    public UnitType getType();

    public Location getLocation();
    
    public UnitConfigType.UnitConfig getUnitConfig();

}
