package org.dc.bco.registry.location.lib.provider;

import org.dc.bco.registry.location.lib.LocationRegistry;
import org.dc.jul.exception.NotAvailableException;

/**
 * Interface provides a globally managed location registry instance.
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface LocationRegistryProvider {

    /**
     * Returns the globally managed location registry instance.
     * @return
     * @throws NotAvailableException 
     */
    public LocationRegistry getLocationRegistry() throws NotAvailableException;
}
