package org.dc.bco.manager.device.lib;

import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.pattern.Controller;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface DeviceController extends Device, Controller<DeviceConfig> {

    public ServiceFactory getServiceFactory() throws NotAvailableException;
}
