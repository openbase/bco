package org.dc.bco.registry.device.lib.provider;

import org.dc.bco.registry.device.lib.DeviceRegistry;
import org.dc.jul.exception.NotAvailableException;

/**
 * Interface provides a globally managed device registry instance.
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface DeviceRegistryProvider {

    /**
     * Returns the globally managed device registry instance.
     * @return
     * @throws NotAvailableException 
     */
    public DeviceRegistry getDeviceRegistry() throws NotAvailableException;
}
