package org.dc.bco.registry.app.lib.provider;

import org.dc.bco.registry.app.lib.AppRegistry;
import org.dc.jul.exception.NotAvailableException;

/**
 * Interface provides a globally managed app registry instance.
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface AppRegistryProvider {

    /**
     * Returns the globally managed app registry instance.
     * @return
     * @throws NotAvailableException 
     */
    public AppRegistry getAppRegistry() throws NotAvailableException;
}
