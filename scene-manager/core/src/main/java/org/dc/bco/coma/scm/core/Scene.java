package org.dc.bco.coma.scm.core;

import org.dc.bco.dal.lib.data.Location;
import org.dc.bco.dal.lib.hal.service.ServiceFactory;
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
