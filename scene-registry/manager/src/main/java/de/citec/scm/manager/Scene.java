/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.scm.manager;

import de.citec.dal.data.Location;
import de.citec.dal.hal.service.ServiceFactory;
import org.dc.jul.extension.rsb.scope.ScopeProvider;
import org.dc.jul.iface.Activatable;
import org.dc.jul.iface.Identifiable;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public interface Scene extends ScopeProvider, Identifiable<String>, Activatable {

	public String getLabel();

    public Location getLocation();
    
    public ServiceFactory getDefaultServiceFactory();
}
