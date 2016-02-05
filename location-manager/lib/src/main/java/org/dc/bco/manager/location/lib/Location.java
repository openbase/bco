package org.dc.bco.manager.location.lib;

import org.dc.jul.extension.rst.iface.ScopeProvider;
import org.dc.jul.iface.Configurable;
import org.dc.jul.iface.Identifiable;
import org.dc.jul.iface.provider.LabelProvider;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface Location extends ScopeProvider, LabelProvider, Identifiable<String>, Configurable<String, LocationConfig>{

}
