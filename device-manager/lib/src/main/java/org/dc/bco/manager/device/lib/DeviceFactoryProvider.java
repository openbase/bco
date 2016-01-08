package org.dc.bco.manager.device.lib;

import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.jul.exception.NotAvailableException;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface DeviceFactoryProvider {
    public DeviceFactory getDeviceFactory() throws NotAvailableException;
}
