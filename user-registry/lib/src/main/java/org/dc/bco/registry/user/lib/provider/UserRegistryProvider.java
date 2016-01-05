package org.dc.bco.registry.user.lib.provider;

import org.dc.bco.registry.user.lib.UserRegistry;
import org.dc.jul.exception.NotAvailableException;

/**
 * Interface provides a globally managed user registry instance.
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface UserRegistryProvider {

    /**
     * Returns the globally managed user registry instance.
     * @return
     * @throws NotAvailableException 
     */
    public UserRegistry getUserRegistry() throws NotAvailableException;
}
