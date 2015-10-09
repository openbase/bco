/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.scm.manager;

import de.citec.dal.data.Location;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.jul.extension.rsb.scope.ScopeProvider;
import de.citec.jul.iface.Activatable;
import de.citec.jul.iface.Identifiable;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public interface Scene extends ScopeProvider, Identifiable<String>, Activatable {

	public String getLabel();

    public Location getLocation();
    
    public ServiceFactory getDefaultServiceFactory();
}
