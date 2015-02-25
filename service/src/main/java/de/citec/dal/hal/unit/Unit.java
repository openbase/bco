/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.data.Location;
import de.citec.dal.hal.service.MultiService;
import de.citec.jul.iface.Identifiable;
import de.citec.jul.rsb.ScopeProvider;

/**
 *
 * @author mpohling
 */
public interface Unit extends MultiService, ScopeProvider, Identifiable<String> {

    public String getName();

	public String getLabel();

    public Location getLocation();
    
}
